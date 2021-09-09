package clover.studio.spikabroadcastsdk.call

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import clover.studio.clovermediasouppoc.utils.PeerConnectionUtils
import clover.studio.sdk.SpikaBroadcast
import clover.studio.sdk.SpikaBroadcastListener
import clover.studio.spikabroadcastsdk.call.adapter.PeerAdapter
import clover.studio.sdk.call.ParticipantsFragment
import clover.studio.sdk.model.LocalStream
import clover.studio.sdk.model.RemotePeer
import clover.studio.sdk.model.ServerInfo
import clover.studio.sdk.model.UserInformation
import clover.studio.sdk.service.CallServiceImpl
import clover.studio.sdk.viewmodel.PeerProps
import clover.studio.spikabroadcastsdk.R
import clover.studio.spikabroadcastsdk.databinding.ActivityCallBinding
import org.webrtc.RendererCommon


const val EXTRA_ROOD_ID = "roomId"
private const val HOSTNAME = "mediasouptest.clover.studio"
private const val PORT = "4443"

class CallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallBinding
    private var mPeerAdapter: PeerAdapter? = null

    private lateinit var viewModel: CallViewModel
    private var spikaBroadcast: SpikaBroadcast? = null
    private var roomId: String? = null

    private val spikaBroadcastListener = object : SpikaBroadcastListener {
        override fun onLocalStreamChanged(localStream: LocalStream) {
            localStream.videoTrack?.let {
                it.removeSink(binding.pipVideoView)
                binding.pipVideoView.visibility = View.VISIBLE
                it.addSink(binding.pipVideoView)
            }
        }

        override fun onRemoteStreamsChanged(consumers: List<RemotePeer>) {
            if (mPeerAdapter == null) {
                mPeerAdapter = PeerAdapter(consumers)
                binding.rvPeers.layoutManager = GridLayoutManager(this@CallActivity, 1)
                binding.rvPeers.adapter = mPeerAdapter
            } else {
                mPeerAdapter!!.replacePeers(consumers)
            }
        }

        override fun microphoneStateChanged(enabled: Boolean) {
            if (enabled) {
                binding.btnAudio.setImageResource(clover.studio.sdk.R.drawable.microphone_disabled)
            } else {
                binding.btnAudio.setImageResource(clover.studio.sdk.R.drawable.microphone_enabled)
            }
        }

        override fun cameraStateChanged(enabled: Boolean) {
            if (enabled) {
                binding.btnVideo.setImageResource(clover.studio.sdk.R.drawable.camera_disabled)
                binding.overlayMutedVideo.visibility = View.GONE
            } else {
                binding.btnVideo.setImageResource(clover.studio.sdk.R.drawable.camera_enabled)
                binding.overlayMutedVideo.visibility = View.VISIBLE
            }
        }

        override fun speakerStateChanged(enabled: Boolean) {
            if (enabled) {
                binding.btnSpeaker.setImageResource(clover.studio.sdk.R.drawable.speaker_disabled)
            } else {
                binding.btnSpeaker.setImageResource(clover.studio.sdk.R.drawable.speaker_enabled)
            }
        }

        override fun callClosed() {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this).get(CallViewModel::class.java)

        roomId = intent.extras?.getString(EXTRA_ROOD_ID)
//        roomId?.let {
//            spikaBroadcast = SpikaBroadcastUi(
//                this,
//                this,
//                spikaBroadcastListener,
//                UserInformation(
//                    "Korisnik",
//                    it,
//                    "https://i.pravatar.cc/300"
//                ),
//                ServerInfo(HOSTNAME, PORT),
//                binding.rootLayout,
//                CallServiceImpl.NotificationConfig(
//                    iconResId = R.drawable.end_call,
//                )
//            )
//        }
        roomId?.let {
            spikaBroadcast = SpikaBroadcast(
                this,
                this,
                spikaBroadcastListener,
                UserInformation(
                    "Korisnik",
                    it,
                    "https://i.pravatar.cc/300"
                ),
                ServerInfo(HOSTNAME, PORT),
                CallServiceImpl.NotificationConfig(
                    iconResId = R.drawable.end_call,
                )
            )
        }
        initViews()
    }

    private fun initViews() {
        binding.pipVideoView.init(PeerConnectionUtils.eglContext, null)
        binding.pipVideoView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        binding.pipVideoView.setZOrderMediaOverlay(true)
        binding.btnSwitchCamera.setOnClickListener { spikaBroadcast?.switchCamera() }
        binding.btnAudio.setOnClickListener { spikaBroadcast?.toggleMicrophoneState() }
        binding.btnVideo.setOnClickListener { spikaBroadcast?.toggleCameraState() }
        binding.btnSpeaker.setOnClickListener { spikaBroadcast?.toggleSpeakerState() }
        binding.btnParticipants.setOnClickListener {
            ParticipantsFragment(
                spikaBroadcast?.getParticipantsLiveData(),
                spikaBroadcast?.getInvitationLink()
            ).show(this.supportFragmentManager, null)
        }
        binding.btnEndCall.setOnClickListener { spikaBroadcast?.endCall() }
    }

    override fun onPause() {
        spikaBroadcast?.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        spikaBroadcast?.resume()
    }

    override fun finish() {
        spikaBroadcast?.disconnect()
        super.finish()
    }
}