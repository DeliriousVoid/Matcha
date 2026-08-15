package com.deliriousvoid.openvkmatcha.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.data.repository.AuthRepository
import com.deliriousvoid.openvkmatcha.util.AppEvents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val instanceUrl: String = "",
    val username: String = "",
    val password: String = "",
    val token: String = "",
    val useTokenLogin: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val needsTwoFactor: Boolean = false,
    val twoFactorCode: String = "",
    val navigateTo2FA: Boolean = false,
)

data class TwoFactorUiState(
    val code: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LoginUiState(instanceUrl = authRepository.getSavedInstance())
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateInstance(url: String) = _uiState.update { it.copy(instanceUrl = url, error = null) }
    fun updateUsername(value: String) = _uiState.update { it.copy(username = value, error = null) }
    fun updatePassword(value: String) = _uiState.update { it.copy(password = value, error = null) }
    fun updateToken(value: String) = _uiState.update { it.copy(token = value, error = null) }
    fun updateTwoFactorCode(value: String) = _uiState.update { it.copy(twoFactorCode = value, error = null) }
    fun toggleLoginMode() = _uiState.update { it.copy(useTokenLogin = !it.useTokenLogin, error = null) }
    fun onNavigatedTo2FA() = _uiState.update { it.copy(navigateTo2FA = false) }

    fun login(onSuccess: () -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.saveInstance(state.instanceUrl)

            val result = if (state.useTokenLogin) {
                authRepository.loginWithToken(state.token.trim())
            } else {
                authRepository.login(
                    username = state.username.trim(),
                    password = state.password,
                    code = state.twoFactorCode.takeIf { state.needsTwoFactor },
                )
            }

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    viewModelScope.launch {
                        AppEvents.emitAccountChanged()
                        AppEvents.emitRefreshFeed()
                        AppEvents.emitRefreshMusic()
                    }
                    onSuccess()
                },
                onFailure = { error ->
                    val message = error.message.orEmpty()
                    val needs2fa = message.contains("need_validation", ignoreCase = true) ||
                        message.contains("validation", ignoreCase = true) ||
                        message.contains("2fa", ignoreCase = true) ||
                        message.contains("use app code", ignoreCase = true)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = if (needs2fa) null else message,
                            needsTwoFactor = needs2fa || it.needsTwoFactor,
                            navigateTo2FA = needs2fa,
                        )
                    }
                },
            )
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = OpenVKMatchaApp.instance
                return LoginViewModel(app.authRepository) as T
            }
        }
    }
}

class TwoFactorViewModel(
    private val authRepository: AuthRepository,
    private val username: String,
    private val password: String,
    private val instance: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TwoFactorUiState())
    val uiState: StateFlow<TwoFactorUiState> = _uiState.asStateFlow()

    fun updateCode(value: String) = _uiState.update { it.copy(code = value, error = null) }

    fun verify(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.code.length != 6) {
            _uiState.update { it.copy(error = "Код должен состоять из 6 цифр") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.saveInstance(instance)
            val result = authRepository.login(username, password, state.code)

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    viewModelScope.launch {
                        AppEvents.emitAccountChanged()
                        AppEvents.emitRefreshFeed()
                        AppEvents.emitRefreshMusic()
                    }
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Ошибка проверки кода",
                        )
                    }
                }
            )
        }
    }

    companion object {
        fun factory(username: String, password: String, instance: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val app = OpenVKMatchaApp.instance
                    return TwoFactorViewModel(app.authRepository, username, password, instance) as T
                }
            }
    }
}

class AccountsViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _accounts = MutableStateFlow(authRepository.getAccounts())
    val accounts = _accounts.asStateFlow()

    private val _currentAccountId = MutableStateFlow(
        OpenVKMatchaApp.instance.accountManager.getCurrentAccountId()
    )
    val currentAccountId = _currentAccountId.asStateFlow()

    fun refreshAccounts() {
        _accounts.value = authRepository.getAccounts()
        _currentAccountId.value = OpenVKMatchaApp.instance.accountManager.getCurrentAccountId()
    }

    fun switchAccount(accountId: String, onSwitched: () -> Unit) {
        if (authRepository.switchAccount(accountId)) {
            _currentAccountId.value = accountId
            viewModelScope.launch {
                AppEvents.emitAccountChanged()
            }
            onSwitched()
        }
    }

    fun removeAccount(accountId: String) {
        authRepository.removeAccount(accountId)
        refreshAccounts()
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AccountsViewModel(OpenVKMatchaApp.instance.authRepository) as T
            }
        }
    }
}

class SplashViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _isAuthenticated = MutableStateFlow<Boolean?>(null)
    val isAuthenticated: StateFlow<Boolean?> = _isAuthenticated.asStateFlow()

    init {
        viewModelScope.launch {
            if (!authRepository.hasToken()) {
                _isAuthenticated.value = false
                return@launch
            }
            _isAuthenticated.value = authRepository.validateSession().isSuccess
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SplashViewModel(OpenVKMatchaApp.instance.authRepository) as T
            }
        }
    }
}
