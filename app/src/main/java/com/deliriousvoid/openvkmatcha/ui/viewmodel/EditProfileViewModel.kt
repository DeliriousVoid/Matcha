package com.deliriousvoid.openvkmatcha.ui.viewmodel

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.data.model.EditableProfileInfo
import com.deliriousvoid.openvkmatcha.data.repository.ProfileRepository
import com.deliriousvoid.openvkmatcha.util.AppEvents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class EditProfileTab {
    MAIN, CONTACTS, INTERESTS, ADDITIONAL
}

data class EditProfileUiState(
    val info: EditableProfileInfo = EditableProfileInfo(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false,
    val currentTab: EditProfileTab = EditProfileTab.MAIN,
    val isAvatarUploading: Boolean = false,
    val avatarUploadError: String? = null,
    val newAvatarUri: Uri? = null
)

class EditProfileViewModel(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfileInfo()
    }

    private fun loadProfileInfo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val profileInfoResult = profileRepository.getProfileInfo()
            val userResult = profileRepository.loadCurrentUser()
            
            if (profileInfoResult.isSuccess && userResult.isSuccess) {
                val info = profileInfoResult.getOrThrow()
                val user = userResult.getOrThrow()
                
                _uiState.update { 
                    it.copy(
                        info = info.copy(
                            about = user.about,
                            activities = user.activities ?: "",
                            interests = user.interests ?: "",
                            music = user.music ?: "",
                            movies = user.movies ?: "",
                            tv = user.tv ?: "",
                            books = user.books ?: "",
                            games = user.games ?: "",
                            quotes = user.quotes ?: ""
                        ),
                        isLoading = false
                    ) 
                }
            } else {
                val error = profileInfoResult.exceptionOrNull()?.message 
                    ?: userResult.exceptionOrNull()?.message 
                    ?: "Не удалось загрузить данные профиля"
                _uiState.update { it.copy(isLoading = false, error = error) }
            }
        }
    }

    fun updateInfo(update: (EditableProfileInfo) -> EditableProfileInfo) {
        _uiState.update { it.copy(info = update(it.info)) }
    }

    fun setTab(tab: EditProfileTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun uploadAvatar(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAvatarUploading = true, avatarUploadError = null, newAvatarUri = uri) }
            try {
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw Exception("Не удалось прочитать файл")
                
                profileRepository.getOwnerPhotoUploadServer()
                    .onSuccess { serverJson ->
                        val uploadUrl = serverJson.getJSONObject("response").getString("upload_url")
                        profileRepository.uploadFile(uploadUrl, bytes, "avatar.jpg")
                            .onSuccess { uploadResponse ->
                                val server = uploadResponse.optString("server")
                                val photo = uploadResponse.optString("photo")
                                val hash = uploadResponse.optString("hash")
                                val mid = uploadResponse.optString("mid")
                                
                                profileRepository.saveOwnerPhoto(server, photo, hash, mid)
                                    .onSuccess {
                                        _uiState.update { it.copy(isAvatarUploading = false) }
                                        AppEvents.emitRefreshProfile()
                                    }
                                    .onFailure { error ->
                                        _uiState.update { it.copy(isAvatarUploading = false, avatarUploadError = error.message) }
                                    }
                            }
                            .onFailure { error ->
                                _uiState.update { it.copy(isAvatarUploading = false, avatarUploadError = error.message) }
                            }
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(isAvatarUploading = false, avatarUploadError = error.message) }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(isAvatarUploading = false, avatarUploadError = e.message) }
            }
        }
    }

    fun save() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val info = _uiState.value.info
            profileRepository.saveProfileInfo(info)
                .onSuccess {
                    profileRepository.saveInterestsInfo(info)
                        .onSuccess {
                            _uiState.update { it.copy(isSaving = false, isSaved = true) }
                            AppEvents.emitRefreshProfile()
                        }
                        .onFailure { error ->
                            _uiState.update { it.copy(isSaving = false, error = error.message) }
                        }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSaving = false, error = error.message) }
                }
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return EditProfileViewModel(OpenVKMatchaApp.instance.profileRepository) as T
            }
        }
    }
}
