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
        }
    }

    private fun showNotification(context: Context, intent: Intent) {
        if (!ReminderHelper.isEnabled(context)) return

        // Android 13 trở lên phải có quyền POST_NOTIFICATIONS.
        if (Build.VERSION.SDK_INT >= 33) {
            val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (permission != PackageManager.PERMISSION_GRANTED) return
        }

        val assignmentId = intent.getStringExtra(EXTRA_ASSIGNMENT_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val courseTitle = intent.getStringExtra(EXTRA_COURSE_TITLE) ?: ""
        val dueAt = intent.getLongExtra(EXTRA_DUE_AT, 0L)

        // Tính lại thời gian còn lại; đã quá hạn thì không nhắc nữa.
        val remaining = ReminderHelper.remainingText(dueAt, System.currentTimeMillis()) ?: return

        // Kênh thông báo (bắt buộc từ Android 8). Tạo nhiều lần cũng không sao.
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, "Nhắc hạn nộp", NotificationManager.IMPORTANCE_HIGH)
            channel.description = "Nhắc bài tập chưa hoàn thành 24 giờ và 2 giờ trước hạn nộp"
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
            .setContentTitle("$remaining: $title")
            .setContentText(ReminderHelper.notificationBody(courseTitle, dueAt))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Dùng id theo bài: lời nhắc 2 giờ sẽ thay lời nhắc 24 giờ của cùng bài.
        NotificationManagerCompat.from(context).notify(assignmentId.hashCode(), notification)
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
