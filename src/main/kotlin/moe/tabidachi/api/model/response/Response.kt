package moe.tabidachi.api.model.response

import kotlinx.serialization.Serializable

@Serializable
data class Response<T>(
    val code: Int,
    val message: String,
    val data: T
)

val Response<*>.isSuccess: Boolean
    get() = when (code) {
        20000,
        40209,
        40211 -> true

        else -> false
    }