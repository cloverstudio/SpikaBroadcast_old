package clover.studio.sdk.utils

import java.lang.Exception
import java.util.*

object UrlFactory {
    //  private static final String HOSTNAME = "v3demo.mediasoup.org";
//    private const val HOSTNAME = "192.168.1.119"
    private const val HOSTNAME = "mediasouptest.clover.studio"
    private const val PORT = 4443

    private var hostname: String? = null
    private var port: String? = null

    fun getInvitationLink(roomId: String?, forceH264: Boolean, forceVP9: Boolean): String {

        if (hostname.isNullOrBlank()){
            throw ServerDataNotSetException()
        }

        var url = String.format(Locale.US, "https://%s/?roomId=%s", hostname, roomId)
        if (forceH264) {
            url += "&forceH264=true"
        } else if (forceVP9) {
            url += "&forceVP9=true"
        }
        return url
    }

    fun getProtooUrl(
        roomId: String?, peerId: String?, forceH264: Boolean, forceVP9: Boolean
    ): String {

        if (hostname.isNullOrBlank() || port.isNullOrBlank()){
            throw ServerDataNotSetException()
        }

        var url = String.format(
            Locale.US, "wss://%s:%d/?roomId=%s&peerId=%s", HOSTNAME, PORT, roomId, peerId
        )
        if (forceH264) {
            url += "&forceH264=true"
        } else if (forceVP9) {
            url += "&forceVP9=true"
        }
        return url
    }

    fun setServerData(hostname: String, port: String) {
        this.hostname = hostname
        this.port = port
    }
}

class ServerDataNotSetException() : Exception()