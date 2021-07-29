package clover.studio.clovermediasouppoc.utils

import java.util.*

private const val ALLOWED_CHARACTERS = "0123456789qwertyuiopasdfghjklzxcvbnm"

object Utils {
    fun getRandomString(sizeOfRandomString: Int): String {
        val random = Random()
        val sb = StringBuilder(sizeOfRandomString)
        for (i in 0 until sizeOfRandomString) sb.append(
            ALLOWED_CHARACTERS[random.nextInt(
                ALLOWED_CHARACTERS.length
            )]
        )
        return sb.toString()
    }
}