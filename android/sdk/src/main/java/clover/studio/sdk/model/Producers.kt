package clover.studio.sdk.model

import org.json.JSONArray
import org.mediasoup.droid.Producer
import java.util.concurrent.ConcurrentHashMap

class Producers {
    class ProducersWrapper internal constructor(val producer: Producer?) {
        var score: JSONArray? = null
        var type: String? = null

        companion object {
            const val TYPE_CAM = "cam"
            const val TYPE_SHARE = "share"
        }
    }

    private val mProducers: MutableMap<String, ProducersWrapper>
    fun addProducer(producer: Producer) {
        mProducers[producer.id] = ProducersWrapper(producer)
    }

    fun removeProducer(producerId: String) {
        mProducers.remove(producerId)
    }

    fun setProducerPaused(producerId: String) {
        val wrapper = mProducers[producerId] ?: return
        wrapper.producer!!.pause()
    }

    fun setProducerResumed(producerId: String) {
        val wrapper = mProducers[producerId] ?: return
        wrapper.producer!!.resume()
    }

    fun setProducerScore(producerId: String, score: JSONArray?) {
        mProducers[producerId]?.let {
            it.score = score
        }
    }

    fun filter(kind: String): ProducersWrapper? {
        for (wrapper in mProducers.values) {
            if (wrapper.producer == null) {
                continue
            }
            if (wrapper.producer.track == null) {
                continue
            }
            if (kind == wrapper.producer.track.kind()) {
                return wrapper
            }
        }
        return null
    }

    fun clear() {
        mProducers.clear()
    }

    init {
        mProducers = ConcurrentHashMap()
    }
}