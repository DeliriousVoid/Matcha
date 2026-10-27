package com.deliriousvoid.openvkmatcha.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.data.model.UserProfile
import com.deliriousvoid.openvkmatcha.data.repository.MessagesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class ChatMaterialSection(val id: String, val title: String, val mediaType: String?) {
    MEMBERS("members", "Участники", null),
    PHOTOS("photos", "Фотографии", "photo"),
    VIDEOS("videos", "Видео", "video"),
    AUDIO("audio", "Аудио", "audio"),
    DOCUMENTS("documents", "Документы", "doc"),
    LINKS("links", "Ссылки", "link")
}

data class ChatMemberItem(
    val id: Int,
    val user: UserProfile,
    val isOwner: Boolean = false,
    val isModerator: Boolean = false
) {
    val roleTitle: String? get() = when {
        isOwner -> "Владелец"
        isModerator -> "Модератор"
        else -> null
    }
}

data class ChatMaterialItem(
    val id: String,
    val photoUrl: String? = null,
    val videoUrl: String? = null,
    val audioUrl: String? = null,
    val docUrl: String? = null,
    val linkUrl: String? = null,
    val title: String = "",
    val subtitle: String? = null,
    val video: com.deliriousvoid.openvkmatcha.data.model.Video? = null,
    val document: com.deliriousvoid.openvkmatcha.data.model.Document? = null,
    val audioTrack: com.deliriousvoid.openvkmatcha.data.model.AudioTrack? = null
)

data class MaterialSectionState(
    val items: List<ChatMaterialItem> = emptyList(),
    val isLoading: Boolean = false,
    val nextFrom: String? = null,
    val hasLoaded: Boolean = false,
    val error: String? = null
)

data class ChatInfoUiState(
    val peerId: Int = 0,
    val title: String = "",
    val avatarUrl: String = "",
    val memberCount: Int = 0,
    val isMuted: Boolean = false,
    val isUser: Boolean = false,
    val userProfile: UserProfile? = null,
    val userGifts: List<com.deliriousvoid.openvkmatcha.data.model.Gift> = emptyList(),
    val isLoadingGifts: Boolean = false,
    val blacklistedByMe: Boolean = false,
    val members: List<ChatMemberItem> = emptyList(),
    val selectedSection: ChatMaterialSection = ChatMaterialSection.MEMBERS,
    val materials: Map<ChatMaterialSection, MaterialSectionState> = emptyMap(),
    val isLoadingMembers: Boolean = false,
    val isWorking: Boolean = false,
    val canAddMembers: Boolean = true,
    val canModerate: Boolean = false,
    val isOwner: Boolean = false,
    val isModerator: Boolean = false,
    val error: String? = null
)

class ChatInfoViewModel(
    private val messagesRepository: MessagesRepository,
    private val profileRepository: com.deliriousvoid.openvkmatcha.data.repository.ProfileRepository,
    val peerId: Int
) : ViewModel() {

    private val isUserPeer = peerId > 0 && peerId < 2000000000
    private val _uiState = MutableStateFlow(ChatInfoUiState(peerId = peerId, isUser = isUserPeer))
    val uiState: StateFlow<ChatInfoUiState> = _uiState.asStateFlow()

    init {
        if (peerId == 0) {
            _uiState.update { state ->
                state.copy(
                    title = com.deliriousvoid.openvkmatcha.OpenVKMatchaApp.instance.languageManager.getStringOrDefault(com.deliriousvoid.openvkmatcha.R.string.saved_messages, "Сохранённые сообщения"),
                    avatarUrl = com.deliriousvoid.openvkmatcha.Constants.SAVED_MESSAGES_AVATAR
                )
            }
        } else {
            initFromCache()
            if (isUserPeer) {
                loadUserProfileAndGifts()
            } else {
                loadMembers()
            }
            checkMuted()
        }
    }

    private fun loadUserProfileAndGifts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingGifts = true) }
            profileRepository.loadProfile(peerId.toString()).onSuccess { userProfile ->
                _uiState.update { state ->
                    state.copy(
                        userProfile = userProfile,
                        title = userProfile.fullName,
                        avatarUrl = userProfile.photo200.ifBlank { userProfile.photo50 },
                        blacklistedByMe = userProfile.blacklistedByMe
                    )
                }
            }
            profileRepository.getUserGifts(peerId).onSuccess { giftsResponse ->
                _uiState.update { it.copy(userGifts = giftsResponse.items, isLoadingGifts = false) }
            }.onFailure {
                _uiState.update { it.copy(isLoadingGifts = false) }
            }
        }
    }

    fun blockUser(onComplete: (Boolean) -> Unit) {
        if (_uiState.value.isWorking || !isUserPeer) return
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true) }
            profileRepository.banUser(peerId)
                .onSuccess {
                    _uiState.update { it.copy(isWorking = false, blacklistedByMe = true) }
                    onComplete(true)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isWorking = false, error = error.message) }
                    onComplete(false)
                }
        }
    }

    private fun initFromCache() {
        val cachedPeer = messagesRepository.getCachedPeerInfo(peerId)
        if (cachedPeer != null) {
            _uiState.update { state ->
                state.copy(
                    title = cachedPeer.title.ifEmpty { state.title },
                    avatarUrl = cachedPeer.photo.ifEmpty { state.avatarUrl },
                    memberCount = cachedPeer.membersCount ?: state.memberCount
                )
            }
        }
    }

    fun selectSection(section: ChatMaterialSection) {
        _uiState.update { it.copy(selectedSection = section) }
        if (section != ChatMaterialSection.MEMBERS) {
            val state = _uiState.value.materials[section]
            if (state == null || !state.hasLoaded) {
                loadMaterials(section, refresh = true)
            }
        }
    }

    fun checkMuted() {
        viewModelScope.launch {
            messagesRepository.getPushSettings(peerId).onSuccess { json ->
                val response = json.optJSONObject("response") ?: json
                val pushSettings = response.optJSONObject("push_settings") ?: response
                val soundObj = pushSettings.opt("sound")
                val sound = when (soundObj) {
                    is Number -> soundObj.toInt()
                    is Boolean -> if (soundObj) 1 else 0
                    is String -> if (soundObj == "0" || soundObj.lowercase() == "false") 0 else 1
                    else -> 1
                }
                val disabledUntil = pushSettings.optLong("disabled_until", 0)
                val isMuted = sound == 0 || disabledUntil == -1L || disabledUntil > (System.currentTimeMillis() / 1000)
                _uiState.update { it.copy(isMuted = isMuted) }
                com.deliriousvoid.openvkmatcha.util.AppEvents.notifyConversationMuted(peerId, isMuted)
            }
        }
    }

    fun toggleMute() {
        if (_uiState.value.isWorking) return
        val nextMuted = !_uiState.value.isMuted
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true) }
            messagesRepository.setSilenceMode(peerId, time = if (nextMuted) -1 else 0, sound = if (nextMuted) 0 else 1)
                .onSuccess {
                    _uiState.update { it.copy(isMuted = nextMuted, isWorking = false) }
                    com.deliriousvoid.openvkmatcha.util.AppEvents.notifyConversationMuted(peerId, nextMuted)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isWorking = false, error = error.message) }
                }
        }
    }

    fun loadMembers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMembers = true, error = null) }
            messagesRepository.getChatMembers(peerId)
                .onSuccess { json ->
                    val membersList = parseChatMembers(json)
                    val titleFromResponse = parseTitle(json)
                    val photoFromResponse = parsePhoto(json)

                    val sortedMembers = membersList.sortedWith(
                        compareByDescending<ChatMemberItem> { it.isOwner }
                            .thenByDescending { it.isModerator }
                            .thenBy { it.user.fullName }
                    )

                    _uiState.update { state ->
                        val updatedTitle = titleFromResponse.ifEmpty { state.title }
                        val updatedPhoto = photoFromResponse.ifEmpty { state.avatarUrl }
                        val updatedCount = if (sortedMembers.isNotEmpty()) sortedMembers.size else state.memberCount

                        val peerToSave = com.deliriousvoid.openvkmatcha.data.model.Peer(
                            id = peerId,
                            type = if (peerId >= 2000000000) com.deliriousvoid.openvkmatcha.data.model.PeerType.CHAT else com.deliriousvoid.openvkmatcha.data.model.PeerType.USER,
                            title = updatedTitle,
                            photo = updatedPhoto,
                            membersCount = updatedCount
                        )
                        messagesRepository.savePeerInfo(peerToSave)

                        state.copy(
                            members = sortedMembers,
                            memberCount = updatedCount,
                            title = updatedTitle,
                            avatarUrl = updatedPhoto,
                            isLoadingMembers = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoadingMembers = false, error = error.message) }
                }
        }
    }

    fun loadMaterials(section: ChatMaterialSection, refresh: Boolean = false) {
        if (section == ChatMaterialSection.MEMBERS) return
        val currentSectionState = _uiState.value.materials[section] ?: MaterialSectionState()
        if (currentSectionState.isLoading) return
        if (!refresh && !currentSectionState.hasLoaded) return
        if (!refresh && currentSectionState.nextFrom.isNullOrBlank() && currentSectionState.items.isNotEmpty()) return

        val startFrom = if (refresh) null else currentSectionState.nextFrom
        viewModelScope.launch {
            _uiState.update { state ->
                val updatedMaterials = state.materials.toMutableMap()
                updatedMaterials[section] = (updatedMaterials[section] ?: MaterialSectionState()).copy(isLoading = true, error = null)
                state.copy(materials = updatedMaterials)
            }

            messagesRepository.getHistoryAttachments(peerId, section.mediaType ?: "", startFrom = startFrom, count = 40)
                .onSuccess { json ->
                    val (newItems, nextFrom) = parseMaterials(json, section)
                    _uiState.update { state ->
                        val updatedMaterials = state.materials.toMutableMap()
                        val oldSec = updatedMaterials[section] ?: MaterialSectionState()
                        val combined = if (refresh) newItems else (oldSec.items + newItems).distinctBy { it.id }
                        updatedMaterials[section] = MaterialSectionState(
                            items = combined,
                            nextFrom = nextFrom,
                            hasLoaded = true,
                            isLoading = false
                        )
                        state.copy(materials = updatedMaterials)
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        val updatedMaterials = state.materials.toMutableMap()
                        val oldSec = updatedMaterials[section] ?: MaterialSectionState()
                        updatedMaterials[section] = oldSec.copy(isLoading = false, error = error.message)
                        state.copy(materials = updatedMaterials)
                    }
                }
        }
    }

    fun addUsers(userIds: List<Int>, onComplete: (Boolean) -> Unit) {
        if (userIds.isEmpty() || _uiState.value.isWorking) return
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true) }
            messagesRepository.addChatUser(peerId, userIds)
                .onSuccess {
                    _uiState.update { it.copy(isWorking = false) }
                    loadMembers()
                    onComplete(true)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isWorking = false, error = error.message) }
                    onComplete(false)
                }
        }
    }

    fun leaveChat(onComplete: (Boolean) -> Unit) {
        if (_uiState.value.isWorking) return
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true) }
            messagesRepository.leaveChat(peerId)
                .onSuccess {
                    _uiState.update { it.copy(isWorking = false) }
                    onComplete(true)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isWorking = false, error = error.message) }
                    onComplete(false)
                }
        }
    }

    fun setModerator(member: ChatMemberItem, enable: Boolean) {
        if (_uiState.value.isWorking) return
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true) }
            messagesRepository.setMemberRole(peerId, member.id, role = if (enable) "admin" else "member")
                .onSuccess {
                    _uiState.update { it.copy(isWorking = false) }
                    loadMembers()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isWorking = false, error = error.message) }
                }
        }
    }

    fun kickUser(member: ChatMemberItem) {
        if (_uiState.value.isWorking) return
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true) }
            messagesRepository.removeChatUser(peerId, member.id)
                .onSuccess {
                    _uiState.update { it.copy(isWorking = false) }
                    loadMembers()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isWorking = false, error = error.message) }
                }
        }
    }

    private fun parseChatMembers(response: JSONObject): List<ChatMemberItem> {
        val responseObj = response.optJSONObject("response") ?: response
        val items = responseObj.optJSONArray("items") ?: org.json.JSONArray()
        val profilesMap = mutableMapOf<Int, UserProfile>()

        responseObj.optJSONArray("profiles")?.let { profiles ->
            for (i in 0 until profiles.length()) {
                val prof = profiles.optJSONObject(i) ?: continue
                val user = com.deliriousvoid.openvkmatcha.data.parser.JsonParsers.parseUser(prof)
                profilesMap[user.id] = user
            }
        }
        responseObj.optJSONArray("groups")?.let { groups ->
            for (i in 0 until groups.length()) {
                val grp = groups.optJSONObject(i) ?: continue
                val group = com.deliriousvoid.openvkmatcha.data.parser.JsonParsers.parseGroup(grp)
                profilesMap[group.id] = group
            }
        }

        val settings = responseObj.optJSONObject("chat_settings")
        val ownerId = settings?.optInt("owner_id", 0) ?: 0
        val adminId = settings?.optInt("admin_id", 0) ?: 0

        val membersList = mutableListOf<ChatMemberItem>()
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val memberId = item.optInt("member_id", item.optInt("id"))
            if (memberId == 0) continue

            val isOwner = item.optBoolean("is_owner") || memberId == ownerId
            val isModerator = !isOwner && (item.optBoolean("is_admin") || item.optBoolean("is_moderator") || memberId == adminId)

            val user = profilesMap[memberId] ?: UserProfile(
                id = memberId,
                firstName = if (memberId < 0) "Сообщество" else "Пользователь",
                lastName = "$memberId",
                screenName = "id$memberId",
                photo50 = "",
                photo200 = "",
                status = "",
                online = false,
                verified = false
            )

            membersList.add(
                ChatMemberItem(
                    id = memberId,
                    user = user,
                    isOwner = isOwner,
                    isModerator = isModerator
                )
            )
        }
        return membersList
    }

    private fun parseTitle(response: JSONObject): String {
        val responseObj = response.optJSONObject("response") ?: response
        val settings = responseObj.optJSONObject("chat_settings")
        return settings?.optString("title", "") ?: ""
    }

    private fun parsePhoto(response: JSONObject): String {
        val responseObj = response.optJSONObject("response") ?: response
        val settings = responseObj.optJSONObject("chat_settings") ?: return ""
        val photoObj = settings.optJSONObject("photo")
        return com.deliriousvoid.openvkmatcha.data.parser.JsonParsers.largestPhotoUrl(photoObj?.optJSONArray("sizes"))
            ?: photoObj?.optString("photo_200", "")
            ?: settings.optString("photo_200", "")
    }

    private fun parseMaterials(response: JSONObject, section: ChatMaterialSection): Pair<List<ChatMaterialItem>, String?> {
        val responseObj = response.optJSONObject("response") ?: response
        val items = responseObj.optJSONArray("items") ?: org.json.JSONArray()
        val nextFrom = responseObj.optString("next_from").takeIf { it.isNotBlank() }

        val list = mutableListOf<ChatMaterialItem>()
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val attachment = item.optJSONObject("attachment") ?: continue

            val material = when (section) {
                ChatMaterialSection.PHOTOS -> {
                    val photo = attachment.optJSONObject("photo")
                    val url = com.deliriousvoid.openvkmatcha.data.parser.JsonParsers.largestPhotoUrl(photo?.optJSONArray("sizes"))
                        ?: photo?.optString("photo_604") ?: photo?.optString("photo_130")
                    if (!url.isNullOrBlank()) {
                        val ownerId = photo?.optInt("owner_id") ?: 0
                        val photoId = photo?.optInt("id") ?: i
                        ChatMaterialItem(id = "photo_${ownerId}_${photoId}_$i", photoUrl = url, title = "Фотография")
                    } else null
                }
                ChatMaterialSection.VIDEOS -> {
                    val videoJson = attachment.optJSONObject("video")
                    if (videoJson != null) {
                        val parsedVideo = com.deliriousvoid.openvkmatcha.data.parser.JsonParsers.parseVideo(videoJson)
                        val title = parsedVideo.title.ifEmpty { "Видео" }
                        ChatMaterialItem(
                            id = "video_${parsedVideo.ownerId}_${parsedVideo.id}_$i",
                            videoUrl = parsedVideo.videoUrl ?: parsedVideo.playerUrl,
                            photoUrl = parsedVideo.thumbnailUrl,
                            title = title,
                            video = parsedVideo
                        )
                    } else null
                }
                ChatMaterialSection.AUDIO -> {
                    val audio = attachment.optJSONObject("audio")
                    if (audio != null) {
                        val parsedTrack = com.deliriousvoid.openvkmatcha.data.parser.JsonParsers.parseAudioItem(audio)
                        ChatMaterialItem(
                            id = "audio_${parsedTrack.ownerId}_${parsedTrack.id}_$i",
                            audioUrl = parsedTrack.url,
                            title = parsedTrack.title,
                            subtitle = parsedTrack.artist,
                            audioTrack = parsedTrack
                        )
                    } else null
                }
                ChatMaterialSection.DOCUMENTS -> {
                    val doc = attachment.optJSONObject("doc")
                    if (doc != null) {
                        val title = doc.optString("title") ?: "Документ"
                        val url = doc.optString("url") ?: ""
                        val ext = doc.optString("ext")?.lowercase() ?: ""
                        val size = doc.optInt("size", 0)
                        val ownerId = doc.optInt("owner_id", 0)
                        val docId = doc.optInt("id", 0)

                        val previewObj = doc.optJSONObject("preview")
                        val previewPhotoUrl = previewObj?.optJSONObject("photo")?.let {
                            com.deliriousvoid.openvkmatcha.data.parser.JsonParsers.largestPhotoUrl(it.optJSONArray("sizes"))
                        } ?: doc.optString("photo_130").takeIf { it.isNotBlank() }
                        val previewVideoUrl = previewObj?.optJSONObject("video")?.optString("src")?.takeIf { it.isNotBlank() }

                        val docModel = com.deliriousvoid.openvkmatcha.data.model.Document(
                            id = docId,
                            ownerId = ownerId,
                            title = title,
                            size = size,
                            ext = ext,
                            url = url,
                            date = doc.optLong("date", 0),
                            type = doc.optInt("type", 0),
                            previewUrl = previewPhotoUrl,
                            previewGifUrl = previewVideoUrl ?: if (ext == "gif" || url.lowercase().contains("gif")) url else previewPhotoUrl
                        )

                        ChatMaterialItem(
                            id = "doc_${ownerId}_${docId}_$i",
                            docUrl = url,
                            photoUrl = previewPhotoUrl,
                            title = title,
                            subtitle = ext.uppercase(),
                            document = docModel
                        )
                    } else null
                }
                ChatMaterialSection.LINKS -> {
                    val link = attachment.optJSONObject("link")
                    val title = link?.optString("title") ?: link?.optString("url") ?: "Ссылка"
                    val url = link?.optString("url")
                    ChatMaterialItem(id = "link_${i}_${url.hashCode()}", linkUrl = url, title = title, subtitle = url)
                }
                else -> null
            }
            if (material != null) {
                list.add(material)
            }
        }
        return Pair(list, nextFrom)
    }

    companion object {
        fun factory(peerId: Int): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = OpenVKMatchaApp.instance
                return ChatInfoViewModel(app.messagesRepository, app.profileRepository, peerId) as T
            }
        }
    }
}
