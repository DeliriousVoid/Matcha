package com.deliriousvoid.openvkmatcha.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deliriousvoid.openvkmatcha.data.model.Topic
import com.deliriousvoid.openvkmatcha.data.model.TopicComment
import com.deliriousvoid.openvkmatcha.data.model.CommentThreadItem
import com.deliriousvoid.openvkmatcha.data.model.PendingAttachment
import com.deliriousvoid.openvkmatcha.data.model.AttachmentType
import com.deliriousvoid.openvkmatcha.data.repository.FeedRepository
import com.deliriousvoid.openvkmatcha.data.repository.BoardRepository
import com.deliriousvoid.openvkmatcha.data.repository.ProfileRepository
import com.deliriousvoid.openvkmatcha.data.repository.ReportsRepository
import com.deliriousvoid.openvkmatcha.data.repository.AttachmentsRepository
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.util.AppEvents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

data class BoardUiState(
    val topics: List<Topic> = emptyList(),
    val comments: List<TopicComment> = emptyList(),
    val threadedComments: List<CommentThreadItem<TopicComment>> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null,
    val canLoadMore: Boolean = true,
    val isLoadingMore: Boolean = false,
    val inputText: String = "",
    val pendingAttachments: List<PendingAttachment> = emptyList(),
    val replyingTo: TopicComment? = null,
    val isDeveloperMode: Boolean = false,
    val resolvedVirtualId: Int? = null,
    val isResolvingVid: Boolean = false,
    val isAdmin: Boolean = false,
    val fromGroup: Boolean = false
)

class BoardViewModel(
    private val boardRepository: BoardRepository,
    private val feedRepository: FeedRepository,
    private val profileRepository: ProfileRepository,
    private val reportsRepository: ReportsRepository,
    private val attachmentsRepository: AttachmentsRepository,
    private val groupId: Int,
    private val topicId: Int? = null,
    private val vidGuess: Int? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(BoardUiState(
        isDeveloperMode = OpenVKMatchaApp.instance.getSharedPreferences("settings", Context.MODE_PRIVATE).getBoolean("experimental_features", false)
    ))
    val uiState: StateFlow<BoardUiState> = _uiState

    init {
        if (topicId != null) {
            resolveAndLoadComments()
        } else {
            loadTopics()
        }
        checkAdminStatus()
    }

    private fun checkAdminStatus() {
        viewModelScope.launch {
            profileRepository.loadProfile((-groupId.absoluteValue).toString()).onSuccess { profile ->
                _uiState.update { it.copy(isAdmin = profile.isAdmin, fromGroup = profile.isAdmin) }
            }
        }
    }

    fun setFromGroup(enabled: Boolean) = _uiState.update { it.copy(fromGroup = enabled) }

    private fun resolveAndLoadComments() {
        if (topicId == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isResolvingVid = true, isLoading = true) }
            
            val resolvedVid = resolveVirtualId(topicId, vidGuess)
            if (resolvedVid != null) {
                _uiState.update { it.copy(resolvedVirtualId = resolvedVid, isResolvingVid = false) }
                loadCommentsInternal(resolvedVid)
            } else {
                _uiState.update { 
                    it.copy(
                        isResolvingVid = false, 
                        isLoading = false, 
                        error = "Не удалось открыть тему (ограничение API OpenVK)."
                    ) 
                }
            }
        }
    }

    private suspend fun resolveVirtualId(dbId: Int, guess: Int?): Int? {
        // 1. Try guess
        if (guess != null) {
            val topic = boardRepository.getTopicById(groupId, guess).getOrNull()
            if (topic?.id == dbId) return guess
        }

        // 2. Scan around guess or start from 1
        val start = guess ?: 1
        val upper = start + 60
        
        // Sequential check for simplicity, Swift does it in parallel but Coroutines make it easy too
        for (vid in start..upper) {
            val topic = boardRepository.getTopicById(groupId, vid).getOrNull()
            if (topic?.id == dbId) return vid
        }
        
        // 3. Fallback: search in topics list (expensive but reliable)
        // In a real app we might want to fetch a larger chunk of topics
        return null
    }

    fun updateInput(text: String) = _uiState.update { it.copy(inputText = text) }

    fun addAttachment(uri: android.net.Uri, type: AttachmentType, name: String = "", size: Long = 0) {
        _uiState.update { it.copy(pendingAttachments = it.pendingAttachments + PendingAttachment(uri, type, name, size)) }
    }

    fun addExistingAttachment(attachmentString: String, type: AttachmentType, name: String) {
        _uiState.update {
            it.copy(pendingAttachments = it.pendingAttachments + PendingAttachment(
                type = type,
                name = name,
                isExisting = true,
                attachmentString = attachmentString
            ))
        }
    }

    fun removeAttachment(attachment: PendingAttachment) {
        _uiState.update { it.copy(pendingAttachments = it.pendingAttachments - attachment) }
    }

    fun replyTo(comment: TopicComment) {
        val idStr = if (comment.fromId < 0) "club${comment.fromId.absoluteValue}" else "id${comment.fromId}"
        val mention = "[$idStr|${comment.authorName}], "
        _uiState.update { 
            it.copy(
                inputText = it.inputText + mention,
                replyingTo = comment
            ) 
        }
    }

    fun cancelReply() {
        _uiState.update { it.copy(replyingTo = null) }
    }

    fun report(type: String, ownerId: Int, itemId: Int? = null, comment: String? = null) {
        viewModelScope.launch {
            reportsRepository.report(type, ownerId, itemId, comment).onSuccess {
                AppEvents.showSnackbar("Жалоба отправлена")
            }.onFailure {
                AppEvents.showSnackbar("Ошибка: ${it.message}")
            }
        }
    }

    fun loadTopics(refresh: Boolean = false) {
        viewModelScope.launch {
            if (refresh) _uiState.value = _uiState.value.copy(isRefreshing = true)
            else _uiState.value = _uiState.value.copy(isLoading = true)

            boardRepository.getTopics(groupId, offset = 0)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        topics = response.items,
                        isLoading = false,
                        isRefreshing = false,
                        error = null,
                        canLoadMore = response.items.size < response.count
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = error.message
                    )
                }
        }
    }

    fun loadComments(refresh: Boolean = false) {
        val vid = _uiState.value.resolvedVirtualId
        if (vid == null) {
            resolveAndLoadComments()
            return
        }
        viewModelScope.launch {
            if (refresh) _uiState.value = _uiState.value.copy(isRefreshing = true)
            else _uiState.value = _uiState.value.copy(isLoading = true)

            loadCommentsInternal(vid, refresh)
        }
    }

    private fun buildThreadedComments(comments: List<TopicComment>): List<CommentThreadItem<TopicComment>> {
        val roots = comments.filter { it.replyToComment == null }
        val replies = comments.filter { it.replyToComment != null }
        
        val result = mutableListOf<CommentThreadItem<TopicComment>>()
        val processedIds = mutableSetOf<Int>()

        for (root in roots) {
            val treeReplies = findDescendants(root.id, replies).sortedBy { it.date }
            result.add(CommentThreadItem(
                item = root,
                level = 0,
                isLastInThread = false,
                hasNextInThread = treeReplies.isNotEmpty()
            ))
            processedIds.add(root.id)
            
            for (i in treeReplies.indices) {
                val reply = treeReplies[i]
                if (!processedIds.contains(reply.id)) {
                    result.add(CommentThreadItem(
                        item = reply,
                        level = 1,
                        isLastInThread = i == treeReplies.size - 1,
                        hasNextInThread = i < treeReplies.size - 1
                    ))
                    processedIds.add(reply.id)
                }
            }
        }

        val remaining = replies.filter { !processedIds.contains(it.id) }.sortedBy { it.date }
        for (rem in remaining) {
            result.add(CommentThreadItem(
                item = rem,
                level = 0,
                isLastInThread = false,
                hasNextInThread = false
            ))
        }

        return result
    }

    private fun findDescendants(parentId: Int, allReplies: List<TopicComment>): List<TopicComment> {
        val descendants = mutableListOf<TopicComment>()
        val queue = mutableListOf(parentId)
        val visited = mutableSetOf(parentId)

        var i = 0
        while (i < queue.size) {
            val currentId = queue[i++]
            val children = allReplies.filter { it.replyToComment == currentId }
            for (child in children) {
                if (!visited.contains(child.id)) {
                    descendants.add(child)
                    visited.add(child.id)
                    queue.add(child.id)
                }
            }
        }
        return descendants
    }

    private fun updateUiStateWithComments(update: (BoardUiState) -> BoardUiState) {
        _uiState.update { state ->
            val newState = update(state)
            if (newState.comments != state.comments) {
                newState.copy(threadedComments = buildThreadedComments(newState.comments))
            } else {
                newState
            }
        }
    }

    private suspend fun loadCommentsInternal(vid: Int, refresh: Boolean = false) {
        boardRepository.getComments(groupId, vid, offset = 0)
            .onSuccess { response ->
                android.util.Log.d("Board", "Loaded ${response.items.size} comments. First item attachments: img=${response.items.firstOrNull()?.imageUrls?.size}, aud=${response.items.firstOrNull()?.audios?.size}")
                updateUiStateWithComments {
                    it.copy(
                        comments = response.items,
                        isLoading = false,
                        isRefreshing = false,
                        error = null,
                        canLoadMore = response.items.size < response.count
                    )
                }
                loadMissingAuthors(response.items)
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = error.message
                    )
                }
            }
    }

    private fun loadMissingAuthors(comments: List<TopicComment>) {
        viewModelScope.launch {
            // Suspect positive IDs might be groups if name is DELETED or matches groupId
            val suspectedGroupIds = comments.filter { 
                it.fromId > 0 && (it.authorName == "DELETED" || it.authorName == "DELETED " || it.fromId == groupId || it.fromId == -groupId) 
            }.map { it.fromId }.toSet()

            val missingUserIds = comments.filter { 
                it.fromId > 0 && it.authorAvatar.isEmpty() && !suspectedGroupIds.contains(it.fromId) 
            }.map { it.fromId }.distinct()
            
            val missingGroupIds = (comments.filter { 
                it.fromId < 0 && it.authorAvatar.isEmpty() 
            }.map { it.fromId.absoluteValue } + suspectedGroupIds).distinct()

            if (missingUserIds.isEmpty() && missingGroupIds.isEmpty()) return@launch

            val users = if (missingUserIds.isNotEmpty()) {
                profileRepository.loadUsers(missingUserIds).getOrDefault(emptyList())
            } else emptyList()

            val groups = if (missingGroupIds.isNotEmpty()) {
                profileRepository.loadGroupsByIds(missingGroupIds.map { -it.absoluteValue }).getOrDefault(emptyList())
            } else emptyList()

            if (users.isEmpty() && groups.isEmpty()) return@launch

            updateUiStateWithComments { state ->
                val newComments = state.comments.map { comment ->
                    // Try group match first for suspected IDs
                    val group = groups.find { it.id == -comment.fromId.absoluteValue }
                    val user = if (group == null) users.find { it.id == comment.fromId } else null
                    
                    when {
                        group != null -> comment.copy(
                            fromId = group.id, // Fix ID to be negative
                            authorName = group.firstName, 
                            authorAvatar = group.photo200
                        )
                        user != null -> comment.copy(
                            authorName = user.fullName, 
                            authorAvatar = user.photo200
                        )
                        else -> comment
                    }
                }
                state.copy(comments = newComments)
            }
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoadingMore || !_uiState.value.canLoadMore) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            val offset = if (topicId != null) _uiState.value.comments.size else _uiState.value.topics.size
            
            if (topicId != null) {
                val vid = _uiState.value.resolvedVirtualId
                if (vid == null) {
                    _uiState.value = _uiState.value.copy(isLoadingMore = false)
                    return@launch
                }
                boardRepository.getComments(groupId, vid, offset = offset)
                    .onSuccess { response ->
                        updateUiStateWithComments {
                            it.copy(
                                comments = it.comments + response.items,
                                isLoadingMore = false,
                                canLoadMore = (it.comments.size + response.items.size) < response.count
                            )
                        }
                        loadMissingAuthors(response.items)
                    }
                    .onFailure { 
                        _uiState.value = _uiState.value.copy(isLoadingMore = false)
                    }
            } else {
                boardRepository.getTopics(groupId, offset = offset)
                    .onSuccess { response ->
                        _uiState.value = _uiState.value.copy(
                            topics = _uiState.value.topics + response.items,
                            isLoadingMore = false,
                            canLoadMore = (_uiState.value.topics.size + response.items.size) < response.count
                        )
                    }
                    .onFailure {
                        _uiState.value = _uiState.value.copy(isLoadingMore = false)
                    }
            }
        }
    }

    fun postComment(text: String) {
        if (topicId == null) return
        val attachments = _uiState.value.pendingAttachments
        if (text.isBlank() && attachments.isEmpty()) return
        val replying = _uiState.value.replyingTo

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            
            // Upload attachments
            val attachmentStrings = mutableListOf<String>()
            var uploadError: String? = null
            
            for (pending in attachments) {
                if (pending.isExisting) {
                    attachmentStrings.add(pending.attachmentString!!)
                    continue
                }
                val result = attachmentsRepository.uploadAttachment(pending)
                if (result.isFailure) {
                    uploadError = "Ошибка загрузки вложения: ${result.exceptionOrNull()?.message}"
                    break
                }
                attachmentStrings.add(result.getOrThrow())
            }
            
            if (uploadError != null) {
                _uiState.update { it.copy(isSending = false, error = uploadError) }
                return@launch
            }

            val attachmentsParam = if (attachmentStrings.isNotEmpty()) attachmentStrings.joinToString(",") else null
            android.util.Log.d("Board", "Posting comment: text='$text', fromGroup=${_uiState.value.fromGroup}, attachments=$attachmentsParam")

            val vid = _uiState.value.resolvedVirtualId
            if (vid == null) {
                android.util.Log.e("Board", "Cannot post: resolvedVirtualId is null")
                _uiState.update { it.copy(isSending = false, error = "ID темы не разрешен") }
                return@launch
            }

            boardRepository.createComment(groupId, vid, text, attachmentsParam, _uiState.value.fromGroup, replyToComment = replying?.id)
                .onSuccess {
                    android.util.Log.d("Board", "Post success")
                    _uiState.update { it.copy(inputText = "", pendingAttachments = emptyList(), isSending = false, replyingTo = null) }
                    loadComments(refresh = true)
                }
                .onFailure { error ->
                    android.util.Log.e("Board", "Post failure: ${error.message}")
                    _uiState.update { it.copy(isSending = false, error = error.message) }
                }
        }
    }

    fun votePoll(comment: TopicComment, answerIds: List<Int>) {
        val poll = comment.poll ?: return
        viewModelScope.launch {
            feedRepository.addPollVote(poll.ownerId, poll.id, answerIds).onSuccess {
                loadComments(refresh = true)
            }
        }
    }

    fun toggleLike(comment: TopicComment) {
        viewModelScope.launch {
            boardRepository.toggleLike(groupId, comment.id, comment.isLiked).onSuccess {
                _uiState.update { state ->
                    state.copy(
                        comments = state.comments.map { item ->
                            if (item.id == comment.id) {
                                item.copy(
                                    isLiked = !item.isLiked,
                                    likeCount = if (item.isLiked) item.likeCount - 1 else item.likeCount + 1
                                )
                            } else item
                        }
                    )
                }
            }
        }
    }

    companion object {
        fun factory(groupId: Int, topicId: Int? = null, vidGuess: Int? = null): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = OpenVKMatchaApp.instance
                return BoardViewModel(
                    boardRepository = app.boardRepository,
                    feedRepository = app.feedRepository,
                    profileRepository = app.profileRepository,
                    reportsRepository = app.reportsRepository,
                    attachmentsRepository = app.attachmentsRepository,
                    groupId = groupId,
                    topicId = topicId,
                    vidGuess = vidGuess
                ) as T
            }
        }
    }
}
