package com.deliriousvoid.openvkmatcha.ui.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.R

fun formatTimeAgo(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val millis = if (timestamp > 10000000000L) timestamp else timestamp * 1000
    val date = Date(millis)
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

fun formatMessageTime(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val millis = if (timestamp > 10000000000L) timestamp else timestamp * 1000
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    return timeFormat.format(Date(millis))
}

fun formatCompactDate(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val millis = if (timestamp > 10000000000L) timestamp else timestamp * 1000
    val date = Date(millis)
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { time = date }

    val lm = runCatching { OpenVKMatchaApp.instance.languageManager }.getOrNull()
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeStr = timeFormat.format(date)

    return when {
        // Today
        now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR) -> {
            timeStr
        }

        // Yesterday
        isYesterday(now, target) -> {
            timeStr
        }

        // Other time
        else -> {
            val day = target.get(Calendar.DAY_OF_MONTH)
            val month = target.get(Calendar.MONTH) + 1
            val monthRes = getMonthRes(month)
            val monthStr = lm?.getStringOrDefault(monthRes)
                ?: runCatching { OpenVKMatchaApp.instance.getString(monthRes) }.getOrNull()
                ?: defaultMonthShortName(month)
            "$day $monthStr"
        }
    }
}

private fun defaultMonthShortName(month: Int): String {
    return when (month) {
        1 -> "янв"
        2 -> "фев"
        3 -> "мар"
        4 -> "апр"
        5 -> "мая"
        6 -> "июн"
        7 -> "июл"
        8 -> "авг"
        9 -> "сен"
        10 -> "окт"
        11 -> "ноя"
        12 -> "дек"
        else -> ""
    }
}

private fun isYesterday(now: Calendar, target: Calendar): Boolean {
    val yesterday = (now.clone() as Calendar).apply {
        add(Calendar.DAY_OF_YEAR, -1)
    }
    return yesterday.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
           yesterday.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
}

fun getRussianMonthGenitive(month: Int): String {
    return when (month) {
        1 -> "января"
        2 -> "февраля"
        3 -> "марта"
        4 -> "апреля"
        5 -> "мая"
        6 -> "июня"
        7 -> "июля"
        8 -> "августа"
        9 -> "сентября"
        10 -> "октября"
        11 -> "ноября"
        12 -> "декабря"
        else -> ""
    }
}

fun formatDatePill(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val millis = if (timestamp > 10000000000L) timestamp else timestamp * 1000
    val targetCal = Calendar.getInstance().apply { timeInMillis = millis }
    val nowCal = Calendar.getInstance()

    val targetYear = targetCal.get(Calendar.YEAR)
    val targetDayOfYear = targetCal.get(Calendar.DAY_OF_YEAR)

    val nowYear = nowCal.get(Calendar.YEAR)
    val nowDayOfYear = nowCal.get(Calendar.DAY_OF_YEAR)

    // Today
    if (targetYear == nowYear && targetDayOfYear == nowDayOfYear) {
        return "Сегодня"
    }

    // Yesterday
    val yesterdayCal = (nowCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    if (targetYear == yesterdayCal.get(Calendar.YEAR) && targetDayOfYear == yesterdayCal.get(Calendar.DAY_OF_YEAR)) {
        return "Вчера"
    }

    val day = targetCal.get(Calendar.DAY_OF_MONTH)
    val month = targetCal.get(Calendar.MONTH) + 1
    val monthGenitive = getRussianMonthGenitive(month)

    return if (targetYear == nowYear) {
        "$day $monthGenitive"
    } else {
        "$day $monthGenitive $targetYear"
    }
}

fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
    if (timestamp1 <= 0 || timestamp2 <= 0) return false
    val millis1 = if (timestamp1 > 10000000000L) timestamp1 else timestamp1 * 1000
    val millis2 = if (timestamp2 > 10000000000L) timestamp2 else timestamp2 * 1000

    val cal1 = Calendar.getInstance().apply { timeInMillis = millis1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = millis2 }

    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
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
