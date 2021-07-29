package clover.studio.sdk.socket.protoo

import androidx.annotation.WorkerThread
import clover.studio.sdk.socket.SocketManager
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import org.json.JSONObject
import org.mediasoup.droid.Logger
import org.protoojs.droid.Peer
import org.protoojs.droid.ProtooException

class ProtooSocketManager(
    transport: WebSocketTransport,
    listener: Listener
) : Peer(transport, listener), SocketManager {

    override fun request(method: String): Observable<String> {
        return request(method, JSONObject())
    }

    override fun request(method: String, generator: (JSONObject) -> Unit): Observable<String> {
        val req = JSONObject()
        generator.invoke(req)
        return request(method, req)
    }

    private fun request(method: String, data: JSONObject): Observable<String> {
        Logger.d(TAG, "request(), method: $method")
        return Observable.create { emitter: ObservableEmitter<String> ->
            request(
                method,
                data,
                object : ClientRequestHandler {
                    override fun resolve(data: String) {
                        if (!emitter.isDisposed) {
                            emitter.onNext(data)
                        }
                    }

                    override fun reject(error: Long, errorReason: String) {
                        if (!emitter.isDisposed) {
                            emitter.onError(ProtooException(error, errorReason))
                        }
                    }
                })
        }
    }

    @WorkerThread
    @Throws(ProtooException::class)
    override fun syncRequest(method: String): String {
        return syncRequest(method, JSONObject())
    }

    @WorkerThread
    @Throws(ProtooException::class)
    override fun syncRequest(method: String, generator: (JSONObject) -> Unit): String {
        val req = JSONObject()
        generator.invoke(req)
        return syncRequest(method, req)
    }

    @WorkerThread
    @Throws(ProtooException::class)
    private fun syncRequest(method: String, data: JSONObject): String {
        Logger.d(TAG, "syncRequest(), method: $method")
        return try {
            request(method, data).blockingFirst()
        } catch (throwable: Throwable) {
            throw ProtooException(-1, throwable.message)
        }
    }

    companion object {
        private const val TAG = "Protoo"
    }
}