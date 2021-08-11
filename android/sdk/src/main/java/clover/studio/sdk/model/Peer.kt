package clover.studio.sdk.model

import org.json.JSONObject
import java.util.*

class Peer(info: JSONObject) : Info() {
    val consumers: HashSet<String>

    init {
        id = info.optString("id")
        displayName = info.optString("displayName")
        avatarUrl = info.optString("avatarUrl")
        val deviceInfo = info.optJSONObject("device")
        device = if (deviceInfo != null) {
            DeviceInfo()
                .setFlag(deviceInfo.optString("flag"))
                .setName(deviceInfo.optString("name"))
                .setVersion(deviceInfo.optString("version"))
        } else {
            DeviceInfo.unknownDevice()
        }
        consumers = HashSet()
    }
}