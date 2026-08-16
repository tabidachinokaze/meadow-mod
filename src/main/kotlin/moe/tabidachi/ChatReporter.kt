package moe.tabidachi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import moe.tabidachi.api.ServerApi
import moe.tabidachi.api.model.request.AgentChatReportRequest
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import org.slf4j.LoggerFactory

/**
 * Agent 聊天事件上报（规划 §9.12 实时事件）
 * 监听游戏内聊天消息，转发到后端 POST /servers/{id}/sync/chat（server_key 认证）。
 */
class ChatReporter(
    private val scope: CoroutineScope,
    private val serverApi: ServerApi,
    private val configStorage: ConfigStorage,
) {
    private val LOGGER = LoggerFactory.getLogger("ChatReporter")

    fun register() {
        ServerMessageEvents.CHAT_MESSAGE.register { message, player, _ ->
            val serverId = configStorage.config.serverInfo.id ?: return@register
            val serverKey = configStorage.config.serverInfo.serverKey ?: return@register
            val content = runCatching { message.signedContent() }.getOrNull() ?: return@register
            val senderName = runCatching { player.scoreboardName }.getOrNull() ?: return@register
            val senderUuid = runCatching { message.sender().toString() }.getOrNull()

            scope.launch {
                runCatching {
                    serverApi.reportAgentChat(
                        serverId = serverId,
                        request = AgentChatReportRequest(
                            serverKey = serverKey,
                            machineId = configStorage.machineId,
                            senderUuid = senderUuid,
                            senderName = senderName,
                            content = content,
                            type = "chat",
                        )
                    )
                }.onFailure {
                    LOGGER.debug("聊天上报失败（忽略）", it)
                }
            }
        }
    }
}
