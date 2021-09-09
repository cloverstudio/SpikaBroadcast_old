package clover.studio.sdk.call

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.preference.PreferenceManager
import android.text.TextUtils
import androidx.annotation.WorkerThread
import clover.studio.sdk.service.CallServiceImpl
import clover.studio.sdk.socket.SocketManager
import clover.studio.sdk.socket.protoo.ProtooSocketManager
import clover.studio.sdk.socket.protoo.WebSocketTransport
import clover.studio.clovermediasouppoc.utils.Async
import clover.studio.clovermediasouppoc.utils.JsonUtils.jsonPut
import clover.studio.clovermediasouppoc.utils.JsonUtils.toJsonObject
import clover.studio.clovermediasouppoc.utils.PeerConnectionUtils
import clover.studio.sdk.utils.UrlFactory
import io.reactivex.disposables.CompositeDisposable
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.mediasoup.droid.*
import org.protoojs.droid.Message
import org.protoojs.droid.Peer
import org.protoojs.droid.ProtooException
import org.webrtc.AudioTrack
import org.webrtc.CameraVideoCapturer
import org.webrtc.VideoTrack

enum class ConnectionState {
    // initial state.
    NEW,  // connecting or reconnecting.
    CONNECTING,  // connected.
    CONNECTED,  // mClosed.
    CLOSED
}

internal class RoomClient(
    context: Context,
    roomStore: RoomStore,
    private val callConfig: CallServiceImpl.CallConfig,
    options: RoomOptions?
) : RoomMessageHandler(roomStore) {


    // Closed flag.
    private var mClosed: Boolean = false

    // Android context.
    private val mContext: Context = context.applicationContext

    // PeerConnection util.
    private var mPeerConnectionUtils: PeerConnectionUtils? = null

    // Room mOptions.
    private val mOptions: RoomOptions = options ?: RoomOptions()

    // Display name.
    private var mDisplayName: String?

    // Display name.
    private var avatarUrl: String?

    // TODO(Haiyangwu):Next expected dataChannel test number.
    private val mNextDataChannelTestNumber: Long = 0

    // Protoo URL.
    private val mProtooUrl: String

    // mProtoo-client Protoo instance.
    private var socketManager: SocketManager? = null

    // mediasoup-client Device instance.
    private var mMediasoupDevice: Device? = null

    // mediasoup Transport for sending.
    private var mSendTransport: SendTransport? = null

    // mediasoup Transport for receiving.
    private var mRecvTransport: RecvTransport? = null

    // Local Audio Track for mic.
    private var mLocalAudioTrack: AudioTrack? = null

    // Local mic mediasoup Producer.
    private var mMicProducer: Producer? = null

    // local Video Track for cam.
    private var mLocalVideoTrack: VideoTrack? = null

    // Local cam mediasoup Producer.
    private var mCamProducer: Producer? = null

    // TODO(Haiyangwu): Local share mediasoup Producer.
    private val mShareProducer: Producer? = null

    // TODO(Haiyangwu): Local chat DataProducer.
    private val mChatDataProducer: Producer? = null

    // TODO(Haiyangwu): Local bot DataProducer.
    private val mBotDataProducer: Producer? = null

    // jobs worker handler.
    private val mWorkHandler: Handler

    // main looper handler.
    private val mMainHandler: Handler

    // Disposable Composite. used to cancel running
    private val mCompositeDisposable: CompositeDisposable = CompositeDisposable()

    // Share preferences
    private val mPreferences: SharedPreferences

    init {
        Logger.d(TAG, "init")
        mDisplayName = callConfig.displayName
        avatarUrl = callConfig.avatarUrl
        mClosed = false
        mProtooUrl = UrlFactory.getProtooUrl(callConfig)
        this.mStore.setMe(callConfig.peerId, callConfig.displayName, mOptions.device)
        this.mStore.setRoomUrl(callConfig.roomId, UrlFactory.getInvitationLink(callConfig))
        mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext)

        // init worker handler.
        Logger.d(TAG, "init handlers")
        val handlerThread = HandlerThread("worker")
        handlerThread.start()
        mWorkHandler = Handler(handlerThread.looper)
        Logger.d(TAG, "work handler initialized")
        mMainHandler = Handler(Looper.getMainLooper())
        Logger.d(TAG, "main handler initialized")
        mWorkHandler.post { mPeerConnectionUtils = PeerConnectionUtils() }
    }

    @Async
    fun join() {
        Logger.d(TAG, "join() " + mProtooUrl)
        mStore.setRoomState(ConnectionState.CONNECTING)
        mWorkHandler.post {
            val transport = WebSocketTransport(mProtooUrl)
            socketManager = ProtooSocketManager(transport, peerListener)
        }
    }

    @Async
    fun enableMic() {
        Logger.d(TAG, "enableMic()")
        mWorkHandler.post { enableMicImpl() }
    }

    @Async
    fun disableMic() {
        Logger.d(TAG, "disableMic()")
        mWorkHandler.post { disableMicImpl() }
    }

    @Async
    fun muteMic() {
        Logger.d(TAG, "muteMic()")
        mWorkHandler.post { muteMicImpl() }
    }

    @Async
    fun unmuteMic() {
        Logger.d(TAG, "unmuteMic()")
        mWorkHandler.post { unmuteMicImpl() }
    }

    @Async
    fun enableCam() {
        Logger.d(TAG, "enableCam()")
        mStore.setCamInProgress(true)
        mWorkHandler.post {
            enableCamImpl()
            mStore.setCamInProgress(false)
        }
    }

    @Async
    fun disableCam() {
        Logger.d(TAG, "disableCam()")
        mWorkHandler.post { disableCamImpl() }
    }

    @Async
    fun changeCam() {
        Logger.d(TAG, "changeCam()")
        mStore.setCamInProgress(true)
        mWorkHandler.post {
            mPeerConnectionUtils?.switchCam(
                object : CameraVideoCapturer.CameraSwitchHandler {
                    override fun onCameraSwitchDone(b: Boolean) {
                        mStore.setCamInProgress(false)
                    }

                    override fun onCameraSwitchError(s: String) {
                        Logger.w(TAG, "changeCam() | failed: $s")
                        mStore.addNotify("error", "Could not change cam: $s")
                        mStore.setCamInProgress(false)
                    }
                })
        }
    }

    @Async
    fun disableShare() {
        Logger.d(TAG, "disableShare()")
        // TODO(feature): share
    }

    @Async
    fun enableShare() {
        Logger.d(TAG, "enableShare()")
        // TODO(feature): share
    }

    @Async
    fun enableAudioOnly() {
        Logger.d(TAG, "enableAudioOnly()")
        mStore.setAudioOnlyInProgress(true)
        disableCam()
        mWorkHandler.post {
            for (holder in mConsumers.values) {
                if ("video" != holder.mConsumer.kind) {
                    continue
                }
                pauseConsumer(holder.mConsumer)
            }
            mStore.setAudioOnlyState(true)
            mStore.setAudioOnlyInProgress(false)
        }
    }

    @Async
    fun disableAudioOnly() {
        Logger.d(TAG, "disableAudioOnly()")
        mStore.setAudioOnlyInProgress(true)
        if (mCamProducer == null && mOptions.isProduce) {
            enableCam()
        }
        mWorkHandler.post {
            for (holder in mConsumers.values) {
                if ("video" != holder.mConsumer.kind) {
                    continue
                }
                resumeConsumer(holder.mConsumer)
            }
            mStore.setAudioOnlyState(false)
            mStore.setAudioOnlyInProgress(false)
        }
    }

    @Async
    fun muteAudio() {
        Logger.d(TAG, "muteAudio()")
        mStore.setAudioMutedState(true)
        mWorkHandler.post {
            for (holder in mConsumers.values) {
                if ("audio" != holder.mConsumer.kind) {
                    continue
                }
                pauseConsumer(holder.mConsumer)
            }
        }
    }

    @Async
    fun unmuteAudio() {
        Logger.d(TAG, "unmuteAudio()")
        mStore.setAudioMutedState(false)
        mWorkHandler.post {
            for (holder in mConsumers.values) {
                if ("audio" != holder.mConsumer.kind) {
                    continue
                }
                resumeConsumer(holder.mConsumer)
            }
        }
    }

    @Async
    fun restartIce() {
        Logger.d(TAG, "restartIce()")
        mStore.setRestartIceInProgress(true)
        mWorkHandler.post {
            try {
                mSendTransport?.let {
                    val iceParameters: String = socketManager!!.syncRequest(
                        "restartIce"
                    ) { req -> jsonPut(req, "transportId", it.id) }
                    it.restartIce(iceParameters)
                }
                mRecvTransport?.let {
                    val iceParameters: String = socketManager!!.syncRequest(
                        "restartIce"
                    ) { req -> jsonPut(req, "transportId", it.getId()) }
                    it.restartIce(iceParameters)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                logError("restartIce() | failed:", e)
                mStore.addNotify("error", "ICE restart failed: " + e.message)
            }
            mStore.setRestartIceInProgress(false)
        }
    }

    @Async
    fun setMaxSendingSpatialLayer() {
        Logger.d(TAG, "setMaxSendingSpatialLayer()")
        // TODO(feature): layer
    }

    @Async
    fun setConsumerPreferredLayers(spatialLayer: String?) {
        Logger.d(TAG, "setConsumerPreferredLayers()")
        // TODO(feature): layer
    }

    @Async
    fun setConsumerPreferredLayers(
        consumerId: String?, spatialLayer: String?, temporalLayer: String?
    ) {
        Logger.d(TAG, "setConsumerPreferredLayers()")
        // TODO: layer
    }

    @Async
    fun requestConsumerKeyFrame(consumerId: String?) {
        Logger.d(TAG, "requestConsumerKeyFrame()")
        mWorkHandler.post {
            try {
                socketManager?.syncRequest(
                    "requestConsumerKeyFrame"
                ) { req -> jsonPut(req, "consumerId", "consumerId") }
                mStore.addNotify("Keyframe requested for video consumer")
            } catch (e: ProtooException) {
                e.printStackTrace()
                logError("restartIce() | failed:", e)
                mStore.addNotify("error", "ICE restart failed: " + e.message)
            }
        }
    }

    @Async
    fun enableChatDataProducer() {
        Logger.d(TAG, "enableChatDataProducer()")
        // TODO(feature): data channel
    }

    @Async
    fun enableBotDataProducer() {
        Logger.d(TAG, "enableBotDataProducer()")
        // TODO(feature): data channel
    }

    @Async
    fun sendChatMessage(txt: String?) {
        Logger.d(TAG, "sendChatMessage()")
        // TODO(feature): data channel
    }

    @Async
    fun sendBotMessage(txt: String?) {
        Logger.d(TAG, "sendBotMessage()")
        // TODO(feature): data channel
    }

    @Async
    fun changeDisplayName(displayName: String?) {
        Logger.d(TAG, "changeDisplayName()")

        // Store in cookie.
        mPreferences.edit().putString("displayName", displayName).apply()
        mWorkHandler.post {
            try {
                socketManager?.syncRequest(
                    "changeDisplayName"
                ) { req -> jsonPut(req, "displayName", displayName) }
                mDisplayName = displayName
                mStore.setDisplayName(displayName)
                mStore.addNotify("Display name change")
            } catch (e: ProtooException) {
                e.printStackTrace()
                logError("changeDisplayName() | failed:", e)
                mStore.addNotify("error", "Could not change display name: " + e.message)

                // We need to refresh the component for it to render the previous
                // displayName again.
                mStore.setDisplayName(mDisplayName)
            }
        }
    }

    // TODO(feature): stats
    @get:Async
    val sendTransportRemoteStats: Unit
        get() {
            Logger.d(TAG, "getSendTransportRemoteStats()")
            // TODO(feature): stats
        }

    // TODO(feature): stats
    @get:Async
    val recvTransportRemoteStats: Unit
        get() {
            Logger.d(TAG, "getRecvTransportRemoteStats()")
            // TODO(feature): stats
        }

    // TODO(feature): stats
    @get:Async
    val audioRemoteStats: Unit
        get() {
            Logger.d(TAG, "getAudioRemoteStats()")
            // TODO(feature): stats
        }

    // TODO(feature): stats
    @get:Async
    val videoRemoteStats: Unit
        get() {
            Logger.d(TAG, "getVideoRemoteStats()")
            // TODO(feature): stats
        }

    @Async
    fun getConsumerRemoteStats(consumerId: String?) {
        Logger.d(TAG, "getConsumerRemoteStats()")
        // TODO(feature): stats
    }

    @Async
    fun getChatDataProducerRemoteStats(consumerId: String?) {
        Logger.d(TAG, "getChatDataProducerRemoteStats()")
        // TODO(feature): stats
    }

    // TODO(feature): stats
    @get:Async
    val botDataProducerRemoteStats: Unit
        get() {
            Logger.d(TAG, "getBotDataProducerRemoteStats()")
            // TODO(feature): stats
        }

    @Async
    fun getDataConsumerRemoteStats(dataConsumerId: String?) {
        Logger.d(TAG, "getDataConsumerRemoteStats()")
        // TODO(feature): stats
    }

    // TODO(feature): stats
    @get:Async
    val sendTransportLocalStats: Unit
        get() {
            Logger.d(TAG, "getSendTransportLocalStats()")
            // TODO(feature): stats
        }

    /// TODO(feature): stats
    @get:Async
    val recvTransportLocalStats: Unit
        get() {
            Logger.d(TAG, "getRecvTransportLocalStats()")
            /// TODO(feature): stats
        }

    // TODO(feature): stats
    @get:Async
    val audioLocalStats: Unit
        get() {
            Logger.d(TAG, "getAudioLocalStats()")
            // TODO(feature): stats
        }

    // TODO(feature): stats
    @get:Async
    val videoLocalStats: Unit
        get() {
            Logger.d(TAG, "getVideoLocalStats()")
            // TODO(feature): stats
        }

    @Async
    fun getConsumerLocalStats(consumerId: String?) {
        Logger.d(TAG, "getConsumerLocalStats()")
        // TODO(feature): stats
    }

    @Async
    fun applyNetworkThrottle(uplink: String?, downlink: String?, rtt: String?, secret: String?) {
        Logger.d(TAG, "applyNetworkThrottle()")
        // TODO(feature): stats
    }

    @Async
    fun resetNetworkThrottle(silent: Boolean, secret: String?) {
        Logger.d(TAG, "applyNetworkThrottle()")
        // TODO(feature): stats
    }

    @Async
    fun close() {
        if (mClosed) {
            return
        }
        mClosed = true
        Logger.d(TAG, "close()")
        mWorkHandler.post {

            // Close mProtoo Protoo
            if (socketManager != null) {
                socketManager?.close()
                socketManager = null
            }

            // dispose all transport and device.
            disposeTransportDevice()

            // dispose audio track.
            if (mLocalAudioTrack != null) {
                mLocalAudioTrack!!.setEnabled(false)
                mLocalAudioTrack!!.dispose()
                mLocalAudioTrack = null
            }

            // dispose video track.
            if (mLocalVideoTrack != null) {
                mLocalVideoTrack?.setEnabled(false)
                mLocalVideoTrack?.dispose()
                mLocalVideoTrack = null
            }

            // dispose peerConnection.
            mPeerConnectionUtils?.dispose()

            // quit worker handler thread.
            mWorkHandler.looper.quit()
        }

        // dispose request.
        mCompositeDisposable.dispose()

        // Set room state.
        mStore.setRoomState(ConnectionState.CLOSED)
    }

    @WorkerThread
    private fun disposeTransportDevice() {
        Logger.d(TAG, "disposeTransportDevice()")
        // Close mediasoup Transports.
        if (mSendTransport != null) {
            mSendTransport?.close()
            mSendTransport?.dispose()
            mSendTransport = null
        }
        if (mRecvTransport != null) {
            mRecvTransport?.close()
            mRecvTransport?.dispose()
            mRecvTransport = null
        }

        // dispose device.
        if (mMediasoupDevice != null) {
            mMediasoupDevice!!.dispose()
            mMediasoupDevice = null
        }
    }

    private val peerListener = object : Peer.Listener {
        override fun onOpen() {
            Logger.d(TAG, "onOpen")
            mWorkHandler.post { joinImpl() }
        }

        override fun onFail() {
            mWorkHandler.post {
                mStore.addNotify("error", "WebSocket connection failed")
                mStore.setRoomState(ConnectionState.CONNECTING)
            }
        }

        override fun onRequest(
            request: Message.Request, handler: Peer.ServerRequestHandler
        ) {
            Logger.d(TAG, "onRequest() " + request.data.toString())
            mWorkHandler.post {
                try {
                    when (request.method) {
                        "newConsumer" -> {
                            onNewConsumer(request, handler)
                        }
                        "newDataConsumer" -> {
                            onNewDataConsumer(request, handler)
                        }
                        else -> {
                            handler.reject(403, "unknown protoo request.method " + request.method)
                            Logger.w(TAG, "unknown protoo request.method " + request.method)
                        }
                    }
                } catch (e: Exception) {
                    Logger.e(TAG, "handleRequestError.", e)
                }
            }
        }

        override fun onNotification(notification: Message.Notification) {
            Logger.d(
                TAG,
                "onNotification() "
                        + notification.method
                        + ", "
                        + notification.data.toString()
            )
            mWorkHandler.post {
                try {
                    handleNotification(notification)
                } catch (e: Exception) {
                    Logger.e(TAG, "handleNotification error.", e)
                }
            }
        }

        override fun onDisconnected() {
            mWorkHandler.post {
                mStore.addNotify("error", "WebSocket disconnected")
                mStore.setRoomState(ConnectionState.CONNECTING)

                // Close All Transports created by device.
                // All will reCreated After ReJoin.
                disposeTransportDevice()
            }
        }

        override fun onClose() {
            if (mClosed) {
                return
            }
            mWorkHandler.post {
                if (mClosed) {
                    return@post
                }
                close()
            }
        }
    }

    @WorkerThread
    private fun joinImpl() {
        Logger.d(TAG, "joinImpl()")
        try {
            mMediasoupDevice = Device()
            val routerRtpCapabilities: String = socketManager!!.syncRequest("getRouterRtpCapabilities")
            mMediasoupDevice!!.load(routerRtpCapabilities)
            val rtpCapabilities = mMediasoupDevice!!.rtpCapabilities

            // Create mediasoup Transport for sending (unless we don't want to produce).
            if (mOptions.isProduce) {
                createSendTransport()
            }

            // Create mediasoup Transport for sending (unless we don't want to consume).
            if (mOptions.isConsume) {
                createRecvTransport()
            }

            // Join now into the room.
            // TODO(HaiyangWu): Don't send our RTP capabilities if we don't want to consume.
            val joinResponse: String = socketManager!!.syncRequest(
                "join"
            ) { req ->
                jsonPut(req, "displayName", mDisplayName)
                jsonPut(req, "avatarUrl", avatarUrl)
                jsonPut(req, "device", mOptions.device.toJSONObject())
                jsonPut(req, "rtpCapabilities", toJsonObject(rtpCapabilities))
                // TODO (HaiyangWu): add sctpCapabilities
                jsonPut(req, "sctpCapabilities", "")
            }
            mStore.setRoomState(ConnectionState.CONNECTED)
            mStore.addNotify("You are in the room!", 3000)
            val resObj: JSONObject = toJsonObject(joinResponse)
            val peers: JSONArray = resObj.optJSONArray("peers")
            var i = 0
            while (i < peers.length()) {
                val peer: JSONObject = peers.getJSONObject(i)
                mStore.addPeer(peer.optString("id"), peer)
                i++
            }

            // Enable mic/webcam.
            if (mOptions.isProduce) {
                val canSendMic = mMediasoupDevice!!.canProduce("audio")
                val canSendCam = mMediasoupDevice!!.canProduce("video")
                mStore.setMediaCapabilities(canSendMic, canSendCam)
                mMainHandler.post { enableMic() }
                mMainHandler.post { enableCam() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            logError("joinRoom() failed:", e)
            if (TextUtils.isEmpty(e.message)) {
                mStore.addNotify("error", "Could not join the room, internal error")
            } else {
                mStore.addNotify("error", "Could not join the room: " + e.message)
            }
            mMainHandler.post { close() }
        }
    }

    @WorkerThread
    private fun enableMicImpl() {
        Logger.d(TAG, "enableMicImpl()")
        try {
            if (mMicProducer != null) {
                return
            }
            if (!mMediasoupDevice!!.isLoaded) {
                Logger.w(TAG, "enableMic() | not loaded")
                return
            }
            if (!mMediasoupDevice!!.canProduce("audio")) {
                Logger.w(TAG, "enableMic() | cannot produce audio")
                return
            }
            if (mSendTransport == null) {
                Logger.w(TAG, "enableMic() | mSendTransport doesn't ready")
                return
            }
            if (mLocalAudioTrack == null) {
                mLocalAudioTrack = mPeerConnectionUtils?.createAudioTrack(mContext, "mic")
                mLocalAudioTrack!!.setEnabled(true)
            }
            mMicProducer = mSendTransport!!.produce(
                {
                    Logger.e(TAG, "onTransportClose(), micProducer")
                    mMicProducer?.let {
                        mStore.removeProducer(it.id)
                        mMicProducer = null
                    }
                },
                mLocalAudioTrack,
                null,
                null
            )
            mStore.addProducer(mMicProducer)
        } catch (e: MediasoupException) {
            e.printStackTrace()
            logError("enableMic() | failed:", e)
            mStore.addNotify("error", "Error enabling microphone: " + e.message)
            if (mLocalAudioTrack != null) {
                mLocalAudioTrack!!.setEnabled(false)
            }
        }
    }

    @WorkerThread
    private fun disableMicImpl() {
        Logger.d(TAG, "disableMicImpl()")
        if (mMicProducer == null) {
            return
        }
        mMicProducer?.close()
        mStore.removeProducer(mMicProducer?.id)
        try {
            socketManager?.syncRequest("closeProducer") { req ->
                jsonPut(
                    req,
                    "producerId",
                    mMicProducer?.id
                )
            }
        } catch (e: ProtooException) {
            e.printStackTrace()
            mStore.addNotify("error", "Error closing server-side mic Producer: " + e.message)
        }
        mMicProducer = null
    }

    @WorkerThread
    private fun muteMicImpl() {
        Logger.d(TAG, "muteMicImpl()")
        mMicProducer?.pause()
        try {
            socketManager?.syncRequest("pauseProducer") { req ->
                jsonPut(
                    req,
                    "producerId",
                    mMicProducer?.id
                )
            }
            mStore.setProducerPaused(mMicProducer?.id)
        } catch (e: ProtooException) {
            e.printStackTrace()
            logError("muteMic() | failed:", e)
            mStore.addNotify("error", "Error pausing server-side mic Producer: " + e.message)
        }
    }

    @WorkerThread
    private fun unmuteMicImpl() {
        Logger.d(TAG, "unmuteMicImpl()")
        mMicProducer?.resume()
        try {
            socketManager?.syncRequest(
                "resumeProducer"
            ) { req -> jsonPut(req, "producerId", mMicProducer?.id) }
            mStore.setProducerResumed(mMicProducer?.id)
        } catch (e: ProtooException) {
            e.printStackTrace()
            logError("unmuteMic() | failed:", e)
            mStore.addNotify("error", "Error resuming server-side mic Producer: " + e.message)
        }
    }

    @WorkerThread
    private fun enableCamImpl() {
        Logger.d(TAG, "enableCamImpl()")
        try {
            if (mCamProducer != null) {
                return
            }
            if (!mMediasoupDevice!!.isLoaded) {
                Logger.w(TAG, "enableCam() | not loaded")
                return
            }
            if (!mMediasoupDevice!!.canProduce("video")) {
                Logger.w(TAG, "enableCam() | cannot produce video")
                return
            }
            if (mSendTransport == null) {
                Logger.w(TAG, "enableCam() | mSendTransport doesn't ready")
                return
            }
            if (mLocalVideoTrack == null) {
                mLocalVideoTrack = mPeerConnectionUtils?.createVideoTrack(mContext, "cam")
                mLocalVideoTrack!!.setEnabled(true)
            }
            mCamProducer = mSendTransport?.produce(
                Producer.Listener { producer: Producer? ->
                    Logger.e(TAG, "onTransportClose(), camProducer")
                    if (mCamProducer != null) {
                        mStore.removeProducer(mCamProducer?.id)
                        mCamProducer = null
                    }
                },
                mLocalVideoTrack,
                null,
                null
            )
            mStore.addProducer(mCamProducer)
        } catch (e: MediasoupException) {
            e.printStackTrace()
            logError("enableWebcam() | failed:", e)
            mStore.addNotify("error", "Error enabling webcam: " + e.message)
            if (mLocalVideoTrack != null) {
                mLocalVideoTrack!!.setEnabled(false)
            }
        }
    }

    @WorkerThread
    private fun disableCamImpl() {
        Logger.d(TAG, "disableCamImpl()")
        if (mCamProducer == null) {
            return
        }
        mCamProducer?.close()
        mStore.removeProducer(mCamProducer?.id)
        try {
            socketManager?.syncRequest("closeProducer") { req ->
                jsonPut(
                    req,
                    "producerId",
                    mCamProducer?.id
                )
            }
        } catch (e: ProtooException) {
            e.printStackTrace()
            mStore.addNotify("error", "Error closing server-side webcam Producer: " + e.message)
        }
        mCamProducer = null
    }

    @WorkerThread
    @Throws(ProtooException::class, JSONException::class, MediasoupException::class)
    private fun createSendTransport() {
        Logger.d(TAG, "createSendTransport()")
        val res: String = socketManager!!.syncRequest(
            "createWebRtcTransport"
        ) { req ->
            jsonPut(req, "forceTcp", mOptions.isForceTcp)
            jsonPut(req, "producing", true)
            jsonPut(req, "consuming", false)
            // TODO: sctpCapabilities
            jsonPut(req, "sctpCapabilities", "")
        }
        val info = JSONObject(res)
        Logger.d(TAG, "device#createSendTransport() $info")
        val id: String = info.optString("id")
        val iceParameters: String = info.optString("iceParameters")
        val iceCandidates: String = info.optString("iceCandidates")
        val dtlsParameters: String = info.optString("dtlsParameters")
        val sctpParameters: String = info.optString("sctpParameters")
        mSendTransport = mMediasoupDevice!!.createSendTransport(
            sendTransportListener, id, iceParameters, iceCandidates, dtlsParameters
        )
    }

    @WorkerThread
    @Throws(ProtooException::class, JSONException::class, MediasoupException::class)
    private fun createRecvTransport() {
        Logger.d(TAG, "createRecvTransport()")
        val res: String = socketManager!!.syncRequest(
            "createWebRtcTransport"
        ) { req ->
            jsonPut(req, "forceTcp", mOptions.isForceTcp)
            jsonPut(req, "producing", false)
            jsonPut(req, "consuming", true)
            // TODO (HaiyangWu): add sctpCapabilities
            jsonPut(req, "sctpCapabilities", "")
        }
        val info = JSONObject(res)
        Logger.d(TAG, "device#createRecvTransport() $info")
        val id: String = info.optString("id")
        val iceParameters: String = info.optString("iceParameters")
        val iceCandidates: String = info.optString("iceCandidates")
        val dtlsParameters: String = info.optString("dtlsParameters")
        val sctpParameters: String = info.optString("sctpParameters")
        mRecvTransport = mMediasoupDevice!!.createRecvTransport(
            recvTransportListener, id, iceParameters, iceCandidates, dtlsParameters, null
        )
    }

    private val sendTransportListener: SendTransport.Listener = object : SendTransport.Listener {
        private val listenerTAG: String = TAG + "_SendTrans"
        override fun onProduce(
            transport: Transport, kind: String, rtpParameters: String, appData: String
        ): String {
            if (mClosed) {
                return ""
            }
            Logger.d(listenerTAG, "onProduce() ")
            val producerId = fetchProduceId { req ->
                jsonPut(req, "transportId", transport.id)
                jsonPut(req, "kind", kind)
                jsonPut(req, "rtpParameters", toJsonObject(rtpParameters))
                jsonPut(req, "appData", appData)
            }
            Logger.d(listenerTAG, "producerId: $producerId")
            return producerId
        }

        override fun onConnect(transport: Transport, dtlsParameters: String) {
            if (mClosed) {
                return
            }
            Logger.d(listenerTAG + "_send", "onConnect()")
            mCompositeDisposable.add(
                socketManager!!
                    .request(
                        "connectWebRtcTransport"
                    ) { req ->
                        jsonPut(req, "transportId", transport.id)
                        jsonPut(req, "dtlsParameters", toJsonObject(dtlsParameters))
                    }
                    .subscribe(
                        { d -> Logger.d(listenerTAG, "connectWebRtcTransport res: $d") }
                    ) { t -> logError("connectWebRtcTransport for mSendTransport failed", t) })
        }

        override fun onConnectionStateChange(transport: Transport, connectionState: String) {
            Logger.d(listenerTAG, "onConnectionStateChange: $connectionState")
        }
    }
    private val recvTransportListener: RecvTransport.Listener = object : RecvTransport.Listener {
        private val listenerTAG: String = TAG.toString() + "_RecvTrans"
        override fun onConnect(transport: Transport, dtlsParameters: String) {
            if (mClosed) {
                return
            }
            Logger.d(listenerTAG, "onConnect()")
            mCompositeDisposable.add(
                socketManager!!.request("connectWebRtcTransport") { req ->
                    jsonPut(req, "transportId", transport.id)
                    jsonPut(req, "dtlsParameters", toJsonObject(dtlsParameters))
                }.subscribe(
                    { d -> Logger.d(listenerTAG, "connectWebRtcTransport res: $d") })
                { t -> logError("connectWebRtcTransport for mRecvTransport failed", t) }
            )
        }

        override fun onConnectionStateChange(transport: Transport, connectionState: String) {
            Logger.d(listenerTAG, "onConnectionStateChange: $connectionState")
        }
    }

    private fun fetchProduceId(generator: (JSONObject) -> Unit): String {
        Logger.d(TAG, "fetchProduceId:()")
        return try {
            val response: String = socketManager!!.syncRequest("produce", generator)
            JSONObject(response).optString("id")
        } catch (e: ProtooException) {
            e.printStackTrace()
            logError("send produce request failed", e)
            ""
        } catch (e: JSONException) {
            e.printStackTrace()
            logError("send produce request failed", e)
            ""
        }
    }

    private fun logError(message: String, throwable: Throwable) {
        Logger.e(TAG, message, throwable)
    }

    private fun onNewConsumer(request: Message.Request, handler: Peer.ServerRequestHandler) {
        if (!mOptions.isConsume) {
            handler.reject(403, "I do not want to consume")
            return
        }
        try {
            val data: JSONObject = request.data
            val peerId: String = data.optString("peerId")
            val producerId: String = data.optString("producerId")
            val id: String = data.optString("id")
            val kind: String = data.optString("kind")
            val rtpParameters: String = data.optString("rtpParameters")
            val type: String = data.optString("type")
            val appData: String = data.optString("appData")
            val producerPaused: Boolean = data.optBoolean("producerPaused")
            val consumer: Consumer = mRecvTransport!!.consume(
                { c: Consumer ->
                    mConsumers.remove(c.id)
                    Logger.w(TAG, "onTransportClose for consume")
                },
                id,
                producerId,
                kind,
                rtpParameters,
                appData
            )
            mConsumers[consumer.id] = ConsumerHolder(peerId, consumer)
            mStore.addConsumer(peerId, type, consumer, producerPaused)

            // We are ready. Answer the protoo request so the server will
            // resume this Consumer (which was paused for now if video).
            handler.accept()

            // If audio-only mode is enabled, pause it.
            if ("video" == consumer.kind && mStore.getMe().value.isAudioOnly) {
                pauseConsumer(consumer)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            logError("\"newConsumer\" request failed:", e)
            mStore.addNotify("error", "Error creating a Consumer: " + e.message)
        }
    }

    private fun onNewDataConsumer(request: Message.Request, handler: Peer.ServerRequestHandler) {
        handler.reject(403, "I do not want to data consume")
        // TODO(HaiyangWu): support data consume
    }

    @WorkerThread
    private fun pauseConsumer(consumer: Consumer) {
        Logger.d(TAG, "pauseConsumer() " + consumer.id)
        if (consumer.isPaused) {
            return
        }
        try {
            socketManager?.syncRequest("pauseConsumer") { req -> jsonPut(req, "consumerId", consumer.id) }
            consumer.pause()
            mStore.setConsumerPaused(consumer.id, "local")
        } catch (e: ProtooException) {
            e.printStackTrace()
            logError("pauseConsumer() | failed:", e)
            mStore.addNotify("error", "Error pausing Consumer: " + e.message)
        }
    }

    @WorkerThread
    private fun resumeConsumer(consumer: Consumer) {
        Logger.d(TAG, "resumeConsumer() " + consumer.id)
        if (!consumer.isPaused) {
            return
        }
        try {
            socketManager?.syncRequest("resumeConsumer") { req -> jsonPut(req, "consumerId", consumer.id) }
            consumer.resume()
            mStore.setConsumerResumed(consumer.id, "local")
        } catch (e: Exception) {
            e.printStackTrace()
            logError("resumeConsumer() | failed:", e)
            mStore.addNotify("error", "Error resuming Consumer: " + e.message)
        }
    }

    fun getInvitationLink(): String{
        return UrlFactory.getInvitationLink(callConfig)
    }
}