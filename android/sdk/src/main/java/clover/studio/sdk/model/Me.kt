package clover.studio.sdk.model

class Me : Info() {
    var isDisplayNameSet = false
    var isCanSendMic = false
    var isCanSendCam = false
    var isCanChangeCam = false
    var isCamInProgress = false
    var isShareInProgress = false
    var isAudioOnly = false
    var isAudioOnlyInProgress = false
    var isAudioMuted = false
    var isRestartIceInProgress = false
    fun clear() {
        isCamInProgress = false
        isShareInProgress = false
        isAudioOnly = false
        isAudioOnlyInProgress = false
        isAudioMuted = false
        isRestartIceInProgress = false
    }
}