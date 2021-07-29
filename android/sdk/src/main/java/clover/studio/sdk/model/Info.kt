package clover.studio.sdk.model

open class Info {
    var id: String? = null
    var displayName: String? = null
    var device: DeviceInfo? = DeviceInfo.androidDevice()
}