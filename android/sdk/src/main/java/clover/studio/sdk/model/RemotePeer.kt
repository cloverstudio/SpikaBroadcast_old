package clover.studio.sdk.model

import org.json.JSONArray
import org.webrtc.AudioTrack
import org.webrtc.VideoTrack

data class RemotePeer(
    var isMe: Boolean = false,
    var peer: Info? = null,
    var audioProducerId: String? = null,
    var videoProducerId: String? = null,
    var audioTrack: AudioTrack? = null,
    var videoTrack: VideoTrack? = null,
    var audioEnabled: Boolean = false,
    var videoVisible: Boolean = false,
    var audioCodec: String? = null,
    var videoCodec: String? = null,
    var audioScore: JSONArray? = null,
    var videoScore: JSONArray? = null
)
