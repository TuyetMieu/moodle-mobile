package com.example.moodle_usth_mobile_app.ui

import android.content.Context
import com.example.moodle_usth_mobile_app.R
import com.example.moodle_usth_mobile_app.data.model.formatScore
import com.example.moodle_usth_mobile_app.data.repository.TimeLeft
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Định dạng thời gian hiển thị (theo giờ của thiết bị và ngôn ngữ app đang dùng).
 * Hàm nào có chữ ("hôm nay", "còn 2 giờ"...) thì cần [Context] để lấy chuỗi đúng ngôn ngữ.
 */
object Formatters {

    private fun format(pattern: String, epochMs: Long): String =
        SimpleDateFormat(pattern, Locale.getDefault()).format(Date(epochMs))

    /** "28 Oct, 14:05" */
    fun dueDate(epochMs: Long): String = format("d MMM, HH:mm", epochMs)

    /** "Tue 28 Oct, 14:05" — dùng cho sự kiện và lịch. */
    fun dateTime(epochMs: Long): String = format("EEE d MMM, HH:mm", epochMs)

    fun time(epochMs: Long): String = format("HH:mm", epochMs)

    /** "14:05 hôm nay" / "14:05, 24/10" — dùng cho thời điểm đồng bộ. */
    fun syncedAt(context: Context, epochMs: Long, nowMs: Long): String {
        val time = time(epochMs)
        return if (dayIndex(nowMs) == dayIndex(epochMs)) context.getString(R.string.time_synced_today, time)
        else "$time, ${format("dd/MM", epochMs)}"
    }

    /** "Today, 08:30" / "Yesterday, 14:15" / "16 May, 14:15". */
    fun postedAt(context: Context, epochMs: Long, nowMs: Long): String {
        val days = dayIndex(nowMs) - dayIndex(epochMs)
        return when (days) {
            0L -> context.getString(R.string.time_today_at, time(epochMs))
            1L -> context.getString(R.string.time_yesterday_at, time(epochMs))
            else -> format("d MMM, HH:mm", epochMs)
        }
    }

    /** "Vừa xong", "5 phút trước", "2 giờ trước", "Hôm qua", "3 ngày trước". */
    fun relative(context: Context, epochMs: Long, nowMs: Long): String {
        val minutes = ((nowMs - epochMs) / 60_000).coerceAtLeast(0)
        val res = context.resources
        return when {
            minutes < 1 -> context.getString(R.string.time_just_now)
            minutes < 60 -> res.getQuantityString(R.plurals.time_minutes_ago, minutes.toInt(), minutes.toInt())
            minutes < 24 * 60 -> res.getQuantityString(R.plurals.time_hours_ago, (minutes / 60).toInt(), (minutes / 60).toInt())
            minutes < 48 * 60 -> context.getString(R.string.time_yesterday)
            else -> res.getQuantityString(R.plurals.time_days_ago, (minutes / (24 * 60)).toInt(), (minutes / (24 * 60)).toInt())
        }
    }

    /** "Overdue" / "30 min left" / "6h 42m left" / "3 days left". */
    fun timeLeft(context: Context, left: TimeLeft): String = when {
        left.overdue -> context.getString(R.string.time_left_overdue)
        left.days > 0 -> context.resources.getQuantityString(R.plurals.time_left_days, left.days, left.days)
        left.hours > 0 -> context.getString(R.string.time_left_hours, left.hours, left.minutes)
        else -> context.getString(R.string.time_left_minutes, left.minutes)
    }

    /** Câu trong thông báo nhắc hạn nộp: "Còn 1 giờ 20 phút". */
    fun reminderRemaining(context: Context, left: TimeLeft): String = when {
        left.days > 0 -> context.resources.getQuantityString(R.plurals.reminder_left_days, left.days, left.days)
        left.hours > 0 && left.minutes == 0 -> context.resources.getQuantityString(R.plurals.reminder_left_hours, left.hours, left.hours)
        left.hours > 0 -> context.getString(R.string.reminder_left_hours_minutes, left.hours, left.minutes)
        else -> context.resources.getQuantityString(R.plurals.reminder_left_minutes, left.minutes, left.minutes)
    }

    /** "18 / 20" */
    fun score(score: Double, maxScore: Double): String = "${formatScore(score)} / ${formatScore(maxScore)}"

    private fun dayIndex(epochMs: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = epochMs
        return (epochMs + calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET)) / 86_400_000L
    }
}
