package com.deliriousvoid.openvkmatcha.ui.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun formatTimeAgo(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val date = Date(timestamp * 1000)
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { time = date }

    val timeFormat = SimpleDateFormat("HH:mm", Locale("ru"))
    val dayFormat = SimpleDateFormat("d MMM", Locale("ru"))
    val yearFormat = SimpleDateFormat("d MMM yyyy", Locale("ru"))

    return when {
        // Today
        now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR) -> {
            "сегодня в ${timeFormat.format(date)}"
        }

        // Yesterday
        isYesterday(now, target) -> {
            "вчера в ${timeFormat.format(date)}"
        }

        // This year
        now.get(Calendar.YEAR) == target.get(Calendar.YEAR) -> {
            "${dayFormat.format(date)} в ${timeFormat.format(date)}"
        }

        // Different year
        else -> {
            "${yearFormat.format(date)} в ${timeFormat.format(date)}"
        }
    }
}

private fun isYesterday(now: Calendar, target: Calendar): Boolean {
    val yesterday = (now.clone() as Calendar).apply {
        add(Calendar.DAY_OF_YEAR, -1)
    }
    return yesterday.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
           yesterday.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
}

fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(minutes, secs)
}

fun formatFileSize(bytes: Int): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    return "%.1f %sB".format(bytes / Math.pow(1024.0, exp.toDouble()), pre)
}

fun formatLastSeen(lastSeen: com.deliriousvoid.openvkmatcha.data.model.LastSeen, sex: Int): String {
    val timeStr = formatTimeAgo(lastSeen.time)
    val action = if (sex == 1) "заходила" else "заходил"
    val platform = when (lastSeen.platform) {
        1 -> "через моб. версию"
        2 -> "через iPhone"
        3 -> "через iPad"
        4 -> "через Android"
        5 -> "через Windows Phone"
        6 -> "через Windows"
        7 -> "через сайт"
        else -> ""
    }
    return "$action $timeStr $platform".trim()
}
