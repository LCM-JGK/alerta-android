package com.alertaturistica.app

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class AuthRepository(private val sessionStore: SecureSessionStore) {
    private val client = HttpClient(OkHttp) {
        expectSuccess = false
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    suspend fun register(username: String, alias: String, password: String): RegistrationResponse =
        client.post("$BASE_URL/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username, alias, password))
        }.requireSuccess().body<RegistrationResponse>().also { saveSession(it.accessToken) }

    suspend fun login(username: String, password: String): AuthResponse =
        client.post("$BASE_URL/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username, password))
        }.requireSuccess().body<AuthResponse>().also { saveSession(it.accessToken) }

    suspend fun resetPassword(username: String, recoveryCode: String, password: String): RecoveryResetResponse =
        client.post("$BASE_URL/api/auth/reset-password") {
            contentType(ContentType.Application.Json)
            setBody(ResetPasswordRequest(username, recoveryCode, password))
        }.requireSuccess().body()

    suspend fun currentUser(): UserDto {
        val token = sessionStore.readToken() ?: error("No existe una sesión guardada.")
        return client.get("$BASE_URL/api/auth/me") { bearerAuth(token) }.requireSuccess().body()
    }

    suspend fun logout() {
        sessionStore.readToken()?.let { token ->
            runCatching {
                client.post("$BASE_URL/api/auth/logout") { bearerAuth(token) }.requireSuccess()
            }
        }
        sessionStore.clear()
    }

    private fun saveSession(token: String) {
        sessionStore.saveToken(token)
    }

    private suspend fun HttpResponse.requireSuccess(): HttpResponse {
        if (status.isSuccess()) return this
        val message = runCatching { body<ApiError>().error }
            .getOrDefault("No se pudo completar la solicitud.")
        throw IllegalArgumentException(message)
    }

    private companion object {
        const val BASE_URL = "https://alerta-backend-production.up.railway.app"
    }
}
