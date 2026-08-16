package moe.tabidachi.shared

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

@Suppress("FunctionName")
fun SharedJson(): Json {
    return Json {
        prettyPrint = true
        isLenient = true
        // 后端响应可能包含调用方未声明的字段（如 sync/status 的 ServerStatusResult），忽略未知 key
        ignoreUnknownKeys = true
        // 与后端一致：全局 snake_case（已有 @SerialName 的字段显式优先）
        namingStrategy = JsonNamingStrategy.SnakeCase
    }
}