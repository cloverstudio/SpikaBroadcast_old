package clover.studio.spikabroadcastsdk

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import clover.studio.spikabroadcastsdk.call.EXTRA_ROOD_ID
import clover.studio.spikabroadcastsdk.databinding.FragmentMainBinding
import java.util.*

class MainFragment : Fragment(R.layout.fragment_main) {

    private lateinit var binding: FragmentMainBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMainBinding.bind(view)

        val newRoomId = UUID.randomUUID().toString()
        binding.btnNewMeeting.setOnClickListener {
            val bundle = Bundle().apply {
                putString(EXTRA_ROOD_ID, newRoomId)
            }
            findNavController().navigate(R.id.action_mainFragment_to_callActivity, bundle)
        }

        binding.btnJoinMeeting.setOnClickListener {
            findNavController().navigate(R.id.joinMeetingFragment)
        }
    }
}