package moe.tabidachi.shared

import kotlinx.serialization.json.Json

@Suppress("FunctionName")
fun SharedJson(): Json {
    return Json {
        prettyPrint = true
        isLenient = true
        // 后端响应可能包含调用方未声明的字段（如 sync/status 的 ServerStatusResult），忽略未知 key
        ignoreUnknownKeys = true
    }
}