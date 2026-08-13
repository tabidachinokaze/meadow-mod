package moe.tabidachi.shared

import kotlinx.serialization.json.Json

@Suppress("FunctionName")
fun SharedJson(): Json {
    return Json {
        prettyPrint = true
        isLenient = true
    }
}