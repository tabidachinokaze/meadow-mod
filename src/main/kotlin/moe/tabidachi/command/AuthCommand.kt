package moe.tabidachi.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import moe.tabidachi.api.AuthApi
import moe.tabidachi.api.model.request.CodeLoginRequest
import moe.tabidachi.api.model.request.PasswordLoginRequest
import moe.tabidachi.api.model.request.SendCodeType
import moe.tabidachi.api.model.response.isSuccess
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import org.slf4j.LoggerFactory

class AuthCommand(
    private val scope: CoroutineScope,
    private val authApi: AuthApi,
    private val onTokenReceived: (String?) -> Unit
) : CoroutineScope by scope {
    private val LOGGER = LoggerFactory.getLogger("AuthCommand")

    val passwordLoginLiteral = Commands.literal("login_by_password")
    val codeLoginLiteral = Commands.literal("login_by_code")
    val requestLiteral = Commands.literal("request")
    val loginEmailLiteral = Commands.literal("login_email")

    val emailArgument = Commands.argument("email", StringArgumentType.string())
    val accountArgument = Commands.argument("account", StringArgumentType.string())
    val passwordArgument = Commands.argument("password", StringArgumentType.string())
    val codeArgument = Commands.argument("code", StringArgumentType.string())

    private val sendLoginCodeCommand =
        requestLiteral.then(loginEmailLiteral.then(emailArgument.executes(::onLoginEmailRequest)))

    private val passwordLoginCommand =
        passwordLoginLiteral.then(accountArgument.then(passwordArgument.executes(::onPasswordLogin)))

    private val codeLoginCommand =
        codeLoginLiteral.then(emailArgument.then(codeArgument.executes(::onCodeLogin)))

    private fun onLoginEmailRequest(context: CommandContext<CommandSourceStack>): Int {
        launch {
            val email = context.getArgument("email", String::class.java)
            runCatching {
                authApi.sendCode(email, SendCodeType.LOGIN)
            }.onSuccess {
                LOGGER.info("send_login_code to $email")
                if (it.isSuccess) {
                    context.source.sendSuccess(
                        { Component.literal("验证码已发送") },
                        false
                    )
                } else {
                    context.source.sendFailure(
                        Component.literal("验证码已发送失败：${it.message}")
                    )
                }
            }.onFailure {
                LOGGER.error("Failed to send code", it)
                context.source.sendFailure(
                    Component.literal("验证码已发送失败：${it.message}")
                )
            }
        }
        return Command.SINGLE_SUCCESS
    }

    private fun onPasswordLogin(context: CommandContext<CommandSourceStack>): Int {
        val account = context.getArgument("account", String::class.java)
        val password = context.getArgument("password", String::class.java)
        launch {
            runCatching {
                authApi.passwordLogin(
                    request = PasswordLoginRequest(
                        account = account,
                        password = password
                    )
                )
            }.onSuccess {
                if (it.isSuccess) {
                    onTokenReceived(it.data)
                    context.source.sendSuccess(
                        { Component.literal("登录成功") },
                        false
                    )
                } else {
                    context.source.sendFailure(Component.literal(it.message))
                }
            }.onFailure {
                LOGGER.error("Failed to login", it)
                context.source.sendFailure(Component.literal("登录失败${it.message}"))
            }

        }
        return Command.SINGLE_SUCCESS
    }

    private fun onCodeLogin(context: CommandContext<CommandSourceStack>): Int {
        val email = context.getArgument("email", String::class.java)
        val code = context.getArgument("code", String::class.java)
        launch {
            runCatching {
                authApi.codeLogin(
                    request = CodeLoginRequest(
                        email = email,
                        verificationCode = code
                    )
                )
            }.onSuccess {
                if (it.isSuccess) {
                    onTokenReceived(it.data)
                    context.source.sendSuccess(
                        { Component.literal("登录成功") },
                        false
                    )
                } else {
                    context.source.sendFailure(Component.literal(it.message))
                }
            }.onFailure {
                LOGGER.error("Failed to login", it)
                context.source.sendFailure(Component.literal("登录失败${it.message}"))
            }

        }
        return Command.SINGLE_SUCCESS
    }

    fun register(parent: LiteralArgumentBuilder<CommandSourceStack>) {
        parent.then(sendLoginCodeCommand)
        parent.then(passwordLoginCommand)
        parent.then(codeLoginCommand)
    }
}