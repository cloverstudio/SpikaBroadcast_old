package clover.studio.sdk.viewmodel

import clover.studio.sdk.call.RoomStore
import clover.studio.sdk.model.Info
import org.json.JSONArray
import org.webrtc.AudioTrack
import org.webrtc.VideoTrack

abstract class PeerViewProps(
    val roomStore: RoomStore
) {
    var isMe = false
    var showInfo: Boolean = false
    var peer: Info? = null
    var audioProducerId: String? = null
    var videoProducerId: String? = null
    var audioConsumerId: String? = null
    var videoConsumerId: String? = null
    var audioRtpParameters: String? = null
    var videoRtpParameters: String? = null
    var consumerSpatialLayers: Int = -1
    var consumerTemporalLayers: Int = -1
    var consumerCurrentSpatialLayer: Int = -1
    var consumerCurrentTemporalLayer: Int = -1
    var consumerPreferredSpatialLayer: Int = -1
    var consumerPreferredTemporalLayer: Int = -1
    var audioTrack: AudioTrack? = null
    var videoTrack: VideoTrack? = null
    var audioEnabled: Boolean = false
    var videoVisible: Boolean = false
    var videoMultiLayer: Boolean = false
    var audioCodec: String? = null
    var videoCodec: String? = null
    var audioScore: JSONArray? = null
    var videoScore: JSONArray? = null
    var faceDetection: Boolean = false
}