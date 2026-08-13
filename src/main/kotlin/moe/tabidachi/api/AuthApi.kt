package moe.tabidachi.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.tabidachi.api.model.request.CodeLoginRequest
import moe.tabidachi.api.model.request.PasswordLoginRequest
import moe.tabidachi.api.model.request.SendCodeType
import moe.tabidachi.api.model.response.Response

interface AuthApi {
    suspend fun passwordLogin(request: PasswordLoginRequest): Response<String?>
    suspend fun codeLogin(request: CodeLoginRequest): Response<String?>
    suspend fun sendCode(email: String, type: SendCodeType): Response<String?>
}

class AuthApiImpl(
    private val client: HttpClient,
    private val baseUrl: () -> String,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : AuthApi {
    override suspend fun passwordLogin(request: PasswordLoginRequest): Response<String?> = withContext(dispatcher) {
        client.post(baseUrl()) {
            url {
                appendPathSegments("auth", "login", "password")
            }
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun codeLogin(request: CodeLoginRequest): Response<String?> = withContext(dispatcher) {
        client.post(baseUrl()) {
            url {
                appendPathSegments("auth", "login", "code")
            }
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun sendCode(
        email: String,
        type: SendCodeType
    ): Response<String?> = withContext(dispatcher) {
        client.post(baseUrl()) {
            url {
                appendPathSegments("send-code")
                parameters.append("email", email)
                parameters.append("type", type.name)
            }
            contentType(ContentType.Application.Json)
        }.body()
    }
}