package clover.studio.sdk.call

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import clover.studio.sdk.R
import clover.studio.sdk.databinding.DialogParticipantsBinding
import clover.studio.sdk.databinding.ItemParticipantBinding
import clover.studio.sdk.model.Participant

class ParticipantsFragment(
    private val participantsLiveData: LiveData<List<Participant>>?
) : DialogFragment(R.layout.dialog_participants) {

    private lateinit var binding: DialogParticipantsBinding
    private lateinit var participantsAdapter: ParticipantRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black);
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = DialogParticipantsBinding.bind(view)

        binding.btnBack.setOnClickListener { dismiss() }

        participantsAdapter = ParticipantRecyclerAdapter()
        binding.rvParticipants.adapter = participantsAdapter

        participantsLiveData?.observe(this, { participants ->
            participantsAdapter.replacePeers(participants)
        })
    }
}

class ParticipantRecyclerAdapter :
    RecyclerView.Adapter<ParticipantRecyclerAdapter.ParticipantViewHolder>() {

    private var participants: List<Participant> = listOf()

    fun replacePeers(participants: List<Participant>) {
        this.participants = participants
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_participant, parent, false)
        return ParticipantViewHolder(view)
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        holder.bind(participants[position])
    }

    override fun getItemCount(): Int {
        return participants.size
    }

    class ParticipantViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view) {
        private var binding: ItemParticipantBinding = ItemParticipantBinding.bind(view)

        fun bind(participant: Participant) {
            binding.name.text = participant.displayName
        }
    }
}
