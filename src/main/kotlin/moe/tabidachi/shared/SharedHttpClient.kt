package moe.tabidachi.shared

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpSend
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
        install(Logging) {
            format = LoggingFormat.Default
            level = LogLevel.ALL
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
