package clover.studio.sdk.call

import clover.studio.sdk.model.DeviceInfo

class RoomOptions {
    // Device info.
    private var mDevice: DeviceInfo = DeviceInfo.androidDevice()

    // Whether we want to force RTC over TCP.
    var isForceTcp = false
        private set

    // Whether we want to produce audio/video.
    var isProduce = true
        private set

    // Whether we should consume.
    var isConsume = true
        private set

    // Whether we want DataChannels.
    var isUseDataChannel = false
        private set

    fun setDevice(device: DeviceInfo): RoomOptions {
        mDevice = device
        return this
    }

    fun setForceTcp(forceTcp: Boolean): RoomOptions {
        isForceTcp = forceTcp
        return this
    }

    fun setProduce(produce: Boolean): RoomOptions {
        isProduce = produce
        return this
    }

    fun setConsume(consume: Boolean): RoomOptions {
        isConsume = consume
        return this
    }

    fun setUseDataChannel(useDataChannel: Boolean): RoomOptions {
        isUseDataChannel = useDataChannel
        return this
    }

    val device: DeviceInfo
        get() = mDevice
}