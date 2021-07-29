package clover.studio.sdk.model

import org.json.JSONArray
import org.mediasoup.droid.Consumer
import java.util.concurrent.ConcurrentHashMap

class Consumers {
    class ConsumerWrapper internal constructor(
        val type: String,
        var isRemotelyPaused: Boolean,
        consumer: Consumer
    ) {
        var isLocallyPaused = false
        var spatialLayer: Int = -1
        var temporalLayer: Int = -1
        val consumer: Consumer = consumer
        var score: JSONArray? = null
        val preferredSpatialLayer: Int = -1
        val preferredTemporalLayer: Int = -1
    }

    private val consumers: MutableMap<String, ConsumerWrapper>
    fun addConsumer(type: String, consumer: Consumer, remotelyPaused: Boolean) {
        consumers[consumer.id] = ConsumerWrapper(type, remotelyPaused, consumer)
    }

    fun removeConsumer(consumerId: String) {
        consumers.remove(consumerId)
    }

    fun setConsumerPaused(consumerId: String, originator: String) {
        val wrapper = consumers[consumerId] ?: return
        consumers[consumerId]?.let {
            if ("local" == originator) {
                it.isLocallyPaused = true
            } else {
                it.isRemotelyPaused = true
            }
        }

    }

    fun setConsumerResumed(consumerId: String, originator: String) {
        consumers[consumerId]?.let {
            if ("local" == originator) {
                it.isLocallyPaused = false
            } else {
                it.isRemotelyPaused = false
            }
        }
    }

    fun setConsumerCurrentLayers(consumerId: String, spatialLayer: Int, temporalLayer: Int) {
        consumers[consumerId]?.let {
            it.spatialLayer = spatialLayer
            it.temporalLayer = temporalLayer
        }
    }

    fun setConsumerScore(consumerId: String, score: JSONArray?) {
        consumers[consumerId]?.let {
            it.score = score
        }
    }

    fun getConsumer(consumerId: String): ConsumerWrapper? {
        return consumers[consumerId]
    }

    fun clear() {
        consumers.clear()
    }

    init {
        consumers = ConcurrentHashMap()
    }
}