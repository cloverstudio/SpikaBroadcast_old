package clover.studio.spikabroadcastsdk.call.adapter

import android.util.Log
import androidx.recyclerview.widget.DiffUtil
import clover.studio.sdk.model.RemotePeer

class PeerDiffCallback(
    private var oldData: List<RemotePeer>,
    private var newData: List<RemotePeer>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldData.size

    override fun getNewListSize() = newData.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        oldData[oldItemPosition].peer?.id === newData[newItemPosition].peer?.id

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val audioSame = oldData[oldItemPosition].audioEnabled == newData[newItemPosition].audioEnabled &&
                oldData[oldItemPosition].audioTrack?.id() == newData[newItemPosition].audioTrack?.id()
        val videoSame = oldData[oldItemPosition].videoVisible == newData[newItemPosition].videoVisible &&
                oldData[oldItemPosition].videoTrack?.id() == newData[newItemPosition].videoTrack?.id()
        val returnValue = videoSame && audioSame
        if (!returnValue) {
            val stringBuilder = StringBuilder("Difference at position $newItemPosition; Changes ->")
            if (!audioSame) {
                stringBuilder.append(" audio")
            }
            if (!videoSame) {
                stringBuilder.append(" video")
            }
            Log.d(TAG, stringBuilder.toString())
        }
        return returnValue
    }

    companion object {
        const val TAG = "PeerDiffCallback"
    }
}
