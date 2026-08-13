package moe.tabidachi.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.LongArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import moe.tabidachi.ConfigStorage
import moe.tabidachi.api.ServerApi
import moe.tabidachi.api.model.request.GameIdBindRequest
import moe.tabidachi.api.model.request.ServerInitializeRequest
import moe.tabidachi.api.model.request.ServerRegisterRequest
import moe.tabidachi.api.model.response.isSuccess
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.players.NameAndId
import org.slf4j.LoggerFactory
import kotlin.uuid.toKotlinUuid

class ServerCommand(
    private val serverApi: ServerApi,
    private val scope: CoroutineScope,
    private val configStorage: ConfigStorage
) : CoroutineScope by scope {
    private val LOGGER = LoggerFactory.getLogger("ServerCommand")
    private val codeMap: MutableMap<NameAndId, String> = mutableMapOf()

    val serverLiteral = Commands.literal("server")
    val registerLiteral = Commands.literal("register")
    val initializeLiteral = Commands.literal("initialize")

    val hostPortArgument = Commands.argument("host:port", StringArgumentType.string())
    val serverIdArgument = Commands.argument("server_id", LongArgumentType.longArg(1))
    val serverKeyArgument = Commands.argument("server_key", StringArgumentType.string())

    private val serverRegisterCommand =
        serverLiteral.then(registerLiteral.then(hostPortArgument.executes(::onServerRegister)))

    private val serverInitializeCommand =
        serverLiteral.then(initializeLiteral.then(serverIdArgument.then(serverKeyArgument.executes(::onServerInitialize))))

    private fun onServerRegister(context: CommandContext<CommandSourceStack>): Int {
        val serverUrl = context.getArgument("host:port", String::class.java)
        val strings = serverUrl.split(':')
        val host = strings.getOrNull(0)?.takeIf { it.isNotBlank() } ?: return run {
            context.source.sendFailure(Component.literal("host参数无效"))
            Command.SINGLE_SUCCESS
        }
        val port = strings.getOrNull(1)?.toIntOrNull() ?: return run {
            context.source.sendFailure(Component.literal("port参数无效"))
            Command.SINGLE_SUCCESS
        }
        launch {
            runCatching {
                serverApi.registerServer(
                    request = ServerRegisterRequest(
                        host = host,
                        port = port
                    )
                )
            }.onSuccess { response ->
                if (response.isSuccess) {
                    context.source.sendSuccess(
                        { Component.literal("注册成功: $response") },
                        false
                    )
                } else {
                    context.source.sendFailure(Component.literal(response.message))
                }
            }.onFailure {
                LOGGER.error("Failed to login", it)
            }
        }
        return Command.SINGLE_SUCCESS
    }

    private fun onServerInitialize(context: CommandContext<CommandSourceStack>): Int {
        val serverId = context.getArgument("server_id", Long::class.java)
        val serverKey = context.getArgument("server_key", String::class.java)
        launch {
            runCatching {
                serverApi.initializeServer(
                    serverId = serverId,
                    request = ServerInitializeRequest(
                        serverKey = serverKey,
                        machineId = configStorage.machineId
                    )
                )
            }.onSuccess { response ->
                if (response.isSuccess) {
                    context.source.sendSuccess(
                        { Component.literal("初始化成功") },
                        false
                    )
                    configStorage.update {
                        it.copy(serverInfo = it.serverInfo.copy(id = serverId, serverKey = serverKey))
                    }
                } else {
                    context.source.sendFailure(Component.literal(response.message))
                }
            }.onFailure {
                LOGGER.error("initialize failure", it)
            }
        }
        return Command.SINGLE_SUCCESS
    }

    private val bindCommand = Commands.literal("bind")
        .then(
            Commands.argument("code", StringArgumentType.word())
                .executes { context ->
                    val player = context.source.player
                    if (player != null) {
                        val code = context.getArgument("code", String::class.java)
                        val nameAndId = player.nameAndId()
                        codeMap[nameAndId] = code
                        LOGGER.info("player ${player.nameAndId()} send code $code")
                        val serverId = configStorage.config.serverInfo.id
                        val serverKey = configStorage.config.serverInfo.serverKey
                        if (serverId != null && serverKey != null) {
                            launch {
                                runCatching {
                                    serverApi.bind(
                                        serverId = serverId,
                                        request = GameIdBindRequest(
                                            uuid = nameAndId.id.toKotlinUuid(),
                                            name = nameAndId.name,
                                            code = code,
                                            machineId = configStorage.machineId,
                                            serverKey = serverKey
                                        )
                                    )
                                }.onSuccess {
                                    if (it.isSuccess) {
                                        context.source.sendSuccess(
                                            { Component.literal("绑定成功") },
                                            false
                                        )
                                    } else {
                                        context.source.sendFailure(Component.literal(it.message))
                                    }
                                }.onFailure {
                                    context.source.sendFailure(Component.literal("请求失败"))
                                }
                            }
                        } else {
                            context.source.sendFailure(Component.literal("服务器未认证"))
                        }
                        Command.SINGLE_SUCCESS
                    } else {
                        0
                    }
                }
        )

    fun register(parent: LiteralArgumentBuilder<CommandSourceStack>) {
        parent.then(serverRegisterCommand)
        parent.then(serverInitializeCommand)
        parent.then(bindCommand)
    }
}