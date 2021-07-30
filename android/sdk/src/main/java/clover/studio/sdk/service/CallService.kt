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
import clover.studio.sdk.model.Consumers
import clover.studio.sdk.model.Participant
import clover.studio.sdk.model.Peers
import clover.studio.sdk.model.Producers
import clover.studio.sdk.viewmodel.CombinedLiveData
import clover.studio.sdk.viewmodel.DeviceState
import clover.studio.sdk.viewmodel.MeProps
import io.reactivex.Observable
import org.mediasoup.droid.Logger


private const val CHANNEL_ID = "Call service"
private const val NOTIFICATION_ID = 400

const val EXTRA_ACTION = "action"
const val ROOM_ID = "roomId"

interface CallService {
    fun getProducers(): SupplierMutableLiveData<Producers>
    fun getPeers(): SupplierMutableLiveData<Peers>
    fun getConsumers(): SupplierMutableLiveData<Consumers>
    fun getRoomStore(): RoomStore
    fun getMe(): SupplierMutableLiveData<MeProps>
    fun getCallParticipants(): LiveData<List<Participant>>
    fun switchCamera()
    fun toggleCameraState()
    fun toggleMicrophoneState()
    fun toggleSpeakerState()
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
        JOIN_CALL,
        HANG_UP,
        MUTE_AUDIO
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

        fun sendAction(context: Context, action: Action) {
            val actionIntent = Intent(context, CallServiceImpl::class.java)
            actionIntent.putExtra(EXTRA_ACTION, action)
            ContextCompat.startForegroundService(context, actionIntent)
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
                callConfig.roomId = intent.extras?.getString(ROOM_ID)
                createRoom()
            }
            Action.MUTE_AUDIO -> {
                roomClient?.muteMic()
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
        val peerId: String = getRandomString(8),
        val displayName: String = getRandomString(8),
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