package clover.studio.sdk.model

import clover.studio.sdk.call.ConnectionState

class RoomInfo {
    var url: String? = null
    var roomId: String? = null
    private var mConnectionState: ConnectionState = ConnectionState.NEW
    var activeSpeakerId: String? = null
    var statsPeerId: String? = null
    var isFaceDetection = false
    var connectionState: ConnectionState
        get() = mConnectionState
        set(connectionState) {
            mConnectionState = connectionState
        }
}