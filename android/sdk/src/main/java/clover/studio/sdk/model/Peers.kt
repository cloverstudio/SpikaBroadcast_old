package clover.studio.sdk.model

import org.json.JSONObject
import org.mediasoup.droid.Consumer
import org.mediasoup.droid.Logger
import java.util.*

class Peers {
    private val mPeersInfo: MutableMap<String, Peer> = Collections.synchronizedMap(
        LinkedHashMap()
    )

    fun addPeer(peerId: String, peerInfo: JSONObject) {
        mPeersInfo[peerId] = Peer(peerInfo)
    }

    fun removePeer(peerId: String) {
        mPeersInfo.remove(peerId)
    }

    fun setPeerDisplayName(peerId: String, displayName: String?) {
        val peer: Peer? = mPeersInfo[peerId]
        if (peer == null) {
            Logger.e(TAG, "no Protoo found")
            return
        }
        peer.displayName = displayName
    }

    fun addConsumer(peerId: String, consumer: Consumer) {
        val peer: Peer? = getPeer(peerId)
        if (peer == null) {
            Logger.e(TAG, "no Peer found for new Consumer")
            return
        }
        peer.consumers.add(consumer.id)
    }

    fun removeConsumer(peerId: String, consumerId: String?) {
        val peer: Peer = getPeer(peerId) ?: return
        peer.consumers.remove(consumerId)
    }

    fun getPeer(peerId: String): Peer? {
        return mPeersInfo[peerId]
    }

    val allPeers: List<Peer>
        get() {
            val peers: MutableList<Peer> =
                ArrayList<Peer>()
            for ((_, value) in mPeersInfo) {
                peers.add(value)
            }
            return peers
        }

    fun clear() {
        mPeersInfo.clear()
    }

    companion object {
        private const val TAG = "Peers"
    }

}