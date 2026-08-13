package moe.tabidachi.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class ServerInfo(
    @SerialName("id")
    val id: Long? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("host")
    val host: String? = null,
    @SerialName("port")
    val port: Int? = null,
    @SerialName("mod_loader")
    val modLoader: ModLoader? = null,
    @SerialName("version")
    val version: String? = null,
    @SerialName("banner_url")
    val bannerUrl: String? = null,
    @SerialName("tags")
    val tags: List<String>? = null,
    @SerialName("rcon_host")
    val rconHost: String? = null,
    @SerialName("rcon_port")
    val rconPort: Int? = null,
    @SerialName("rcon_password")
    val rconPassword: String? = null,
    @SerialName("is_verified")
    val isVerified: Boolean? = false,
    @SerialName("server_key")
    val serverKey: String? = null,
    @SerialName("machine_id")
    val machineId: String? = null,
    @SerialName("created_at")
    val createdAt: Instant? = null,
    @SerialName("updated_at")
    val updatedAt: Instant? = null,
    @SerialName("owner_id")
    val ownerId: Long? = null,
) {
    fun desensitize(): ServerInfo = copy(
        rconHost = null,
        rconPort = null,
        rconPassword = null,
        serverKey = null,
        machineId = null,
    )
}