package clover.studio.sdk.model

import android.os.Build
import clover.studio.clovermediasouppoc.utils.JsonUtils.jsonPut
import org.json.JSONObject

class DeviceInfo {
    var flag: String? = null
        private set
    var name: String? = null
        private set
    var version: String? = null
        private set

    fun setFlag(flag: String?): DeviceInfo {
        this.flag = flag
        return this
    }

    fun setName(name: String?): DeviceInfo {
        this.name = name
        return this
    }

    fun setVersion(version: String?): DeviceInfo {
        this.version = version
        return this
    }

    fun toJSONObject(): JSONObject {
        val deviceInfo = JSONObject()
        jsonPut(deviceInfo, "flag", flag)
        jsonPut(deviceInfo, "name", name)
        jsonPut(deviceInfo, "version", version)
        return deviceInfo
    }

    companion object {
        fun androidDevice(): DeviceInfo {
            return DeviceInfo()
                .setFlag("android")
                .setName("Android " + Build.DEVICE)
                .setVersion(Build.VERSION.CODENAME)
        }

        fun unknownDevice(): DeviceInfo {
            return DeviceInfo().setFlag("unknown").setName("unknown").setVersion("unknown")
        }
    }
}