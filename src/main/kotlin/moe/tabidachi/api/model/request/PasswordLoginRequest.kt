package moe.tabidachi.api.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PasswordLoginRequest(
    @SerialName("account")
    val account: String,
    @SerialName("password")
    val password: String
)