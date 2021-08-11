package clover.studio.sdk

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.GridLayoutManager
import clover.studio.clovermediasouppoc.utils.PeerConnectionUtils
import clover.studio.sdk.adapter.PeerAdapter
import clover.studio.sdk.call.ParticipantsFragment
import clover.studio.sdk.databinding.CallViewBinding
import clover.studio.sdk.model.Participant
import clover.studio.sdk.model.ServerInfo
import clover.studio.sdk.model.UserInformation
import clover.studio.sdk.service.CallServiceImpl
import clover.studio.sdk.service.USER_INFO
import clover.studio.sdk.utils.UrlFactory
import clover.studio.sdk.viewmodel.DeviceState
import clover.studio.sdk.viewmodel.PeerProps
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions
import org.mediasoup.droid.Logger
import org.mediasoup.droid.MediasoupClient
//import org.mediasoup.droid.Producer
import org.webrtc.RendererCommon

interface SpikaBroadcastListener {
//    fun onLocalVideoStreamStart(producer: Producer)
    fun onRemoteStreamsChanged(consumers: List<PeerProps>)
    fun microphoneStateChanged(enabled: Boolean)
    fun cameraStateChanged(enabled: Boolean)
    fun speakerStateChanged(enabled: Boolean)
    fun callClosed()
}

// TODO Consider moving to a builder pattern
class SpikaBroadcast(
    private val applicationContext: AppCompatActivity,
    private val lifecycleOwner: LifecycleOwner,
    private val userInformation: UserInformation,
    private val spikaBroadcastListener: SpikaBroadcastListener?,
    viewContainer: ViewGroup,
    serverInfo: ServerInfo
) {

    private var binding: CallViewBinding
    private var mPeerAdapter: PeerAdapter? = null

    private var callService: CallServiceImpl? = null
    private var callServiceBound = false

    init {
        MediasoupClient.initialize(applicationContext)
        Logger.setLogLevel(Logger.LogLevel.LOG_DEBUG)
        Logger.setDefaultHandler()

        // Initialize server data in UrlFactory singleton
        UrlFactory.setServerData(serverInfo.hostName, serverInfo.port)

        binding = CallViewBinding.inflate((LayoutInflater.from(applicationContext)))
        viewContainer.addView(binding.root)

        initViews()
        checkPermission()
    }

    private fun initViews() {
        binding.pipVideoView.init(PeerConnectionUtils.eglContext, null)
        binding.pipVideoView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        binding.pipVideoView.setZOrderMediaOverlay(true)

        binding.btnSwitchCamera.setOnClickListener {
            switchCamera()
        }

        binding.btnAudio.setOnClickListener {
            callService?.toggleMicrophoneState()
        }

        binding.btnVideo.setOnClickListener {
            callService?.toggleCameraState()
        }

        binding.btnEndCall.setOnClickListener {
            callService?.endCall()
            spikaBroadcastListener?.callClosed()
        }

        binding.btnParticipants.setOnClickListener {
            ParticipantsFragment(
                getParticipantsLiveData()
            ).show(applicationContext.supportFragmentManager, null)
        }
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
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder: CallServiceImpl.CallServiceBinder =
                service as CallServiceImpl.CallServiceBinder
            callService = binder.getService()
            callServiceBound = true
            observeService()
        }
    }

    private fun observeService() {
        callService?.let {
            mPeerAdapter = PeerAdapter(it.getRoomStore(), lifecycleOwner)
            binding.rvPeers.layoutManager = GridLayoutManager(applicationContext, 1)
            binding.rvPeers.adapter = mPeerAdapter
            observeMe()
        }
    }

    private fun observeMe() {
        callService?.getMe()?.observe(
            lifecycleOwner,
            { me ->
                me.videoTrack?.addSink(binding.pipVideoView)

                if (me.cameraState == DeviceState.ON) {
                    binding.btnVideo.setImageResource(R.drawable.camera_disabled)
                } else {
                    binding.btnVideo.setImageResource(R.drawable.camera_enabled)
                }

                if (me.microphoneState == DeviceState.ON) {
                    binding.btnAudio.setImageResource(R.drawable.microphone_disabled)
                } else {
                    binding.btnAudio.setImageResource(R.drawable.microphone_enabled)
                }
            }
        )
    }

    private fun switchCamera() {
        callService?.switchCamera()
    }

    fun toggleMicrophoneState(){
        callService?.toggleMicrophoneState()
    }

    fun toggleCameraState(){
        callService?.toggleCameraState()
    }

    fun toggleSpeakerState(){
        callService?.toggleSpeakerState()
    }

    fun getParticipantsLiveData(): LiveData<List<Participant>>? {
        return callService?.getCallParticipants()
    }

    fun endCall(){
        callService?.endCall()
    }

    companion object {
        const val TAG = "SpikaBroadcast"
    }
}