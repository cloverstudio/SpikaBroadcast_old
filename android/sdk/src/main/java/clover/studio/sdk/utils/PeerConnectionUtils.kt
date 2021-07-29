package clover.studio.clovermediasouppoc.utils

import android.content.Context
import android.text.TextUtils
import android.util.Log
import androidx.annotation.MainThread
import org.mediasoup.droid.Logger
import org.webrtc.*
import org.webrtc.CameraVideoCapturer.CameraEventsHandler
import org.webrtc.CameraVideoCapturer.CameraSwitchHandler
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule.AudioRecordErrorCallback
import org.webrtc.audio.JavaAudioDeviceModule.AudioTrackErrorCallback

class PeerConnectionUtils {
    private val mThreadChecker: ThreadUtils.ThreadChecker = ThreadUtils.ThreadChecker()
    private var mPeerConnectionFactory: PeerConnectionFactory? = null
    private var mAudioSource: AudioSource? = null
    private var mVideoSource: VideoSource? = null
    private var mCamCapture: CameraVideoCapturer? = null

    // PeerConnection factory creation.
    private fun createPeerConnectionFactory(context: Context) {
        Logger.d(TAG, "createPeerConnectionFactory()")
        mThreadChecker.checkIsOnValidThread()
        val builder = PeerConnectionFactory.builder()
        builder.setOptions(null)
        val adm = createJavaAudioDevice(context)
        val encoderFactory: VideoEncoderFactory = DefaultVideoEncoderFactory(
            mEglBase.eglBaseContext, true /* enableIntelVp8Encoder */, true
        )
        val decoderFactory: VideoDecoderFactory =
            DefaultVideoDecoderFactory(mEglBase.eglBaseContext)
        mPeerConnectionFactory = builder
            .setAudioDeviceModule(adm)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }

    private fun createJavaAudioDevice(appContext: Context): AudioDeviceModule {
        Logger.d(TAG, "createJavaAudioDevice()")
        mThreadChecker.checkIsOnValidThread()
        // Enable/disable OpenSL ES playback.
        // Set audio record error callbacks.
        val audioRecordErrorCallback: AudioRecordErrorCallback = object : AudioRecordErrorCallback {
            override fun onWebRtcAudioRecordInitError(errorMessage: String) {
                Logger.e(TAG, "onWebRtcAudioRecordInitError: $errorMessage")
            }

            override fun onWebRtcAudioRecordStartError(
                errorCode: JavaAudioDeviceModule.AudioRecordStartErrorCode, errorMessage: String
            ) {
                Logger.e(TAG, "onWebRtcAudioRecordStartError: $errorCode. $errorMessage")
            }

            override fun onWebRtcAudioRecordError(errorMessage: String) {
                Logger.e(TAG, "onWebRtcAudioRecordError: $errorMessage")
            }
        }
        val audioTrackErrorCallback: AudioTrackErrorCallback = object : AudioTrackErrorCallback {
            override fun onWebRtcAudioTrackInitError(errorMessage: String) {
                Log.e(TAG, "onWebRtcAudioTrackInitError: $errorMessage")
            }

            override fun onWebRtcAudioTrackStartError(
                errorCode: JavaAudioDeviceModule.AudioTrackStartErrorCode, errorMessage: String
            ) {
                Log.e(TAG, "onWebRtcAudioTrackStartError: $errorCode. $errorMessage")
            }

            override fun onWebRtcAudioTrackError(errorMessage: String) {
                Log.e(TAG, "onWebRtcAudioTrackError: $errorMessage")
            }
        }
        return JavaAudioDeviceModule.builder(appContext)
            .setAudioRecordErrorCallback(audioRecordErrorCallback)
            .setAudioTrackErrorCallback(audioTrackErrorCallback)
            .createAudioDeviceModule()
    }

    // Audio source creation.
    private fun createAudioSource(context: Context) {
        Logger.d(TAG, "createAudioSource()")
        mThreadChecker.checkIsOnValidThread()
        if (mPeerConnectionFactory == null) {
            createPeerConnectionFactory(context)
        }
        mAudioSource = mPeerConnectionFactory!!.createAudioSource(MediaConstraints())
    }

    private fun createCamCapture(context: Context) {
        Logger.d(TAG, "createCamCapture()")
        mThreadChecker.checkIsOnValidThread()
        val isCamera2Supported = Camera2Enumerator.isSupported(context)
        val cameraEnumerator: CameraEnumerator = if (isCamera2Supported) {
            Camera2Enumerator(context)
        } else {
            Camera1Enumerator()
        }
        val deviceNames = cameraEnumerator.deviceNames
        for (deviceName in deviceNames) {
            val needFrontFacing = "front".endsWith(mPreferCameraFace!!)
            var selectedDeviceName: String? = null
            if (needFrontFacing) {
                if (cameraEnumerator.isFrontFacing(deviceName)) {
                    selectedDeviceName = deviceName
                }
            } else {
                if (!cameraEnumerator.isFrontFacing(deviceName)) {
                    selectedDeviceName = deviceName
                }
            }
            if (!TextUtils.isEmpty(selectedDeviceName)) {
                mCamCapture = cameraEnumerator.createCapturer(
                    selectedDeviceName,
                    object : CameraEventsHandler {
                        override fun onCameraError(s: String) {
                            Logger.e(TAG, "onCameraError, $s")
                        }

                        override fun onCameraDisconnected() {
                            Logger.w(TAG, "onCameraDisconnected")
                        }

                        override fun onCameraFreezed(s: String) {
                            Logger.w(TAG, "onCameraFreezed, $s")
                        }

                        override fun onCameraOpening(s: String) {
                            Logger.d(TAG, "onCameraOpening, $s")
                        }

                        override fun onFirstFrameAvailable() {
                            Logger.d(TAG, "onFirstFrameAvailable")
                        }

                        override fun onCameraClosed() {
                            Logger.d(TAG, "onCameraClosed")
                        }
                    })
                break
            }
        }
        checkNotNull(mCamCapture) { "Failed to create Camera Capture" }
    }

    fun switchCam(switchHandler: CameraSwitchHandler?) {
        Logger.d(TAG, "switchCam()")
        mThreadChecker.checkIsOnValidThread()
        if (mCamCapture != null) {
            mCamCapture!!.switchCamera(switchHandler)
        }
    }

    // Video source creation.
    @MainThread
    private fun createVideoSource(context: Context) {
        Logger.d(TAG, "createVideoSource()")
        mThreadChecker.checkIsOnValidThread()
        if (mPeerConnectionFactory == null) {
            createPeerConnectionFactory(context)
        }
        if (mCamCapture == null) {
            createCamCapture(context)
        }
        mVideoSource = mPeerConnectionFactory!!.createVideoSource(false)
        val surfaceTextureHelper =
            SurfaceTextureHelper.create("CaptureThread", mEglBase.eglBaseContext)
        mCamCapture!!.initialize(surfaceTextureHelper, context, mVideoSource?.capturerObserver)
        mCamCapture!!.startCapture(640, 480, 30)
    }

    // Audio track creation.
    fun createAudioTrack(context: Context, id: String?): AudioTrack {
        Logger.d(TAG, "createAudioTrack()")
        mThreadChecker.checkIsOnValidThread()
        if (mAudioSource == null) {
            createAudioSource(context)
        }
        return mPeerConnectionFactory!!.createAudioTrack(id, mAudioSource)
    }

    // Video track creation.
    fun createVideoTrack(context: Context, id: String?): VideoTrack {
        Logger.d(TAG, "createVideoTrack()")
        mThreadChecker.checkIsOnValidThread()
        if (mVideoSource == null) {
            createVideoSource(context)
        }
        return mPeerConnectionFactory!!.createVideoTrack(id, mVideoSource)
    }

    fun dispose() {
        Logger.w(TAG, "dispose()")
        mThreadChecker.checkIsOnValidThread()
        if (mCamCapture != null) {
            mCamCapture!!.dispose()
            mCamCapture = null
        }
        if (mVideoSource != null) {
            mVideoSource!!.dispose()
            mVideoSource = null
        }
        if (mAudioSource != null) {
            mAudioSource!!.dispose()
            mAudioSource = null
        }
        if (mPeerConnectionFactory != null) {
            mPeerConnectionFactory!!.dispose()
            mPeerConnectionFactory = null
        }
    }

    companion object {
        private const val TAG = "PeerConnectionUtils"
        private var mPreferCameraFace: String? = null
        private val mEglBase = EglBase.create()
        val eglContext: EglBase.Context
            get() = mEglBase.eglBaseContext

        fun setPreferCameraFace(preferCameraFace: String?) {
            mPreferCameraFace = preferCameraFace
        }
    }

}