package clover.studio.spikabroadcastsdk

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import clover.studio.spikabroadcastsdk.call.EXTRA_ROOD_ID
import clover.studio.spikabroadcastsdk.databinding.FragmentJoinCallBinding

class JoinMeetingFragment: Fragment(R.layout.fragment_join_call) {

    private lateinit var binding: FragmentJoinCallBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentJoinCallBinding.bind(view)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnJoinMeeting.setOnClickListener {
            if (binding.etMeetingLink.text.isNotEmpty()){
                val bundle = Bundle().apply {
                    putString(EXTRA_ROOD_ID, binding.etMeetingLink.text.toString())
                }
                findNavController().navigate(R.id.action_joinMeetingFragment_to_callActivity, bundle)
            }
        }
    }
}
