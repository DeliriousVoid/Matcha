package com.deliriousvoid.openvkmatcha.ui.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.R

fun formatTimeAgo(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val date = Date(timestamp * 1000)
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { time = date }

    val lm = OpenVKMatchaApp.instance.languageManager
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val day = target.get(Calendar.DAY_OF_MONTH)
    val month = target.get(Calendar.MONTH) + 1
    val year = target.get(Calendar.YEAR)
    
    val timeStr = timeFormat.format(date)
    val monthStr = lm.getStringOrDefault(getMonthRes(month))

    return when {
        // Today
        now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR) -> {
            lm.getStringOrDefault(R.string.date_today_at, timeStr)
        }

        // Yesterday
        isYesterday(now, target) -> {
            lm.getStringOrDefault(R.string.date_yesterday_at, timeStr)
        }

        // This year
        now.get(Calendar.YEAR) == target.get(Calendar.YEAR) -> {
            "$day $monthStr ${lm.getStringOrDefault(R.string.date_at)} $timeStr"
        }

        // Different year
        else -> {
            "$day $monthStr $year ${lm.getStringOrDefault(R.string.date_at)} $timeStr"
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

fun getMonthRes(month: Int): Int {
    return when (month) {
        1 -> R.string.month_1
        2 -> R.string.month_2
        3 -> R.string.month_3
        4 -> R.string.month_4
        5 -> R.string.month_5
        6 -> R.string.month_6
        7 -> R.string.month_7
        8 -> R.string.month_8
        9 -> R.string.month_9
        10 -> R.string.month_10
        11 -> R.string.month_11
        12 -> R.string.month_12
        else -> R.string.month_1 // Fallback
    }
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
    val lm = OpenVKMatchaApp.instance.languageManager
    
    val platform = when (lastSeen.platform) {
        1 -> lm.getStringOrDefault(R.string.platform_mobile_short)
        2 -> lm.getStringOrDefault(R.string.platform_iphone)
        3 -> lm.getStringOrDefault(R.string.platform_ipad)
        4 -> lm.getStringOrDefault(R.string.platform_android)
        5 -> lm.getStringOrDefault(R.string.platform_wphone)
        6 -> lm.getStringOrDefault(R.string.platform_windows)
        7 -> lm.getStringOrDefault(R.string.platform_site)
        else -> ""
    }
    
    return if (platform.isNotEmpty()) {
        if (sex == 1) lm.getStringOrDefault(R.string.last_seen_f_platform, timeStr, platform)
        else lm.getStringOrDefault(R.string.last_seen_m_platform, timeStr, platform)
    } else {
        if (sex == 1) lm.getStringOrDefault(R.string.last_seen_f, timeStr)
        else lm.getStringOrDefault(R.string.last_seen_m, timeStr)
    }
}
