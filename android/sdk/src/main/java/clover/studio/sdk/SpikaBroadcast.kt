package clover.studio.sdk

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.os.bundleOf
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import clover.studio.sdk.model.LocalStream
import clover.studio.sdk.model.Participant
import clover.studio.sdk.model.ServerInfo
import clover.studio.sdk.model.UserInformation
import clover.studio.sdk.service.CallServiceImpl
import clover.studio.sdk.service.USER_INFO
import clover.studio.sdk.utils.UrlFactory
import clover.studio.sdk.viewmodel.DeviceState
import clover.studio.sdk.viewmodel.MeProps
import clover.studio.sdk.viewmodel.PeerProps
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions
import org.mediasoup.droid.Logger
import org.mediasoup.droid.MediasoupClient

interface SpikaBroadcastListener {
    fun onLocalStreamChanged(localStream: LocalStream)
    fun onRemoteStreamsChanged(consumers: List<PeerProps>)
    fun microphoneStateChanged(enabled: Boolean)
    fun cameraStateChanged(enabled: Boolean)
    fun speakerStateChanged(enabled: Boolean)
    fun callClosed()
}

// TODO Consider moving to a builder pattern
open class SpikaBroadcast(
    private val applicationContext: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val userInformation: UserInformation,
    private val spikaBroadcastListener: SpikaBroadcastListener?,
    serverInfo: ServerInfo,
) {

    protected var callService: CallServiceImpl? = null
    protected var callServiceBound = false

    init {
        MediasoupClient.initialize(applicationContext)
        Logger.setLogLevel(Logger.LogLevel.LOG_DEBUG)
        Logger.setDefaultHandler()

        // Initialize server data in UrlFactory singleton
        UrlFactory.setServerData(serverInfo.hostName, serverInfo.port)

        checkPermission()
    }

    private fun checkPermission() {
        val permissions = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )

        val rationale = "Please provide permissions"
        val options: Permissions.Options =
            Permissions.Options()
                .setRationaleDialogTitle("Info")
                .setSettingsDialogTitle("Warning")
                .setCreateNewTask(true)
        Permissions.check(
            applicationContext,
            permissions,
            rationale,
            options,
            object : PermissionHandler() {
                override fun onGranted() {
                    connect()
                }
            })
    }

    fun connect() {
        startCallService()
    }

    fun pause() {
        unbindCallService()
    }

    fun resume() {
        bindCallService()
    }

    fun disconnect() {
        stopCallService()
    }

    private fun startCallService() {
        CallServiceImpl.startService(
            applicationContext,
            CallServiceImpl.Action.JOIN_CALL,
            bundleOf(
                Pair(USER_INFO, userInformation)
            )
        )
    }

    private fun stopCallService() {
        CallServiceImpl.stopService(applicationContext)
    }

    private fun bindCallService() {
        applicationContext.bindService(
            Intent(applicationContext, CallServiceImpl::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    private fun unbindCallService() {
        if (callServiceBound) {
            applicationContext.unbindService(serviceConnection)
        }
    }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            callServiceBound = false
            onServiceConnectionUpdate(callServiceBound)
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder: CallServiceImpl.CallServiceBinder =
                service as CallServiceImpl.CallServiceBinder
            callService = binder.getService()
            callServiceBound = true
            observeMe()
            onServiceConnectionUpdate(callServiceBound)
        }
    }

    private fun observeMe() {
        callService?.let {
            it.getMe().observe(
                lifecycleOwner,
                { me -> onMeUpdate(me) }
            )
        }
    }

    protected open fun onServiceConnectionUpdate(isConnected: Boolean) {}

    protected open fun onMeUpdate(me: MeProps) {
        val cameraEnabled = me.cameraState == DeviceState.ON
        val microphoneEnabled = me.microphoneState == DeviceState.ON
        spikaBroadcastListener?.onLocalStreamChanged(
            LocalStream(me.videoTrack, cameraEnabled, microphoneEnabled)
        )
    }

    internal fun switchCamera() {
        callService?.switchCamera()
    }

    fun toggleMicrophoneState() {
        callService?.toggleMicrophoneState()
    }

    fun toggleCameraState() {
        callService?.toggleCameraState()
    }

    fun toggleSpeakerState(): Boolean {
        val newSpeakerState = callService?.toggleSpeakerState() ?: false
        spikaBroadcastListener?.speakerStateChanged(newSpeakerState)
        return  newSpeakerState
    }

    fun getParticipantsLiveData(): LiveData<List<Participant>>? {
        return callService?.getCallParticipants()
    }

    fun endCall() {
        callService?.endCall()
    }

    companion object {
        const val TAG = "SpikaBroadcast"
    }
}