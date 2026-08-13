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

    override fun onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        LOGGER.info("Hello Fabric world!")

        CommandRegistrationCallback.EVENT.register { dispatcher: CommandDispatcher<CommandSourceStack>, context: CommandBuildContext, selection: Commands.CommandSelection ->
            val meadowLiteral = Commands.literal("meadow")
            authCommand.register(meadowLiteral)
            serverCommand.register(meadowLiteral)
            dispatcher.register(meadowLiteral)
            dispatcher.register(
                Commands
                    .literal("fetch_code")
                    .then(
                        Commands
                            .argument("key", StringArgumentType.string())
                            .executes {
                                val player = it.source.player
                                if (player != null) {
                                    val code = it.getArgument("code", String::class.java)
                                    LOGGER.info("player ${player.nameAndId()} send code $code")
                                    it.source.sendSuccess(
                                        { Component.literal("验证码已发送") },
                                        false
                                    )
                                    Command.SINGLE_SUCCESS
                                } else {
                                    0
                                }
                            }
                            .build()
                    )
            )
        }
    }

    fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(MOD_ID, path)
}
