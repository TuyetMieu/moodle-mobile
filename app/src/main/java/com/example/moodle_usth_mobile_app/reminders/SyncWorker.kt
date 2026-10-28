package com.example.moodle_usth_mobile_app.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.moodle_usth_mobile_app.MainActivity
import com.example.moodle_usth_mobile_app.R
import com.example.moodle_usth_mobile_app.data.local.AppSettings
import com.example.moodle_usth_mobile_app.data.local.LocaleHelper
import com.example.moodle_usth_mobile_app.data.local.SessionStore
import com.example.moodle_usth_mobile_app.data.model.AppData
import com.example.moodle_usth_mobile_app.data.model.NotificationChannelKind
import com.example.moodle_usth_mobile_app.data.remote.ApiException
import com.example.moodle_usth_mobile_app.data.remote.JsonMapper
import com.example.moodle_usth_mobile_app.data.remote.MoodleApi
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Chạy nền khoảng 15 phút một lần (WorkManager — tối thiểu Android cho phép), kể cả khi app đã đóng:
 * 1. Tải dữ liệu mới của sinh viên và lưu cache (mở app lên là có dữ liệu mới, xem được cả khi mất mạng).
 * 2. Đặt lại lịch nhắc hạn nộp (giáo viên đổi deadline → lời nhắc đổi theo).
 * 3. Hiện thông báo hệ thống cho tin mới: tin của ngành, việc thầy cô làm và tin toàn trường.
 * Không cần Firebase: app tự hỏi server thay vì server đẩy xuống.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val session = SessionStore(applicationContext)
        if (session.token == null || session.demoMode || session.role != "STUDENT") return Result.success()
        return try {
            val json = MoodleApi(session).bootstrap()
            session.writeCache(json.toString())
            session.lastSyncedAtMs = System.currentTimeMillis()
            val data = JsonMapper().appData(json)
            ReminderHelper.update(applicationContext, data.assignments)
            UpdateNotifier.notifyNew(applicationContext, data)
            Result.success()
        } catch (error: ApiException) {
            Result.success() // 401 (phiên hết hạn) hoặc lỗi server: đợi lần chạy sau, app sẽ xử lý khi mở
        } catch (error: IOException) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "moodle-sync"

        /** Bật đồng bộ nền (gọi sau khi đăng nhập / mở app). Đã có lịch thì giữ nguyên. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}

/** Hiện thông báo hệ thống cho tin mới (chưa đọc và chưa từng báo). */
object UpdateNotifier {

    const val CHANNEL_PROGRAM = "program_news"
    const val CHANNEL_GENERAL = "general_news"
    const val EXTRA_OPEN_NOTIFICATIONS = "open_notifications"
    private const val MAX_PER_RUN = 5

    /**
     * App vừa tải dữ liệu khi đang mở → người dùng sẽ thấy trong app, chỉ cần ghi nhận là đã báo
     * để worker không báo lại.
     */
    fun markSeen(context: Context, data: AppData) {
        val session = SessionStore(context)
        val newest = data.notifications.maxOfOrNull { it.createdAtEpochMs } ?: return
        if (newest > session.lastNotifiedAtMs) session.lastNotifiedAtMs = newest
    }

    @SuppressLint("MissingPermission") // đã kiểm tra quyền bằng canPost() trước khi hiện thông báo
    fun notifyNew(appContext: Context, data: AppData) {
        val session = SessionStore(appContext)
        val since = session.lastNotifiedAtMs
        val fresh = data.notifications.filter { !it.read && it.createdAtEpochMs > since }.sortedBy { it.createdAtEpochMs }
        markSeen(appContext, data)
        // Lần đầu (chưa từng đồng bộ) thì không báo dồn mọi tin cũ.
        if (since == 0L || fresh.isEmpty()) return
        if (!canPost(appContext)) return

        val settings = AppSettings(appContext)
        val wanted = fresh.filter {
            if (it.channel == NotificationChannelKind.PROGRAM) settings.notifyProgram else settings.notifyGeneral
        }
        if (wanted.isEmpty()) return

        val context = LocaleHelper.localized(appContext)
        createChannels(context)
        val manager = NotificationManagerCompat.from(appContext)
        for (notification in wanted.takeLast(MAX_PER_RUN)) {
            val text = NotificationTexts.of(context, notification, data)
            val channelId = if (notification.channel == NotificationChannelKind.PROGRAM) CHANNEL_PROGRAM else CHANNEL_GENERAL

            // Chạm → mở app: bài tập (nếu có) hoặc danh sách thông báo ở đúng luồng.
            val open = Intent(appContext, MainActivity::class.java)
            if (notification.assignmentId != null) open.putExtra(ReminderReceiver.EXTRA_ASSIGNMENT_ID, notification.assignmentId)
            else open.putExtra(EXTRA_OPEN_NOTIFICATIONS, notification.channel.name)
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pendingIntent = PendingIntent.getActivity(
                appContext, notification.id.hashCode(), open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val built = NotificationCompat.Builder(appContext, channelId)
                .setSmallIcon(R.drawable.ic_stat_deadline)
                .setContentTitle(text.title)
                .setContentText(text.body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text.body))
                .setWhen(notification.createdAtEpochMs)
                .setShowWhen(true)
                .setGroup(channelId)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            manager.notify(notification.id.hashCode(), built)
        }
    }

    private fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val program = NotificationChannel(CHANNEL_PROGRAM, context.getString(R.string.channel_program), NotificationManager.IMPORTANCE_DEFAULT)
        program.description = context.getString(R.string.channel_program_desc)
        val general = NotificationChannel(CHANNEL_GENERAL, context.getString(R.string.channel_general), NotificationManager.IMPORTANCE_DEFAULT)
        general.description = context.getString(R.string.channel_general_desc)
        manager.createNotificationChannel(program)
        manager.createNotificationChannel(general)
    }
}
