package moe.tabidachi

import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import moe.tabidachi.api.ServerApi
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

/**
 * Agent WebSocket 客户端（规划 §9.12 指令下发）
 * 连接后端 /ws/agent（server_key 认证），接收该服务器消息广播并在游戏内显示（公告/聊天）。
 */
class AgentWsClient(
    private val scope: CoroutineScope,
    private val httpClient: io.ktor.client.HttpClient,
    private val baseUrl: String,
    private val configStorage: ConfigStorage,
) {
    private val LOGGER = LoggerFactory.getLogger("AgentWsClient")
    private val json = Json {
        ignoreUnknownKeys = true
        // 与后端一致：全局 snake_case
        namingStrategy = JsonNamingStrategy.SnakeCase
    }
    private var job: Job? = null
    private var server: MinecraftServer? = null

    fun start(server: MinecraftServer) {
        this.server = server
        if (job?.isActive == true) return
        val serverId = configStorage.config.serverInfo.id ?: return
        val serverKey = configStorage.config.serverInfo.serverKey ?: return
        LOGGER.info("AgentWsClient: 连接后端广播通道 serverId=$serverId")
        job = scope.launch {
            while (isActive) {
                runCatching { connectAndListen(serverId, serverKey) }
                    .onFailure { LOGGER.debug("Agent WS 连接失败（稍后重连）", it) }
                delay(5.seconds)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        server = null
    }

    private suspend fun connectAndListen(serverId: Long, serverKey: String) {
        val machineId = configStorage.machineId
        val wsUrl = baseUrl.replace("https://", "wss://").replace("http://", "ws://")
        httpClient.webSocket(
            // server_key / machine_id 走请求头，避免出现在 URL（防代理日志/历史记录泄露凭据）
            request = {
                url("$wsUrl/ws/agent?server_id=$serverId")
                header("X-Server-Key", serverKey)
                header("X-Machine-Id", machineId)
            }
        ) {
            LOGGER.info("AgentWsClient: 已连接")
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                val text = frame.readText()
                runCatching { handleMessage(text) }
                    .onFailure { LOGGER.debug("处理广播消息失败", it) }
            }
        }
    }

    private fun handleMessage(text: String) {
        val obj = json.parseToJsonElement(text).jsonObject
        val content = obj["content"]?.jsonPrimitive?.contentOrNull ?: return
        val sender = obj["sender_name"]?.jsonPrimitive?.contentOrNull ?: "系统"
        val type = obj["type"]?.jsonPrimitive?.contentOrNull ?: "chat"
        val mcServer = server ?: return
        mcServer.execute {
            val message = when (type) {
                "announcement" -> "§6[公告] $content"
                else -> "§7[$sender] $content"
            }
            mcServer.playerList.players.forEach { player: ServerPlayer ->
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(message))
            }
            LOGGER.info("AgentWsClient 广播: $message")
        }
    }
}
