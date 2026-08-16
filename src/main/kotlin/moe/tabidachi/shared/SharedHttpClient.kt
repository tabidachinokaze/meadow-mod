package moe.tabidachi.shared

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.LoggingFormat
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.plugins.plugin
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import io.ktor.client.request.bearerAuth
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

@Suppress("FunctionName")
fun SharedHttpClient(
    json: Json,
    tokenProvider: () -> String?,
): HttpClient {
    return HttpClient(CIO) {
        install(ContentNegotiation) {
            json(
                json = json
            )
        }
        install(WebSockets) {
            pingInterval = 10.seconds
            contentConverter = KotlinxWebsocketSerializationConverter(
                format = json
            )
        }
        install(HttpTimeout) {
            // 防黑洞连接挂死协程（后端半死状态时上报/聊天循环停摆）
            connectTimeoutMillis = 5_000
            requestTimeoutMillis = 10_000
            socketTimeoutMillis = 10_000
        }
        install(Logging) {
            format = LoggingFormat.Default
            // 不记录请求/响应体：避免密码、验证码、server_key 等敏感信息落入日志（S1/S2）
            level = LogLevel.INFO
            logger = Logger.SIMPLE
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }

    }.also {
        it.plugin(HttpSend).intercept { request ->
            tokenProvider()?.let(request::bearerAuth)
            execute(request)
        }
    }
}
