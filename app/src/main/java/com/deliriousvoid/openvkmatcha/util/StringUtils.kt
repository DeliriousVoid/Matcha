package com.deliriousvoid.openvkmatcha.util

object StringUtils {
    fun getPollVotesString(count: Int): String {
        val lastDigit = count % 10
        val lastTwoDigits = count % 100
        val suffix = when {
            lastTwoDigits in 11..14 -> "голосов"
            lastDigit == 1 -> "голос"
            lastDigit in 2..4 -> "голоса"
            else -> "голосов"
        }
        return "$count $suffix"
    }

    fun getGolosString(count: Int): String {
        val lastDigit = count % 10
        val lastTwoDigits = count % 100
        return when {
            lastTwoDigits in 11..19 -> "голосов"
            lastDigit == 1 -> "голос"
            lastDigit in 2..4 -> "голоса"
            else -> "голосов"
        }
    }

    fun stripMentions(text: String?): String? {
        if (text == null) return null
        return text.replace(Regex("\\[(?:id|club|user)\\d+\\|([^]]+)\\]")) { it.groupValues[1] }
    }
}
