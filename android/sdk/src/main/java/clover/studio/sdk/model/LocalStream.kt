package clover.studio.sdk.model

import org.webrtc.VideoTrack

class LocalStream(
 val videoTrack: VideoTrack?,
 val cameraEnabled: Boolean,
 val microphoneEnabled: Boolean
)