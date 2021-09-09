package clover.studio.sdk.utils

import clover.studio.sdk.service.CallServiceImpl
import java.lang.Exception
import java.util.*

object UrlFactory {
    private const val HOSTNAME = "mediasouptest.clover.studio"
    private const val PORT = 4443

    fun getInvitationLink(callConfig: CallServiceImpl.CallConfig): String {

        if (callConfig.serverInfo?.hostName.isNullOrBlank()){
            throw ServerDataNotSetException()
        }

        var url = String.format(Locale.US, "https://%s/?roomId=%s", callConfig.serverInfo?.hostName, callConfig.roomId)
        if (callConfig.forceH264) {
            url += "&forceH264=true"
        } else if (callConfig.forceVP9) {
            url += "&forceVP9=true"
        }
        return url
    }

    fun getProtooUrl(callConfig: CallServiceImpl.CallConfig): String {
        if (callConfig.serverInfo?.hostName.isNullOrBlank() ||
            callConfig.serverInfo?.port.isNullOrBlank()){
            throw ServerDataNotSetException()
        }

        var url = String.format(
            Locale.US,
            "wss://%s:%s/?roomId=%s&peerId=%s",
            callConfig.serverInfo?.hostName,
            callConfig.serverInfo?.port,
            callConfig.roomId,
            callConfig.peerId
        )
        if (callConfig.forceH264) {
            url += "&forceH264=true"
        } else if (callConfig.forceVP9) {
            url += "&forceVP9=true"
        }
        return url
    }
}

class ServerDataNotSetException() : Exception()