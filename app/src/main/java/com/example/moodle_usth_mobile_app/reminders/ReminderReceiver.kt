package com.example.moodle_usth_mobile_app.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.moodle_usth_mobile_app.MainActivity
import com.example.moodle_usth_mobile_app.data.local.LocaleHelper
import com.example.moodle_usth_mobile_app.data.repository.LearningRules
import com.example.moodle_usth_mobile_app.ui.Formatters
import com.example.moodle_usth_mobile_app.R

/**
 * Android gọi class này khi:
 * - tới giờ nhắc (ACTION_REMIND) → hiện thông báo;
 * - máy vừa khởi động lại / app vừa được cập nhật → đặt lại các báo thức (Android đã xoá hết).
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REMIND) {
            showNotification(context, intent)
        } else if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            ReminderHelper.restore(context)
            SyncWorker.schedule(context) // WorkManager tự giữ lịch, gọi lại cho chắc sau khi cập nhật app
        }
    }

    private fun showNotification(appContext: Context, intent: Intent) {
        if (!ReminderHelper.isEnabled(appContext)) return
        val context = LocaleHelper.localized(appContext) // chữ trong thông báo theo ngôn ngữ của app

        // Android 13 trở lên phải có quyền POST_NOTIFICATIONS.
        if (Build.VERSION.SDK_INT >= 33) {
            val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (permission != PackageManager.PERMISSION_GRANTED) return
        }

        val assignmentId = intent.getStringExtra(EXTRA_ASSIGNMENT_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val courseTitle = intent.getStringExtra(EXTRA_COURSE_TITLE) ?: ""
        val dueAt = intent.getLongExtra(EXTRA_DUE_AT, 0L)

        // Tính lại thời gian còn lại lúc hiện thông báo (báo thức có thể đến muộn); đã quá hạn thì không nhắc nữa.
        val left = LearningRules.timeLeft(dueAt, System.currentTimeMillis())
        if (left.overdue) return
        val remaining = Formatters.reminderRemaining(context, left)

        // Kênh thông báo (bắt buộc từ Android 8). Tạo nhiều lần cũng không sao.
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.channel_deadlines), NotificationManager.IMPORTANCE_HIGH)
            channel.description = context.getString(R.string.channel_deadlines_desc)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        // Chạm vào thông báo → mở MainActivity, kèm id bài tập để mở đúng bài.
        val openIntent = Intent(context, MainActivity::class.java)
        openIntent.putExtra(EXTRA_ASSIGNMENT_ID, assignmentId)
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            context, assignmentId.hashCode(), openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_deadline)
            .setContentTitle(context.getString(R.string.reminder_title, remaining, title))
            .setContentText(ReminderHelper.notificationBody(context, courseTitle, dueAt))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Dùng id theo bài: lời nhắc 2 giờ sẽ thay lời nhắc 24 giờ của cùng bài.
        NotificationManagerCompat.from(appContext).notify(assignmentId.hashCode(), notification)
    }

    companion object {
        const val ACTION_REMIND = "com.example.moodle_usth_mobile_app.action.DEADLINE_REMINDER"
        const val EXTRA_ASSIGNMENT_ID = "assignment_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_COURSE_TITLE = "course_title"
        const val EXTRA_DUE_AT = "due_at"
        const val CHANNEL_ID = "deadlines"
    }
}
