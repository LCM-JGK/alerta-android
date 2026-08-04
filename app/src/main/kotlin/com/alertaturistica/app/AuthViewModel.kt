package com.alertaturistica.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthViewModel(
    private val repository: AuthRepository,
    private val sessionStore: SecureSessionStore,
) : ViewModel() {
    var uiState by mutableStateOf(
        AuthUiState(
            isLocked = sessionStore.hasToken() && sessionStore.isBiometricEnabled(),
            biometricEnabled = sessionStore.isBiometricEnabled(),
        ),
    )
        private set

    init {
        if (!uiState.isLocked) restoreSession()
        else uiState = uiState.copy(isRestoring = false)
    }

    fun show(screen: AuthScreen) {
        uiState = uiState.copy(screen = screen, message = null)
    }

    fun register(username: String, alias: String, password: String) = launchRequest {
        val response = repository.register(username, alias, password)
        uiState = uiState.copy(
            user = response.user,
            isLocked = false,
            recoveryCodeToSave = response.recoveryCode,
            message = "Cuenta creada. Guarda tu clave de recuperación antes de continuar.",
        )
    }

    fun login(username: String, password: String) = launchRequest {
        val response = repository.login(username, password)
        uiState = uiState.copy(user = response.user, isLocked = false)
    }

    fun resetPassword(username: String, recoveryCode: String, newPassword: String) = launchRequest {
        val response = repository.resetPassword(username, recoveryCode, newPassword)
        sessionStore.clear()
        uiState = AuthUiState(
            screen = AuthScreen.SIGN_IN,
            isRestoring = false,
            recoveryCodeToSave = response.newRecoveryCode,
            message = response.message,
        )
    }

    fun recoveryCodeSaved() {
        uiState = uiState.copy(recoveryCodeToSave = null)
    }

    fun unlockWithBiometrics() {
        uiState = uiState.copy(isLocked = false, isRestoring = true)
        restoreSession()
    }

    fun biometricFailed(message: String) {
        uiState = uiState.copy(message = message)
    }

    fun enableBiometrics() {
        sessionStore.setBiometricEnabled(true)
        uiState = uiState.copy(biometricEnabled = true, message = "Acceso biométrico activado.")
    }

    fun disableBiometrics() {
        sessionStore.setBiometricEnabled(false)
        uiState = uiState.copy(biometricEnabled = false, message = "Acceso biométrico desactivado.")
    }

    fun logout() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.logout() }
            uiState = AuthUiState(isRestoring = false)
        }
    }

    fun consumeMessage() {
        uiState = uiState.copy(message = null)
    }

    private fun restoreSession() {
        if (!sessionStore.hasToken()) {
            uiState = uiState.copy(isRestoring = false, user = null)
            return
        }
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { repository.currentUser() } }
                .onSuccess { user -> uiState = uiState.copy(user = user, isRestoring = false, isLocked = false) }
                .onFailure {
                    sessionStore.clear()
                    uiState = AuthUiState(
                        isRestoring = false,
                        message = "La sesión expiró. Inicia sesión nuevamente.",
                    )
                }
        }
    }

    private fun launchRequest(block: suspend () -> Unit) {
        if (uiState.isLoading) return
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, message = null)
            runCatching { block() }
                .onFailure { error -> uiState = uiState.copy(message = error.message ?: "Ocurrió un error.") }
            uiState = uiState.copy(isLoading = false)
        }
    }
}

class AuthFactory(
    private val repository: AuthRepository,
    private val sessionStore: SecureSessionStore,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AuthViewModel(repository, sessionStore) as T
}
