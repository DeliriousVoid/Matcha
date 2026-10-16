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
    val subtitle: String? = null
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
    val peerId: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatInfoUiState(peerId = peerId))
    val uiState: StateFlow<ChatInfoUiState> = _uiState.asStateFlow()

    init {
        loadMembers()
        checkMuted()
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
                val sound = response.optInt("sound", 1)
                val disabledUntil = response.optInt("disabled_until", 0)
                val isMuted = sound == 0 || disabledUntil == -1 || disabledUntil > (System.currentTimeMillis() / 1000)
                _uiState.update { it.copy(isMuted = isMuted) }
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
                        state.copy(
                            members = sortedMembers,
                            memberCount = sortedMembers.size,
                            title = titleFromResponse.ifEmpty { state.title },
                            avatarUrl = photoFromResponse.ifEmpty { state.avatarUrl },
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
                        ChatMaterialItem(id = "photo_${photo?.optInt("id")}", photoUrl = url, title = "Фотография")
                    } else null
                }
                ChatMaterialSection.VIDEOS -> {
                    val video = attachment.optJSONObject("video")
                    val title = video?.optString("title") ?: "Видео"
                    val thumb = com.deliriousvoid.openvkmatcha.data.parser.JsonParsers.largestPhotoUrl(video?.optJSONArray("image"))
                    val player = video?.optString("player")
                    ChatMaterialItem(id = "video_${video?.optInt("id")}", videoUrl = player, photoUrl = thumb, title = title)
                }
                ChatMaterialSection.AUDIO -> {
                    val audio = attachment.optJSONObject("audio")
                    val title = audio?.optString("title") ?: "Аудиозапись"
                    val artist = audio?.optString("artist")
                    val url = audio?.optString("url")
                    ChatMaterialItem(id = "audio_${audio?.optInt("id")}", audioUrl = url, title = title, subtitle = artist)
                }
                ChatMaterialSection.DOCUMENTS -> {
                    val doc = attachment.optJSONObject("doc")
                    val title = doc?.optString("title") ?: "Документ"
                    val url = doc?.optString("url")
                    val ext = doc?.optString("ext")?.uppercase()
                    ChatMaterialItem(id = "doc_${doc?.optInt("id")}", docUrl = url, title = title, subtitle = ext)
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
                return ChatInfoViewModel(app.messagesRepository, peerId) as T
            }
        }
    }
}
