package moe.tabidachi.api.model.response

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** 状态上报响应（对应后端 ServerStatusResult，snake_case JSON） */
@Serializable
data class ServerStatusResult(
    val onlinePlayers: Int = 0,
    val maxPlayers: Int = 0,
    val onlinePlayerCount: Int = 0,
    val syncedAt: Instant? = null,
)
