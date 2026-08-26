package com.deliriousvoid.openvkmatcha.util

import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.R

object StringUtils {
    fun getPollVotesString(count: Int): String {
        val lm = OpenVKMatchaApp.instance.languageManager
        val lastDigit = count % 10
        val lastTwoDigits = count % 100
        val resId = when {
            lastTwoDigits in 11..14 -> R.string.votes_50
            lastDigit == 1 -> R.string.votes_1
            lastDigit in 2..4 -> R.string.votes_24
            else -> R.string.votes_50
        }
        return lm.getStringOrDefault(resId, count)
    }

    fun getGolosString(count: Int): String {
        val lm = OpenVKMatchaApp.instance.languageManager
        val lastDigit = count % 10
        val lastTwoDigits = count % 100
        val resId = when {
            lastTwoDigits in 11..19 -> R.string.votes_50
            lastDigit == 1 -> R.string.votes_1
            lastDigit in 2..4 -> R.string.votes_24
            else -> R.string.votes_50
        }
        val full = lm.getStringOrDefault(resId, count)
        // Extract only the word if possible, or just return full since it's used in "$price ${getGolosString(price)}"
        // Actually, let's make it return just the suffix part if needed, but the current usage is a bit mixed.
        // I'll adjust the usage in SendGiftScreen to use the full string or return just the suffix.
        return full.replace(count.toString(), "").trim()
    }

    fun stripMentions(text: String?): String? {
        if (text == null) return null
        return text.replace(Regex("\\[(?:id|club|user)\\d+\\|([^]]+)\\]")) { it.groupValues[1] }
    }
}
