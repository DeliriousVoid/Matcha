package com.deliriousvoid.openvkmatcha.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.data.model.UserProfile
import com.deliriousvoid.openvkmatcha.data.repository.ProfileRepository
import com.deliriousvoid.openvkmatcha.util.AppEvents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class ExploreUiState(
    val user: UserProfile? = null,
    val balance: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class ExploreViewModel(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState(isLoading = true))
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    init {
        loadData()
        viewModelScope.launch {
            AppEvents.refreshProfile.collect {
                loadData()
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val userResult = profileRepository.loadCurrentUser()
            val balanceResult = OpenVKMatchaApp.instance.api.callMethod("account.getBalance")

            userResult.onSuccess { user ->
                _uiState.update { it.copy(user = user) }
            }.onFailure { error ->
                _uiState.update { it.copy(error = error.message) }
            }

            balanceResult.onSuccess { json ->
                val votes = json.optJSONObject("response")?.optInt("votes", 0) ?: 0
                _uiState.update { it.copy(balance = votes) }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Доброе утро"
            in 12..17 -> "Добрый день"
            in 18..23 -> "Добрый вечер"
            else -> "Доброй ночи"
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = OpenVKMatchaApp.instance
                return ExploreViewModel(app.profileRepository) as T
            }
        }
    }
}
