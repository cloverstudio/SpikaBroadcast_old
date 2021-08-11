package clover.studio.sdk.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import clover.studio.clovermediasouppoc.utils.PeerConnectionUtils
import clover.studio.clovermediasouppoc.utils.Utils.getRandomString
import clover.studio.sdk.R
import clover.studio.sdk.call.*
import clover.studio.sdk.model.*
import clover.studio.sdk.viewmodel.CombinedLiveData
import clover.studio.sdk.viewmodel.DeviceState
import clover.studio.sdk.viewmodel.MeProps
import clover.studio.sdk.viewmodel.PeerProps
import io.reactivex.Observable
import org.mediasoup.droid.Logger
import org.webrtc.AudioTrack
import org.webrtc.VideoTrack
import java.util.HashSet


private const val CHANNEL_ID = "Call service"
private const val NOTIFICATION_ID = 400

const val EXTRA_ACTION = "action"
const val USER_INFO = "userInfo"

interface CallService {
    fun getProducers(): SupplierMutableLiveData<Producers>
    fun getPeers(): SupplierMutableLiveData<Peers>
    fun getConsumers(): SupplierMutableLiveData<Consumers>
    fun getRoomStore(): RoomStore
    fun getMe(): SupplierMutableLiveData<MeProps>

    /**
     * Returns a LiveData object that contains a list of all the call participants
     * (including the self user)
     */
    fun getCallParticipants(): LiveData<List<Participant>>

    /**
     * Returns a LiveData object that contains a list of all the call peers. This list contains
     * the streams and peer status for displaying.
     */
    fun getCallPeers(): LiveData<List<PeerProps>>

    /**
     * Switches the currently used camera to the next one in the camera list
     */
    fun switchCamera()

    /**
     * Toggles the camera state between enabled and disabled.
     */
    fun toggleCameraState()

    /**
     * Toggles the microphone state between enabled and disabled.
     */
    fun toggleMicrophoneState()

    /**
     * Toggles the speaker state between enabled and disabled.
     */
    fun toggleSpeakerState()

    /**
     * Ends the current call.
     */
    fun endCall()
}

class CallServiceImpl : LifecycleService(), CallService {

    private val binder = CallServiceBinder()
    private val roomOptions: RoomOptions = RoomOptions()
    private var roomStore: RoomStore = RoomStore()
    private var roomClient: RoomClient? = null
    private var me: MeProps = MeProps(roomStore)
    private val meLiveData = SupplierMutableLiveData<MeProps> { me }

    private var callConfig: CallConfig = CallConfig()
    private var notificationConfig: NotificationConfig = NotificationConfig()

    enum class Action {
        JOIN_CALL
    }

    companion object {
        fun startService(context: Context, action: Action, extras: Bundle? = null) {
            val startIntent = Intent(context, CallServiceImpl::class.java)
            startIntent.putExtra(EXTRA_ACTION, action)
            extras?.let { startIntent.putExtras(it) }
            ContextCompat.startForegroundService(context, startIntent)
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, CallServiceImpl::class.java)
            context.stopService(stopIntent)
        }
    }

    override fun onDestroy() {
        roomClient?.close()
        roomClient = null
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.getSerializableExtra(EXTRA_ACTION)) {
            Action.JOIN_CALL -> {
                val userInfo = intent.extras?.getParcelable<UserInformation>(USER_INFO)
                callConfig.roomId = userInfo?.roomId
                callConfig.avatarUrl = userInfo?.avatarUrl
                callConfig.displayName = userInfo?.displayName
                createRoom()
            }
        }

        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }


    private fun createRoom() {
        Log.d(RoomMessageHandler.TAG, "RoomClient init start")
        // Room action config.
        roomOptions.setProduce(true)
        roomOptions.setConsume(true)
        roomOptions.setForceTcp(false)
        PeerConnectionUtils.setPreferCameraFace("front")

        Logger.d(RoomMessageHandler.TAG, "RoomClient init start")
        roomClient = RoomClient(this, roomStore, callConfig, roomOptions)
        me.connect(this, {
            meLiveData.postValue { me }
        })
        Logger.d(RoomMessageHandler.TAG, "RoomClient init end")

        Observable.fromCallable { roomClient?.join() }.subscribe()

    }

    private fun createNotification(): Notification {
        createNotificationChannel()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(notificationConfig.title)
            .setContentText(notificationConfig.content)
            .setSmallIcon(notificationConfig.iconResId)
            .setContentIntent(notificationConfig.clickIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID, "Call Notification",
                NotificationManager.IMPORTANCE_MIN
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager!!.createNotificationChannel(serviceChannel)
        }
    }

    data class CallConfig(
        var roomId: String? = "room",
        var avatarUrl: String? = null,
        val peerId: String = getRandomString(8),
        var displayName: String? = getRandomString(8),
        val forceH264: Boolean = false,
        val forceVP9: Boolean = false
    )

    data class NotificationConfig(
        val title: String? = "Call in progress",
        val content: String? = null,
        val iconResId: Int = R.drawable.buddy,
        val clickIntent: PendingIntent? = null
    )

    inner class CallServiceBinder : Binder() {
        fun getService(): CallServiceImpl = this@CallServiceImpl
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun getRoomStore(): RoomStore {
        return roomStore
    }

    override fun getProducers(): SupplierMutableLiveData<Producers> {
        return roomStore.getProducers()
    }

    override fun getPeers(): SupplierMutableLiveData<Peers> {
        return roomStore.getPeers()
    }

    override fun getConsumers(): SupplierMutableLiveData<Consumers> {
        return roomStore.getConsumers()
    }

    override fun getMe(): SupplierMutableLiveData<MeProps> {
        return meLiveData
    }

    override fun getCallParticipants(): LiveData<List<Participant>> {
        return CombinedLiveData(roomStore.getPeers(), roomStore.getMe()) { peers, me ->
            val participantsList = mutableListOf<Participant>()

            peers?.let {
                for (peer in it.allPeers) {
                    participantsList.add(
                        Participant(
                            displayName = peer.displayName
                        )
                    )
                }
            }

            me?.let {
                participantsList.add(
                    Participant(
                        it.displayName,
                        null,
                        true
                    )
                )
            }

            return@CombinedLiveData participantsList
        }
    }

    override fun getCallPeers(): LiveData<List<PeerProps>> {
        return CombinedLiveData(roomStore.getConsumers(), roomStore.getPeers()) { consumers, peers ->
            val peerList = mutableListOf<PeerProps>()
            if (consumers == null || peers == null) {
                return@CombinedLiveData emptyList<PeerProps>()
            }

            var videoConsumerWrapper: Consumers.ConsumerWrapper? = null
            var audioConsumerWrapper: Consumers.ConsumerWrapper? = null

            for (peer in peers.allPeers) {
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

                val peerProps = PeerProps(roomStore)
                peerProps.videoConsumerId = videoConsumerWrapper?.consumer?.id
                peerProps.videoRtpParameters = videoConsumerWrapper?.consumer?.rtpParameters
                peerProps.videoTrack =
                    if (videoConsumerWrapper != null) videoConsumerWrapper.consumer.track as VideoTrack else null
                peerProps.videoScore = videoConsumerWrapper?.score
                peerProps.videoVisible = if (videoConsumerWrapper != null) {
                    !videoConsumerWrapper.isLocallyPaused && !videoConsumerWrapper.isRemotelyPaused
                } else {
                    false
                }

                peerProps.audioConsumerId = audioConsumerWrapper?.consumer?.id
                peerProps.audioRtpParameters = audioConsumerWrapper?.consumer?.rtpParameters
                peerProps.audioTrack =
                    if (audioConsumerWrapper != null) audioConsumerWrapper.consumer.track as AudioTrack else null
                peerProps.audioScore = audioConsumerWrapper?.score
                peerProps.audioEnabled = if (audioConsumerWrapper != null) {
                    !audioConsumerWrapper.isLocallyPaused && !audioConsumerWrapper.isRemotelyPaused
                } else {
                    false
                }
                peerList.add(peerProps)
            }
            return@CombinedLiveData peerList
        }

    }

    override fun switchCamera() {
        roomClient?.changeCam()
    }

    override fun toggleCameraState() {
        if (me.cameraState == DeviceState.ON) {
            roomClient?.disableCam()
        } else {
            roomClient?.enableCam()
        }
    }

    override fun toggleMicrophoneState() {
        if (me.microphoneState == DeviceState.ON) {
            roomClient?.muteMic()
        } else {
            roomClient?.unmuteMic()
        }
    }

    override fun toggleSpeakerState() {
        val audioManager = this.getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.isSpeakerphoneOn = !audioManager.isSpeakerphoneOn
    }

    override fun endCall() {
        roomClient?.close()
    }
}