package com.alertaturistica.app

import kotlinx.serialization.Serializable

@Serializable data class RegisterRequest(val username: String, val alias: String, val password: String)
@Serializable data class LoginRequest(val username: String, val password: String)
@Serializable data class ResetPasswordRequest(
    val username: String,
    val recoveryCode: String,
    val newPassword: String,
)
@Serializable data class UserDto(
    val id: Long,
    val username: String,
    val alias: String,
    val isModerator: Boolean = false,
)
@Serializable data class AuthResponse(val accessToken: String, val expiresAt: String, val user: UserDto)
@Serializable data class RegistrationResponse(
    val accessToken: String,
    val expiresAt: String,
    val user: UserDto,
    val recoveryCode: String,
)
@Serializable data class RecoveryResetResponse(val message: String, val newRecoveryCode: String)
@Serializable data class MessageResponse(val message: String)
@Serializable data class ApiError(val error: String)

enum class AuthScreen {
    SIGN_IN,
    REGISTER,
    RESET_PASSWORD,
}

data class AuthUiState(
    val user: UserDto? = null,
    val screen: AuthScreen = AuthScreen.SIGN_IN,
    val isLoading: Boolean = false,
    val isRestoring: Boolean = true,
    val isLocked: Boolean = false,
    val biometricEnabled: Boolean = false,
    val recoveryCodeToSave: String? = null,
    val message: String? = null,
)
