package moe.tabidachi

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import io.ktor.client.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import moe.tabidachi.api.AuthApi
import moe.tabidachi.api.AuthApiImpl
import moe.tabidachi.api.ServerApi
import moe.tabidachi.api.ServerApiImpl
import moe.tabidachi.command.AuthCommand
import moe.tabidachi.command.ServerCommand
import moe.tabidachi.shared.SharedHttpClient
import moe.tabidachi.shared.SharedJson
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory


object Meadow : ModInitializer, CoroutineScope by CoroutineScope(Dispatchers.Default + SupervisorJob()) {
    const val MOD_ID: String = "meadow"

    private val LOGGER = LoggerFactory.getLogger(MOD_ID)
    private val json = SharedJson()
    private val configStorage = ConfigStorage(
        json = json,
        scope = this
    )
    private val httpClient: HttpClient = SharedHttpClient(
        json = json,
        tokenProvider = { configStorage.config.token }
    )
    private val baseUrlProvider = { "https://api.meadow.tabidachi.moe" }
    private val authApi: AuthApi = AuthApiImpl(
        client = httpClient,
        baseUrl = baseUrlProvider
    )
    private val serverApi: ServerApi = ServerApiImpl(
        client = httpClient,
        baseUrl = baseUrlProvider
    )
    private val authCommand: AuthCommand = AuthCommand(
        scope = this,
        authApi = authApi,
        onTokenReceived = { token ->
            configStorage.update { it.copy(token = token) }
        }
    )
    private val serverCommand: ServerCommand = ServerCommand(
        serverApi = serverApi,
        scope = this,
        configStorage = configStorage
    )
    private val agentReporter: AgentReporter = AgentReporter(
        scope = this,
        serverApi = serverApi,
        configStorage = configStorage
    )
    private val chatReporter: ChatReporter = ChatReporter(
        scope = this,
        serverApi = serverApi,
        configStorage = configStorage
    )
    private val agentWsClient: AgentWsClient = AgentWsClient(
        scope = this,
        httpClient = httpClient,
        baseUrl = baseUrlProvider(),
        configStorage = configStorage
    )

    override fun onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        LOGGER.info("Hello Fabric world!")

        // 服务器启动完成后启动 Agent 定时上报 + 广播接收（§9.12）；停止时取消
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            agentReporter.start(server)
            agentWsClient.start(server)
        }
        ServerLifecycleEvents.SERVER_STOPPING.register {
            agentReporter.stop()
            agentWsClient.stop()
        }

        // 监听游戏内聊天并上报（§9.12 实时事件）
        chatReporter.register()

        CommandRegistrationCallback.EVENT.register { dispatcher: CommandDispatcher<CommandSourceStack>, context: CommandBuildContext, selection: Commands.CommandSelection ->
            val meadowLiteral = Commands.literal("meadow")
            authCommand.register(meadowLiteral)
            serverCommand.register(meadowLiteral)
            dispatcher.register(meadowLiteral)
        }
    }

    fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(MOD_ID, path)
}
