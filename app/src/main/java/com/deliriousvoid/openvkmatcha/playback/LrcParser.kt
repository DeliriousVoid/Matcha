package com.deliriousvoid.openvkmatcha.playback

import com.deliriousvoid.openvkmatcha.data.model.LrcLine
import java.util.regex.Pattern

object LrcParser {
    private val linePattern = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})](.*)")

    fun parse(lrcContent: String?): List<LrcLine> {
        if (lrcContent.isNullOrBlank()) return emptyList()

        val lines = mutableListOf<LrcLine>()
        lrcContent.lines().forEach { line ->
            val matcher = linePattern.matcher(line.trim())
            if (matcher.matches()) {
                val minutes = matcher.group(1)?.toLong() ?: 0L
                val seconds = matcher.group(2)?.toLong() ?: 0L
                val millisString = matcher.group(3) ?: "00"
                
                // Handle both .xx and .xxx
                val millis = if (millisString.length == 2) {
                    millisString.toLong() * 10
                } else {
                    millisString.toLong()
                }

                val timestampMs = (minutes * 60 + seconds) * 1000 + millis
                val text = matcher.group(4)?.trim() ?: ""
                
                if (text.isNotBlank() || lines.isNotEmpty()) {
                    lines.add(LrcLine(timestampMs, text))
                }
            }
        }
        return lines.sortedBy { it.timestampMs }
    }
}
