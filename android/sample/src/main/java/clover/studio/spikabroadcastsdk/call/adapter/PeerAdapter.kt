package clover.studio.spikabroadcastsdk.call.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import clover.studio.clovermediasouppoc.utils.PeerConnectionUtils
import clover.studio.sdk.R
import clover.studio.sdk.databinding.ItemPeerBinding
import clover.studio.sdk.model.RemotePeer
import clover.studio.spikabroadcastsdk.call.adapter.PeerAdapter.PeerViewHolder
import org.webrtc.RendererCommon

class PeerAdapter(
    consumers: List<RemotePeer>
) : RecyclerView.Adapter<PeerViewHolder>() {
    private var remotePeers: List<RemotePeer> = consumers
    private var containerHeight = 0
    private var recyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }

    fun replacePeers(remotePeers: List<RemotePeer>) {
        val diffResult = DiffUtil.calculateDiff(PeerDiffCallback(this.remotePeers, remotePeers))
        this.remotePeers = remotePeers
        diffResult.dispatchUpdatesTo(this)
        this.recyclerView?.let {
            (it.layoutManager as GridLayoutManager).spanCount = columnCount
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeerViewHolder {
        containerHeight = parent.height
        val context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.item_peer, parent, false)
        return PeerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PeerViewHolder, position: Int) {
        // update height
        val layoutParams = holder.itemView.layoutParams
        layoutParams.height = itemHeight
        holder.itemView.layoutParams = layoutParams
        // bind
        holder.bind(remotePeers[position])
    }

    override fun getItemCount(): Int {
        return remotePeers.size
    }

    private val columnCount: Int
        get() {
            return when {
                itemCount <= 2 -> 1
                else -> 2
            }
        }

    private val itemHeight: Int
        get() {
            val itemCount = itemCount
            return when {
                itemCount <= 1 -> {
                    containerHeight
                }
                itemCount <= 4 -> {
                    containerHeight / 2
                }
                else -> {
                    containerHeight / 3
                }
            }
        }

    class PeerViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view) {

        private var binding: ItemPeerBinding = ItemPeerBinding.bind(view)
        private var videoTrackId: String? = null
        private var audioTrackId: String? = null

        init {
            binding.videoRenderer.init(PeerConnectionUtils.eglContext, null)
            binding.videoRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        }

        fun bind(remotePeer: RemotePeer) {
//            Glide.with(view).load(peer.avatarUrl).into(binding.placeholder)

            remotePeer.videoTrack?.let {
                if (remotePeer.videoProducerId != videoTrackId &&
                        videoTrackId != null) {
                    videoTrackId = remotePeer.videoProducerId
                    remotePeer.videoTrack!!.addSink(binding.videoRenderer)
                }
                binding.videoRenderer.visibility = View.VISIBLE
            } ?: run {
                binding.videoRenderer.visibility = View.GONE
            }

            binding.camOff.visibility = if (!remotePeer.videoVisible) View.VISIBLE else View.GONE
            binding.videoRenderer.visibility =
                if (remotePeer.videoVisible) View.VISIBLE else View.GONE

            binding.micOff.visibility = if (!remotePeer.audioEnabled) View.VISIBLE else View.GONE

        }
    }

    companion object {
        private const val TAG = "PeerAdapter"
    }
}