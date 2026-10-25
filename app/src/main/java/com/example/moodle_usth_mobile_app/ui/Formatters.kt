package com.example.moodle_usth_mobile_app.ui

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Định dạng thời gian hiển thị (theo giờ của thiết bị). */
object Formatters {

    private val dueFormat = SimpleDateFormat("d MMM, HH:mm", Locale.ENGLISH)
    private val dayTimeFormat = SimpleDateFormat("MMM d, HH:mm", Locale.ENGLISH)
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.ENGLISH)

    fun dueDate(epochMs: Long): String = dueFormat.format(Date(epochMs))

    /** "Today, 08:30" / "Yesterday, 14:15" / "May 16, 14:15". */
    fun postedAt(epochMs: Long, nowMs: Long): String {
        val days = dayIndex(nowMs) - dayIndex(epochMs)
        return when (days) {
            0L -> "Today, ${timeFormat.format(Date(epochMs))}"
            1L -> "Yesterday, ${timeFormat.format(Date(epochMs))}"
            else -> dayTimeFormat.format(Date(epochMs))
        }
    }

    /** "2 HOURS AGO", "JUST NOW", "3 DAYS AGO". */
    fun relative(epochMs: Long, nowMs: Long): String {
        val minutes = (nowMs - epochMs) / 60_000
        return when {
            minutes < 1 -> "JUST NOW"
            minutes < 60 -> "$minutes MIN AGO"
            minutes < 120 -> "1 HOUR AGO"
            minutes < 24 * 60 -> "${minutes / 60} HOURS AGO"
            minutes < 48 * 60 -> "YESTERDAY"
            else -> "${minutes / (24 * 60)} DAYS AGO"
        }
    }

    private fun dayIndex(epochMs: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = epochMs }
        return (epochMs + calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET)) / 86_400_000L
    }
}
