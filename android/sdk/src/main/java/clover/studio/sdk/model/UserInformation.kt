package clover.studio.sdk.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserInformation(
    val displayName: String,
    val roomId: String,
    val avatarUrl: String? = null,
) : Parcelable