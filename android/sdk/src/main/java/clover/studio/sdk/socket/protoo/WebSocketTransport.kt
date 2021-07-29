package clover.studio.sdk.socket.protoo

import android.os.Handler
import android.os.HandlerThread
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import okio.ByteString
import org.apache.http.conn.ssl.SSLSocketFactory
import org.json.JSONObject
import org.mediasoup.droid.Logger
import org.protoojs.droid.Message
import org.protoojs.droid.transports.AbsWebSocketTransport
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.CountDownLatch
import javax.net.ssl.*
import kotlin.math.pow

class WebSocketTransport(url: String?) : AbsWebSocketTransport(url) {
    // Closed flag.
    private var mClosed = false

    // Connected flag.
    private var mConnected = false

    // OKHttpClient.
    private val mOkHttpClient: OkHttpClient

    // Handler associate to current thread.
    private val mHandler: Handler

    // Retry operation.
    private val mRetryStrategy: RetryStrategy

    // WebSocket instance.
    private var mWebSocket: WebSocket? = null

    // Listener.
    private var mListener: Listener? = null

    private class RetryStrategy(
        private val retries: Int,
        private val factor: Int,
        private val minTimeout: Int,
        private val maxTimeout: Int
    ) {
        var retryCount = 1
        fun retried() {
            retryCount++
        }

        val reconnectInterval: Int
            get() {
                if (retryCount > retries) {
                    return -1
                }
                var reconnectInterval =
                    (minTimeout * factor.toDouble().pow(retryCount.toDouble())).toInt()
                reconnectInterval = reconnectInterval.coerceAtMost(maxTimeout)
                return reconnectInterval
            }

        fun reset() {
            if (retryCount != 0) {
                retryCount = 0
            }
        }
    }

    override fun connect(listener: Listener) {
        Logger.d(TAG, "connect()")
        mListener = listener
        mHandler.post { newWebSocket() }
    }

    private fun newWebSocket() {
        mWebSocket = null
        mOkHttpClient.newWebSocket(
            Request.Builder().url(mUrl).addHeader("Sec-WebSocket-Protocol", "protoo").build(),
            ProtooWebSocketListener()
        )
    }

    private fun scheduleReconnect(): Boolean {
        val reconnectInterval = mRetryStrategy.reconnectInterval
        if (reconnectInterval == -1) {
            return false
        }
        Logger.d(TAG, "scheduleReconnect() ")
        mHandler.postDelayed(
            {
                if (mClosed) {
                    return@postDelayed
                }
                Logger.w(TAG, "doing reconnect job, retryCount: " + mRetryStrategy.retryCount)
                mOkHttpClient.dispatcher.cancelAll()
                newWebSocket()
                mRetryStrategy.retried()
            },
            reconnectInterval.toLong()
        )
        return true
    }

    override fun sendMessage(message: JSONObject): String {
        check(!mClosed) { "transport closed" }
        val payload = message.toString()
        mHandler.post {
            if (mClosed) {
                return@post
            }
            if (mWebSocket != null) {
                mWebSocket!!.send(payload)
            }
        }
        return payload
    }

    override fun close() {
        if (mClosed) {
            return
        }
        mClosed = true
        Logger.d(TAG, "close()")
        val countDownLatch = CountDownLatch(1)
        mHandler.post {
            if (mWebSocket != null) {
                mWebSocket!!.close(1000, "bye")
                mWebSocket = null
            }
            countDownLatch.countDown()
        }
        try {
            countDownLatch.await()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun isClosed(): Boolean {
        return mClosed
    }

    private inner class ProtooWebSocketListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            if (mClosed) {
                return
            }
            Logger.d(TAG, "onOpen() ")
            mWebSocket = webSocket
            mConnected = true
            if (mListener != null) {
                mListener!!.onOpen()
            }
            mRetryStrategy.reset()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Logger.w(TAG, "onClosed()")
            if (mClosed) {
                return
            }
            mClosed = true
            mConnected = false
            mRetryStrategy.reset()
            if (mListener != null) {
                mListener!!.onClose()
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Logger.w(TAG, "onClosing()")
        }

        override fun onFailure(
            webSocket: WebSocket, t: Throwable, response: Response?
        ) {
            Logger.w(TAG, "onFailure()")
            if (mClosed) {
                return
            }
            if (scheduleReconnect()) {
                if (mListener != null) {
                    if (mConnected) {
                        mListener!!.onFail()
                    } else {
                        mListener!!.onDisconnected()
                    }
                }
            } else {
                Logger.e(TAG, "give up reconnect. notify closed")
                mClosed = true
                if (mListener != null) {
                    mListener!!.onClose()
                }
                mRetryStrategy.reset()
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Logger.d(TAG, "onMessage()")
            if (mClosed) {
                return
            }
            val message = Message.parse(text) ?: return
            if (mListener != null) {
                mListener!!.onMessage(message)
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            Logger.d(TAG, "onMessage()")
        }
    }

    // Called reflectively by X509TrustManagerExtensions.
    private val unsafeOkHttpClient: OkHttpClient
        private get() = try {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    @Throws(CertificateException::class)
                    override fun checkClientTrusted(
                        chain: Array<X509Certificate>, authType: String
                    ) {
                    }

                    @Throws(CertificateException::class)
                    override fun checkServerTrusted(
                        chain: Array<X509Certificate>, authType: String
                    ) {
                    }

                    // Called reflectively by X509TrustManagerExtensions.
                    fun checkServerTrusted(
                        chain: Array<X509Certificate?>?, authType: String?, host: String?
                    ) {
                    }

                    override fun getAcceptedIssuers(): Array<X509Certificate> {
                        return arrayOf()
                    }
                }
            )
            val sslContext = SSLContext.getInstance(SSLSocketFactory.SSL)
            sslContext.init(null, trustAllCerts, SecureRandom())
            val sslSocketFactory = sslContext.socketFactory
            val httpLoggingInterceptor =
                HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
                    override fun log(message: String) {
                        Logger.d(TAG, message)
                    }
                })
            httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC)
            val builder: OkHttpClient.Builder = OkHttpClient.Builder()
                .addInterceptor(httpLoggingInterceptor)
                .retryOnConnectionFailure(true)
            builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            builder.hostnameVerifier { _: String?, _: SSLSession? -> true }
            builder.build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    companion object {
        // Log tag.
        private const val TAG = "WebSocketTransport"
    }

    init {
        mOkHttpClient = unsafeOkHttpClient
        val handlerThread = HandlerThread("socket")
        handlerThread.start()
        mHandler = Handler(handlerThread.looper)
        mRetryStrategy = RetryStrategy(10, 2, 1000, 8 * 1000)
    }
}