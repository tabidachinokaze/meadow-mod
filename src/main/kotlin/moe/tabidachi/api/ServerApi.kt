package moe.tabidachi.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.tabidachi.api.model.ServerInfo
import moe.tabidachi.api.model.request.GameIdBindRequest
import moe.tabidachi.api.model.request.ServerInitializeRequest
import moe.tabidachi.api.model.request.ServerRegisterRequest
import moe.tabidachi.api.model.response.Response

interface ServerApi {
    suspend fun registerServer(request: ServerRegisterRequest): Response<ServerInfo?>
    suspend fun initializeServer(serverId: Long, request: ServerInitializeRequest): Response<ServerInfo?>
    suspend fun bind(serverId: Long, request: GameIdBindRequest): Response<String?>
}

class ServerApiImpl(
    private val client: HttpClient,
    private val baseUrl: () -> String,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ServerApi {
    override suspend fun registerServer(request: ServerRegisterRequest): Response<ServerInfo?> =
        withContext(dispatcher) {
            client.post(baseUrl()) {
                url {
                    appendPathSegments("servers")
                }
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }

    override suspend fun initializeServer(serverId: Long, request: ServerInitializeRequest): Response<ServerInfo?> =
        withContext(dispatcher) {
            client.post(baseUrl()) {
                url {
                    appendPathSegments("servers", "$serverId", "init")
                }
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }

    override suspend fun bind(serverId: Long, request: GameIdBindRequest): Response<String?> = withContext(dispatcher) {
        client.post(baseUrl()) {
            url {
                appendPathSegments("servers", "$serverId", "bind")
            }
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}