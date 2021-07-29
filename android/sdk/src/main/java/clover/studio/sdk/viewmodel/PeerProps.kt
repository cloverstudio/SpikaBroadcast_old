package clover.studio.sdk.viewmodel

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import clover.studio.sdk.call.RoomStore
import clover.studio.sdk.model.Consumers
import org.webrtc.AudioTrack
import org.webrtc.VideoTrack
import java.util.HashSet

class PeerProps(
    roomStore: RoomStore
) : PeerViewProps(roomStore) {

    fun connect(lifecycleOwner: LifecycleOwner, peerId: String?, observer: Observer<in Unit?>) {

        if (peerId.isNullOrEmpty()) {
            return
        }

        CombinedLiveData(roomStore.getConsumers(), roomStore.getPeers()) { consumers, peers ->

            if (consumers == null || peers == null){
                Log.d(TAG, "Combined live data early return")
                return@CombinedLiveData
            }

            var videoConsumerWrapper: Consumers.ConsumerWrapper? = null
            var audioConsumerWrapper: Consumers.ConsumerWrapper? = null
            val peer = peers.getPeer(peerId)

            this.peer = peer

            if (peer?.consumers == null){
                return@CombinedLiveData
            }

            val consumerIds: HashSet<String> = peer.consumers
            for (consumerId in consumerIds) {
                val wp: Consumers.ConsumerWrapper? = consumers.getConsumer(consumerId)
                if (wp?.consumer == null) {
                    continue
                }

                if ("video" == wp.consumer.kind) {
                    videoConsumerWrapper = wp
                } else if ("audio" == wp.consumer.kind) {
                    audioConsumerWrapper = wp
                }
            }

            this.videoConsumerId = videoConsumerWrapper?.consumer?.id
            this.videoRtpParameters = videoConsumerWrapper?.consumer?.rtpParameters
            this.videoTrack = if (videoConsumerWrapper != null) videoConsumerWrapper.consumer.track as VideoTrack else null
            this.videoScore = videoConsumerWrapper?.score
            this.videoVisible = if (videoConsumerWrapper != null){
                !videoConsumerWrapper.isLocallyPaused && !videoConsumerWrapper.isRemotelyPaused
            } else
                false

            this.audioConsumerId = audioConsumerWrapper?.consumer?.id
            this.audioRtpParameters = audioConsumerWrapper?.consumer?.rtpParameters
            this.audioTrack = if (audioConsumerWrapper != null) audioConsumerWrapper.consumer.track as AudioTrack else null
            this.audioScore = audioConsumerWrapper?.score
            this.audioEnabled = if (audioConsumerWrapper != null) {
                !audioConsumerWrapper.isLocallyPaused && !audioConsumerWrapper.isRemotelyPaused
            } else
                false

            Log.d(TAG, "Combined live data updated")

        }.observe(lifecycleOwner, observer)
    }

    companion object {
        private const val TAG = "PeerProps"
    }
}