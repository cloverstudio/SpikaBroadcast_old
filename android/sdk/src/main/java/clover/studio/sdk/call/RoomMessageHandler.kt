package clover.studio.sdk.call

import androidx.annotation.WorkerThread
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.mediasoup.droid.Consumer
import org.mediasoup.droid.Logger
import org.protoojs.droid.Message
import java.util.concurrent.ConcurrentHashMap

internal open class RoomMessageHandler(  // Stored Room States.
    val mStore: RoomStore
) {
    // mediasoup Consumers.
    val mConsumers: MutableMap<String, ConsumerHolder>

    internal class ConsumerHolder(val peerId: String, val mConsumer: Consumer)

    @WorkerThread
    @Throws(JSONException::class)
    fun handleNotification(notification: Message.Notification) {
        val data: JSONObject = notification.data
        when (notification.method) {
            "producerScore" -> {

                // {"producerId":"bdc2e83e-5294-451e-a986-a29c7d591d73","score":[{"score":10,"ssrc":196184265}]}
                val producerId: String = data.getString("producerId")
                val score: JSONArray = data.getJSONArray("score")
                mStore.setProducerScore(producerId, score)
            }
            "newPeer" -> {
                val id: String = data.getString("id")
                val displayName: String = data.optString("displayName")
                mStore.addPeer(id, data)
                mStore.addNotify("$displayName has joined the room")
            }
            "peerClosed" -> {
                val peerId: String = data.getString("peerId")
                mStore.removePeer(peerId)
            }
            "peerDisplayNameChanged" -> {
                val peerId: String = data.getString("peerId")
                val displayName: String = data.optString("displayName")
                val oldDisplayName: String = data.optString("oldDisplayName")
                mStore.setPeerDisplayName(peerId, displayName)
                mStore.addNotify("$oldDisplayName is now $displayName")
            }
            "consumerClosed" -> {
                val consumerId: String = data.getString("consumerId")
                mConsumers.remove(consumerId)?.let {
                    it.mConsumer.close()
                    mConsumers.remove(consumerId)
                    mStore.removeConsumer(it.peerId, it.mConsumer.id)
                }
            }
            "consumerPaused" -> {
                val consumerId: String = data.getString("consumerId")
                mConsumers[consumerId]?.let {
                    mStore.setConsumerPaused(it.mConsumer.id, "remote")
                }
            }
            "consumerResumed" -> {
                val consumerId: String = data.getString("consumerId")
                mConsumers[consumerId]?.let {
                    mStore.setConsumerResumed(it.mConsumer.id, "remote")
                }
            }
            "consumerLayersChanged" -> {
                val consumerId: String = data.getString("consumerId")
                val spatialLayer: Int = data.optInt("spatialLayer")
                val temporalLayer: Int = data.optInt("temporalLayer")
                mConsumers[consumerId]?.let {
                    mStore.setConsumerCurrentLayers(consumerId, spatialLayer, temporalLayer)
                }
            }
            "consumerScore" -> {
                val consumerId: String = data.getString("consumerId")
                val score: JSONArray? = data.optJSONArray("score")
                mConsumers[consumerId]?.let {
                    mStore.setConsumerScore(consumerId, score)
                }

            }
            "dataConsumerClosed" -> {
            }
            "activeSpeaker" -> {
                val peerId: String = data.getString("peerId")
                mStore.setRoomActiveSpeaker(peerId)
            }
            else -> {
                Logger.e(TAG, "unknown protoo notification.method " + notification.method)
            }
        }
    }

    companion object {
        const val TAG = "RoomClient"
    }

    init {
        mConsumers = ConcurrentHashMap()
    }
}