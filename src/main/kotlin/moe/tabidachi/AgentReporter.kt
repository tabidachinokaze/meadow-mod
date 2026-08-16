package moe.tabidachi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import moe.tabidachi.api.ServerApi
import moe.tabidachi.api.model.request.ServerStatusRequest
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.dimension.LevelStem
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

/**
 * Agent 状态定时上报（规划 §9.12）
 * 每 10s 收集在线玩家（含坐标/世界）、Mod 列表、TPS、运行时长并上报。
 * 仅在服务器已初始化（config 中有 server_id + server_key）时工作。
 */
class AgentReporter(
    private val scope: CoroutineScope,
    private val serverApi: ServerApi,
    private val configStorage: ConfigStorage,
) {
    private val LOGGER = LoggerFactory.getLogger("AgentReporter")
    private var job: Job? = null

    /** 启动定时上报（在服务器启动完成回调中调用） */
    fun start(server: MinecraftServer) {
        if (job?.isActive == true) return
        val serverId = configStorage.config.serverInfo.id
        val serverKey = configStorage.config.serverInfo.serverKey
        if (serverId == null || serverKey == null) {
            LOGGER.info("AgentReporter: 服务器未初始化（缺少 server_id/server_key），等待 /meadow server initialize 后自动启动")
            return
        }
        LOGGER.info("AgentReporter: 开始定时上报 serverId=$serverId")
        job = scope.launch {
            while (isActive) {
                runCatching {
                    report(server)
                }.onFailure {
                    LOGGER.error("AgentReporter: 上报失败", it)
                }
                delay(10.seconds)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private suspend fun report(server: MinecraftServer) {
        val serverId = configStorage.config.serverInfo.id ?: return
        val serverKey = configStorage.config.serverInfo.serverKey ?: return
        val players = server.playerList.players
        val request = ServerStatusRequest(
            serverKey = serverKey,
            machineId = configStorage.machineId,
            onlinePlayers = players.size,
            maxPlayers = server.playerList.maxPlayers,
            uptimeSeconds = (server.tickCount / 20).toLong(),
            tps = computeTps(server),
            players = players.map(::toPlayerStatus),
            mods = collectMods(),
        )
        serverApi.syncStatus(serverId, request)
    }

    private fun computeTps(server: MinecraftServer): Double? {
        // 平均 tick 时间（纳秒）→ TPS = 1e9 / avgTickNanos（受 20 TPS 上限约束）
        val avgNanos = runCatching { server.averageTickTimeNanos }.getOrNull() ?: return null
        if (avgNanos <= 0) return null
        return (1_000_000_000.0 / avgNanos).coerceAtMost(20.0)
    }

    private fun toPlayerStatus(player: ServerPlayer): ServerStatusRequest.PlayerStatus {
        val world = runCatching {
            val id = player.level().dimension().identifier()
            "${id.namespace}:${id.path}"
        }.getOrNull()
        return ServerStatusRequest.PlayerStatus(
            uuid = player.uuid.toString(),
            name = player.scoreboardName,
            x = player.x,
            y = player.y,
            z = player.z,
            world = world,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun collectMods(): List<ServerStatusRequest.ModStatus> {
        // 通过 FabricLoader 枚举已加载的 mod，排除平台/依赖 mod（仅上报玩家可见的内容 mod）
        val fabricLoader = net.fabricmc.loader.api.FabricLoader.getInstance()
        val containers = fabricLoader.allMods
        val result = ArrayList<ServerStatusRequest.ModStatus>()
        for (container in containers) {
            val metadata: net.fabricmc.loader.api.metadata.ModMetadata = container.metadata
            val id = metadata.id
            if (isPlatformMod(id)) continue
            val version = runCatching { metadata.version.friendlyString }.getOrNull() ?: ""
            result.add(
                ServerStatusRequest.ModStatus(
                    name = metadata.name ?: id,
                    version = version,
                )
            )
        }
        return result
    }

    /** 平台/依赖 mod：fabric 模块、kotlin/kotlinx 运行时、mixin 等一律不上报 */
    private fun isPlatformMod(id: String): Boolean {
        if (id in PLATFORM_MODS) return true
        return id.startsWith("fabric-") ||
            id.startsWith("kotlin") ||
            id.startsWith("kotlinx-") ||
            id == "mixin" ||
            id == "MixinExtras" ||
            id == "minecraft"
    }

    companion object {
        private val PLATFORM_MODS = setOf(
            "fabric", "fabricloader", "minecraft", "fabric-language-kotlin",
            "java", "kotlin-stdlib", "fabric-api", "fabric-command-api-v2",
            "atomicfu", "atomicfu-jvm",
        )
    }
}
