package clover.studio.sdk.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ServerInfo(
    val hostName: String,
    val port: String
): Parcelable