package com.example.moodle_usth_mobile_app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.moodle_usth_mobile_app.data.model.Assignment
import com.example.moodle_usth_mobile_app.ui.Formatters
import org.json.JSONArray
import org.json.JSONObject

/** Một lần nhắc: nhắc bài [assignmentId] vào lúc [triggerAtEpochMs] (trước hạn [leadMs]). */
data class DeadlineReminder(
    val assignmentId: String,
    val title: String,
    val courseTitle: String,
    val dueAtEpochMs: Long,
    val leadMs: Long,
    val triggerAtEpochMs: Long
)

/**
 * Nhắc hạn nộp bằng thông báo hệ thống.
 *
 * Cách làm: với mỗi bài chưa xong, đặt một "báo thức" (AlarmManager) lúc còn 24 giờ và lúc còn 2 giờ.
 * Tới giờ, Android gọi [ReminderReceiver] để hiện thông báo.
 * Danh sách báo thức đã đặt được lưu lại để lần sau huỷ được, và để đặt lại sau khi khởi động lại máy.
 */
object ReminderHelper {

    private const val HOUR = 3_600_000L
    private const val PREFS = "moodle_reminders"
    private const val MAX_REMINDERS = 24

    /** Nhắc trước hạn 24 giờ và 2 giờ. */
    val LEAD_TIMES = listOf(24 * HOUR, 2 * HOUR)

    // ---------------------------------------------------------- bật / tắt

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("enabled", false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("enabled", enabled).apply()
    }

    /** false nếu chưa cấp quyền thông báo hoặc đã tắt thông báo của app trong Cài đặt. */
    fun canShowNotifications(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    // ---------------------------------------------------------- tính lịch nhắc (không dùng Android, test được)

    /** Lịch nhắc cho các bài chưa xong và chưa quá hạn. Mốc nhắc đã qua thì bỏ. */
    fun makePlan(assignments: List<Assignment>, nowMs: Long): List<DeadlineReminder> {
        val plan = mutableListOf<DeadlineReminder>()
        for (assignment in assignments) {
            if (assignment.isCompleted) continue
            if (assignment.dueAtEpochMs <= nowMs) continue
            for (lead in LEAD_TIMES) {
                val triggerAt = assignment.dueAtEpochMs - lead
                if (triggerAt > nowMs) {
                    plan.add(
                        DeadlineReminder(
                            assignmentId = assignment.id,
                            title = assignment.title,
                            courseTitle = assignment.courseTitle,
                            dueAtEpochMs = assignment.dueAtEpochMs,
                            leadMs = lead,
                            triggerAtEpochMs = triggerAt
                        )
                    )
                }
            }
        }
        // Lần nhắc sớm nhất đứng đầu, chỉ giữ tối đa 24 lần.
        return plan.sortedBy { it.triggerAtEpochMs }.take(MAX_REMINDERS)
    }

    /**
     * "Còn 1 giờ 20 phút", tính lúc thông báo hiện ra (Android có thể giao báo thức muộn tới ~1 giờ,
     * nên không ghi cứng "Còn 2 giờ"). Trả về null nếu đã quá hạn.
     */
    fun remainingText(dueAtEpochMs: Long, nowMs: Long): String? {
        val remaining = dueAtEpochMs - nowMs
        if (remaining <= 0) return null
        val minutes = (remaining + 59_999) / 60_000 // làm tròn lên
        val hours = minutes / 60
        val minutesLeft = minutes % 60
        if (minutes < 60) return "Còn $minutes phút"
        if (minutes < 24 * 60) {
            if (minutesLeft == 0L) return "Còn $hours giờ"
            return "Còn $hours giờ $minutesLeft phút"
        }
        val days = (remaining + 24 * HOUR - 1) / (24 * HOUR)
        return "Còn $days ngày"
    }

    /** Mỗi lần nhắc có một mã riêng, giống nhau giữa các lần đặt lại để không bị nhân đôi. */
    fun requestCodeOf(reminder: DeadlineReminder): Int = (reminder.assignmentId + "@" + reminder.leadMs).hashCode()

    // ---------------------------------------------------------- đặt / huỷ báo thức

    /** Huỷ lịch cũ rồi đặt lịch mới theo danh sách bài. Trả về số lời nhắc đã đặt. */
    fun update(context: Context, assignments: List<Assignment>): Int {
        val plan = if (isEnabled(context)) makePlan(assignments, System.currentTimeMillis()) else emptyList()
        schedule(context, plan)
        return plan.size
    }

    /** Đặt lại lịch đã lưu (dùng sau khi khởi động lại máy, vì Android xoá hết báo thức). */
    fun restore(context: Context) {
        val now = System.currentTimeMillis()
        val saved = loadPlan(context)
        val stillValid = mutableListOf<DeadlineReminder>()
        for (reminder in saved) {
            if (reminder.triggerAtEpochMs > now) stillValid.add(reminder)
        }
        schedule(context, if (isEnabled(context)) stillValid else emptyList())
    }

    fun cancelAll(context: Context) {
        schedule(context, emptyList())
    }

    private fun schedule(context: Context, plan: List<DeadlineReminder>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 1. Huỷ các báo thức đã đặt lần trước.
        for (old in loadPlan(context)) {
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCodeOf(old), reminderIntent(context),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }

        // 2. Đặt báo thức mới. setAndAllowWhileIdle không cần quyền "báo thức chính xác".
        for (reminder in plan) {
            val intent = reminderIntent(context)
            intent.putExtra(ReminderReceiver.EXTRA_ASSIGNMENT_ID, reminder.assignmentId)
            intent.putExtra(ReminderReceiver.EXTRA_TITLE, reminder.title)
            intent.putExtra(ReminderReceiver.EXTRA_COURSE_TITLE, reminder.courseTitle)
            intent.putExtra(ReminderReceiver.EXTRA_DUE_AT, reminder.dueAtEpochMs)
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCodeOf(reminder), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAtEpochMs, pendingIntent)
        }

        // 3. Lưu lại danh sách vừa đặt.
        savePlan(context, plan)
    }

    private fun reminderIntent(context: Context): Intent {
        val intent = Intent(context, ReminderReceiver::class.java)
        intent.action = ReminderReceiver.ACTION_REMIND
        return intent
    }

    // ---------------------------------------------------------- lưu danh sách nhắc (JSON trong SharedPreferences)

    private fun savePlan(context: Context, plan: List<DeadlineReminder>) {
        val array = JSONArray()
        for (reminder in plan) {
            val item = JSONObject()
            item.put("assignmentId", reminder.assignmentId)
            item.put("title", reminder.title)
            item.put("courseTitle", reminder.courseTitle)
            item.put("dueAt", reminder.dueAtEpochMs)
            item.put("lead", reminder.leadMs)
            item.put("triggerAt", reminder.triggerAtEpochMs)
            array.put(item)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("plan", array.toString()).apply()
    }

    private fun loadPlan(context: Context): List<DeadlineReminder> {
        val text = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("plan", null) ?: return emptyList()
        val result = mutableListOf<DeadlineReminder>()
        try {
            val array = JSONArray(text)
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                result.add(
                    DeadlineReminder(
                        assignmentId = item.getString("assignmentId"),
                        title = item.getString("title"),
                        courseTitle = item.getString("courseTitle"),
                        dueAtEpochMs = item.getLong("dueAt"),
                        leadMs = item.getLong("lead"),
                        triggerAtEpochMs = item.getLong("triggerAt")
                    )
                )
            }
        } catch (error: Exception) {
            return emptyList() // dữ liệu lưu bị hỏng thì coi như chưa có gì
        }
        return result
    }

    /** Nội dung dòng thứ hai của thông báo. */
    fun notificationBody(courseTitle: String, dueAtEpochMs: Long): String =
        "$courseTitle · Hạn nộp ${Formatters.dueDate(dueAtEpochMs)}"
}
