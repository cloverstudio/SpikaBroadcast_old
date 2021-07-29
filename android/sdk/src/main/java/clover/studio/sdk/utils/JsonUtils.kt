package clover.studio.clovermediasouppoc.utils

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object JsonUtils {
    fun jsonPut(json: JSONObject, key: String?, value: Any?) {
        try {
            json.put(key, value)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun toJsonObject(data: String?): JSONObject {
        return try {
            JSONObject(data)
        } catch (e: JSONException) {
            e.printStackTrace()
            JSONObject()
        }
    }

    fun toJsonArray(data: String?): JSONArray {
        return try {
            JSONArray(data)
        } catch (e: JSONException) {
            e.printStackTrace()
            JSONArray()
        }
    }
}