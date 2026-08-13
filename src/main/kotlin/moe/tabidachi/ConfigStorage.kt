package moe.tabidachi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import moe.tabidachi.Meadow.MOD_ID
import moe.tabidachi.api.model.ServerInfo
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.uuid.Uuid

class ConfigStorage(
    private val json: Json,
    private val scope: CoroutineScope,
) {
    private val configPath: Path = FabricLoader.getInstance().configDir.resolve("${MOD_ID}.json")

    var config: MeadowConfig = MeadowConfig()
        private set

    val machineId: String
        get() = MachineId.get() ?: config.serverInfo?.machineId ?: Uuid.random().toString().also { machineId ->
            update {
                it.copy(serverInfo = (it.serverInfo ?: ServerInfo()).copy(machineId = machineId))
            }
        }

    init {
        if (!configPath.exists()) {
            configPath.parent.createDirectories()
            configPath.createFile()
        }
        loadConfig()
    }

    fun update(block: (MeadowConfig) -> MeadowConfig) {
        config = block(config)
        saveConfig()
    }

    private fun loadConfig() {
        configPath.readText().runCatching<String, MeadowConfig>(json::decodeFromString)
            .onFailure { it.printStackTrace() }.getOrNull()
            ?.let { config = it }
            ?: run {
                saveConfig()
            }
    }

    private fun saveConfig() {
        scope.launch {
            runCatching { configPath.writeText(json.encodeToString(config)) }.onFailure { it.printStackTrace() }
        }
    }
}

@Serializable
data class MeadowConfig(
    val token: String? = null,
    @SerialName("server_info")
    val serverInfo: ServerInfo = ServerInfo(),
)