package clover.studio.sdk

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.GridLayoutManager
import clover.studio.clovermediasouppoc.utils.PeerConnectionUtils
import clover.studio.sdk.adapter.PeerAdapter
import clover.studio.sdk.call.ParticipantsFragment
import clover.studio.sdk.databinding.CallViewBinding
import clover.studio.sdk.model.ServerInfo
import clover.studio.sdk.model.UserInformation
import clover.studio.sdk.service.CallServiceImpl
import clover.studio.sdk.viewmodel.DeviceState
import clover.studio.sdk.viewmodel.MeProps
import org.webrtc.RendererCommon

// TODO Consider moving to a builder pattern
class SpikaBroadcastUi(
    private val activity: AppCompatActivity,
    private val lifecycleOwner: LifecycleOwner,
    private val spikaBroadcastListener: SpikaBroadcastListener?,
    userInformation: UserInformation,
    serverInfo: ServerInfo,
    viewContainer: ViewGroup,
    notificationConfig: CallServiceImpl.NotificationConfig
) : SpikaBroadcast(
    activity.applicationContext,
    lifecycleOwner,
    spikaBroadcastListener,
    userInformation,
    serverInfo,
    notificationConfig
) {

    private var binding: CallViewBinding =
        CallViewBinding.inflate((LayoutInflater.from(activity)))
    private var mPeerAdapter: PeerAdapter? = null

    init {
        viewContainer.addView(binding.root)
        initViews()
    }

    private fun initViews() {
        binding.topLayout.setBackgroundResource(R.color.black)

        binding.pipVideoView.init(PeerConnectionUtils.eglContext, null)
        binding.pipVideoView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        binding.pipVideoView.setZOrderMediaOverlay(true)

        binding.btnSwitchCamera.setOnClickListener {
            switchCamera()
        }

        binding.btnAudio.setOnClickListener {
            toggleMicrophoneState()
        }

        binding.btnVideo.setOnClickListener {
            toggleCameraState()
        }

        binding.btnSpeaker.setOnClickListener {
            val newSpeakerState = toggleSpeakerState()
            if (newSpeakerState) {
                binding.btnSpeaker.setImageResource(R.drawable.speaker_enabled)
            } else {
                binding.btnSpeaker.setImageResource(R.drawable.speaker_disabled)
            }
        }

        binding.btnEndCall.setOnClickListener {
            endCall()
            spikaBroadcastListener?.callClosed()
        }

        binding.btnParticipants.setOnClickListener {
            ParticipantsFragment(
                getParticipantsLiveData(),
                callService?.getInvitationLink()
            ).show(activity.supportFragmentManager, null)
        }
    }

    override fun onServiceConnectionUpdate(isConnected: Boolean) {
        super.onServiceConnectionUpdate(isConnected)

        if (isConnected
            && mPeerAdapter == null
            && callService != null
        ) {
            mPeerAdapter = PeerAdapter(callService!!.getRoomStore(), lifecycleOwner)
            binding.rvPeers.layoutManager = GridLayoutManager(activity, 1)
            binding.rvPeers.adapter = mPeerAdapter
        }
    }

    override fun onMeUpdate(me: MeProps) {
        me.videoTrack?.addSink(binding.pipVideoView)

        if (me.cameraState == DeviceState.ON) {
            binding.btnVideo.setImageResource(R.drawable.camera_enabled)
            binding.pipVideoView.visibility = View.VISIBLE
        } else {
            binding.btnVideo.setImageResource(R.drawable.camera_disabled)
            binding.pipVideoView.visibility = View.GONE
        }

        if (me.microphoneState == DeviceState.ON) {
            binding.btnAudio.setImageResource(R.drawable.microphone_enabled)
        } else {
            binding.btnAudio.setImageResource(R.drawable.microphone_disabled)
        }
        super.onMeUpdate(me)
    }

    companion object {
        const val TAG = "SpikaBroadcast"
    }
}