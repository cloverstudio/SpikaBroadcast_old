package clover.studio.clovermediasouppoc.utils

import java.util.*

object UrlFactory {
    //  private static final String HOSTNAME = "v3demo.mediasoup.org";
//    private const val HOSTNAME = "192.168.1.119"
    private const val HOSTNAME = "mediasouptest.clover.studio"
    private const val PORT = 4443
    fun getInvitationLink(roomId: String?, forceH264: Boolean, forceVP9: Boolean): String {
        var url = String.format(Locale.US, "https://%s/?roomId=%s", HOSTNAME, roomId)
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
}