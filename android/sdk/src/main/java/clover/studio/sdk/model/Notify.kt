package clover.studio.sdk.model

import clover.studio.clovermediasouppoc.utils.Utils.getRandomString
import java.util.*

class Notify @JvmOverloads constructor(
    private val type: String,
    private val text: String, timeout: Int = 0
) {
    val id: String = getRandomString(6).toLowerCase(Locale.ROOT)
    private var timeout: Int

    init {
        this.timeout = timeout
        if (this.timeout == 0) {
            if ("info" == this.type) {
                this.timeout = 3000
            } else if ("error" == this.type) {
                this.timeout = 5000
            }
        }
    }
}