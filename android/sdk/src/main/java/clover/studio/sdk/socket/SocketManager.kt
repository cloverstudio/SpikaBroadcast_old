package clover.studio.sdk.socket

import androidx.annotation.WorkerThread
import io.reactivex.Observable
import org.json.JSONObject

interface SocketManager {
    fun close()
    fun request(method: String): Observable<String>
    fun request(method: String, generator: (JSONObject) -> Unit): Observable<String>

    @WorkerThread
    fun syncRequest(method: String): String

    @WorkerThread
    fun syncRequest(method: String, generator: (JSONObject) -> Unit): String
}