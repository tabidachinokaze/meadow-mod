package moe.tabidachi.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.tabidachi.api.model.ServerInfo
import moe.tabidachi.api.model.request.AgentChatReportRequest
import moe.tabidachi.api.model.request.GameIdBindRequest
import moe.tabidachi.api.model.request.ServerInitializeRequest
import moe.tabidachi.api.model.request.ServerRegisterRequest
import moe.tabidachi.api.model.request.ServerStatusRequest
import moe.tabidachi.api.model.response.Response
import moe.tabidachi.api.model.response.ServerStatusResult

interface ServerApi {
    suspend fun registerServer(request: ServerRegisterRequest): Response<ServerInfo?>
    suspend fun initializeServer(serverId: Long, request: ServerInitializeRequest): Response<ServerInfo?>
    suspend fun bind(serverId: Long, request: GameIdBindRequest): Response<String?>
    /** Agent 定时状态上报（server_key + machine_id 认证） */
    suspend fun syncStatus(serverId: Long, request: ServerStatusRequest): Response<ServerStatusResult?>
    /** Agent 上报聊天消息（server_key + machine_id 认证） */
    suspend fun reportAgentChat(serverId: Long, request: AgentChatReportRequest): Response<Unit?>
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

    override suspend fun syncStatus(serverId: Long, request: ServerStatusRequest): Response<ServerStatusResult?> =
        withContext(dispatcher) {
            client.post(baseUrl()) {
                url {
                    appendPathSegments("servers", "$serverId", "sync", "status")
                }
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }

    override suspend fun reportAgentChat(serverId: Long, request: AgentChatReportRequest): Response<Unit?> =
        withContext(dispatcher) {
            client.post(baseUrl()) {
                url {
                    appendPathSegments("servers", "$serverId", "sync", "chat")
                }
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
}