package live.ditto.quickstart.tasks.models

import androidx.annotation.Keep
@Keep
data class DittoConfig(
    val authUrl: String,
    val websocketUrl: String,
    val appId: String,
    val authToken: String)

