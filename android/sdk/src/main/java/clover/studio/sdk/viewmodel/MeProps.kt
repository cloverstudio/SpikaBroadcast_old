package clover.studio.sdk.viewmodel

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import clover.studio.sdk.call.RoomStore
import org.webrtc.AudioTrack
import org.webrtc.VideoTrack

enum class DeviceState {
    UNSUPPORTED, ON, OFF
}

class MeProps(
    roomStore: RoomStore
) : PeerViewProps(roomStore) {

    var microphoneState = DeviceState.UNSUPPORTED
    var cameraState = DeviceState.UNSUPPORTED
    var shareState = DeviceState.UNSUPPORTED

    fun connect(lifecycleOwner: LifecycleOwner, observer: Observer<in Unit?>){
        CombinedLiveData(roomStore.getProducers(), roomStore.getMe()) { producers, me ->

            if (producers == null || me == null){
                return@CombinedLiveData
            }

            val meAudioPW = producers.filter("audio")
            val meVideoPW = producers.filter("video")

            audioProducerId = meAudioPW?.producer?.id
            videoProducerId = meVideoPW?.producer?.id

            audioRtpParameters = meAudioPW?.producer?.rtpParameters
            videoRtpParameters = meVideoPW?.producer?.rtpParameters

            audioTrack = if (meAudioPW?.producer?.track != null){
                meAudioPW.producer.track as AudioTrack
            } else {
                null
            }
            videoTrack = if (meVideoPW?.producer?.track != null){
                meVideoPW.producer.track as VideoTrack
            } else {
                null
            }

            audioScore = meAudioPW?.score
            videoScore = meVideoPW?.score

            microphoneState = if (!me.isCanSendMic) {
                DeviceState.UNSUPPORTED
            } else if (meAudioPW?.producer == null) {
                DeviceState.UNSUPPORTED
            } else if (!meAudioPW.producer.isPaused) {
                DeviceState.ON
            } else {
                DeviceState.OFF
            }

            cameraState = if (meVideoPW?.producer == null) {
                DeviceState.UNSUPPORTED
            } else if (!meVideoPW.producer.isPaused) {
                DeviceState.ON
            } else {
                DeviceState.OFF
            }
        }.observe(lifecycleOwner, observer)
    }
}