package clover.studio.spikabroadcastsdk.call

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import clover.studio.sdk.SpikaBroadcast
import clover.studio.sdk.SpikaBroadcastListener
import clover.studio.spikabroadcastsdk.databinding.ActivityCallBinding


const val EXTRA_ROOD_ID = "roomId"

class CallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallBinding
    private lateinit var viewModel: CallViewModel
    private var spikaBroadcast: SpikaBroadcast? = null
    private var roomId: String? = null

    private val spikaBroadcastListener = object : SpikaBroadcastListener {
        override fun microphoneStateChanged(enabled: Boolean) {
            TODO("Not yet implemented")
        }

        override fun cameraStateChanged(enabled: Boolean) {
            TODO("Not yet implemented")
        }

        override fun speakerStateChanged(enabled: Boolean) {
            TODO("Not yet implemented")
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
        roomId?.let {
            spikaBroadcast = SpikaBroadcast(
                this, this, it, spikaBroadcastListener, binding.rootLayout
            )
        }
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