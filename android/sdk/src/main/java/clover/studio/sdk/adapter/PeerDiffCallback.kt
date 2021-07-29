package clover.studio.sdk.adapter

import android.util.Log
import androidx.recyclerview.widget.DiffUtil
import clover.studio.sdk.model.Peer

class PeerDiffCallback(
    private var oldData: List<Peer>,
    private var newData: List<Peer>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldData.size

    override fun getNewListSize() = newData.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        oldData[oldItemPosition].id == newData[newItemPosition].id

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {

        val consumersSame = oldData[oldItemPosition].consumers == newData[newItemPosition].consumers
        val deviceInfoSame = oldData[oldItemPosition].device == newData[newItemPosition].device
        val displayNameSame =
            oldData[oldItemPosition].displayName == newData[newItemPosition].displayName
        val returnValue = consumersSame && deviceInfoSame && displayNameSame

        if (!returnValue) {
            val stringBuilder = StringBuilder("Difference at position $newItemPosition; Changes ->")
            if (!consumersSame) {
                stringBuilder.append(" consumers")
            }
            if (!deviceInfoSame) {
                stringBuilder.append(" deviceInfo")
            }
            if (!displayNameSame) {
                stringBuilder.append(" displayName")
            }
            Log.d(TAG, stringBuilder.toString())
        }
        return returnValue
    }

    companion object {
        const val TAG = "PeerDiffCallback"
    }
}
