package clover.studio.sdk.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import clover.studio.clovermediasouppoc.utils.PeerConnectionUtils
import clover.studio.sdk.R
import clover.studio.sdk.adapter.PeerAdapter.PeerViewHolder
import clover.studio.sdk.call.RoomStore
import clover.studio.sdk.databinding.ItemPeerBinding
import clover.studio.sdk.model.Peer
import clover.studio.sdk.viewmodel.PeerProps
import com.bumptech.glide.Glide
import org.mediasoup.droid.Logger
import org.webrtc.RendererCommon
import java.util.*

class PeerAdapter(
    private val mStore: RoomStore,
    private val mLifecycleOwner: LifecycleOwner
) : RecyclerView.Adapter<PeerViewHolder>() {
    private var mPeers: List<Peer> = LinkedList()
    private var containerHeight = 0
    private var recyclerView: RecyclerView? = null

    init {
        observePeers()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }

    private fun observePeers() {
        mStore.getPeers().observe(mLifecycleOwner, { peers ->
            replacePeers(peers.allPeers)
            this.recyclerView?.let {
                (it.layoutManager as GridLayoutManager).spanCount = columnCount
            }
        })
    }

    private fun replacePeers(peers: List<Peer>) {
        val diffResult = DiffUtil.calculateDiff(PeerDiffCallback(mPeers, peers))
        mPeers = peers
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeerViewHolder {
        containerHeight = parent.height
        val context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.item_peer, parent, false)
        return PeerViewHolder(
            view,
            PeerProps(mStore)
        )
    }

    override fun onBindViewHolder(holder: PeerViewHolder, position: Int) {
        // update height
        val layoutParams = holder.itemView.layoutParams
        layoutParams.height = itemHeight
        holder.itemView.layoutParams = layoutParams
        // bind
        holder.bind(mLifecycleOwner, mPeers[position])
    }

    override fun getItemCount(): Int {
        return mPeers.size
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
        private val view: View,
        private val peerProps: PeerProps
    ) : RecyclerView.ViewHolder(view) {

        private var binding: ItemPeerBinding = ItemPeerBinding.bind(view)

        init {
            binding.videoRenderer.init(PeerConnectionUtils.eglContext, null)
            binding.videoRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        }

        fun bind(owner: LifecycleOwner, peer: Peer) {
            Logger.d(TAG, "bind() id: " + peer.id + ", name: " + peer.displayName)

            Glide.with(view).load(peer.avatarUrl).into(binding.placeholder)

            peerProps.connect(owner, peer.id, {
                if (peerProps.videoTrack != null) {
                    (peerProps.videoTrack!!).addSink(binding.videoRenderer)
                    binding.videoRenderer.visibility = View.VISIBLE
                } else {
                    binding.videoRenderer.visibility = View.GONE
                }

                binding.camOff.visibility = if (!peerProps.videoVisible) View.VISIBLE else View.GONE
                binding.videoRenderer.visibility = if (peerProps.videoVisible) View.VISIBLE else View.GONE

                binding.micOff.visibility = if (!peerProps.audioEnabled) View.VISIBLE else View.GONE
            })
        }
    }

    companion object {
        private const val TAG = "PeerAdapter"
    }
}