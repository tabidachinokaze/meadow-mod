package moe.tabidachi.api.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Agent 上报聊天消息（对应后端 POST /servers/{id}/sync/chat，规划 §9.12） */
@Serializable
data class AgentChatReportRequest(
    @SerialName("server_key")
    val serverKey: String,
    @SerialName("machine_id")
    val machineId: String,
    @SerialName("sender_uuid")
    val senderUuid: String? = null,
    @SerialName("sender_name")
    val senderName: String,
    @SerialName("content")
    val content: String,
    @SerialName("type")
    val type: String = "chat",
)
