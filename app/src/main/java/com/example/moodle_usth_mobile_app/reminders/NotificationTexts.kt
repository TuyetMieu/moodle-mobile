package com.example.moodle_usth_mobile_app.reminders

import android.content.Context
import com.example.moodle_usth_mobile_app.R
import com.example.moodle_usth_mobile_app.data.model.AppData
import com.example.moodle_usth_mobile_app.data.model.AppNotification
import com.example.moodle_usth_mobile_app.data.model.MAX_SCORE
import com.example.moodle_usth_mobile_app.data.model.NotificationKind
import com.example.moodle_usth_mobile_app.ui.Formatters

/** Tiêu đề và nội dung đã viết theo ngôn ngữ của app. */
data class NotificationText(val title: String, val body: String)

/**
 * Viết câu cho một thông báo. Thông báo do server tự sinh (điểm, bài mới, đổi deadline) chỉ gửi tham số,
 * app ghép câu theo ngôn ngữ đang chọn. Tin do người đăng viết (tin lớp, sự kiện, thông báo) giữ nguyên chữ.
 * Dùng chung cho danh sách thông báo trong app và thông báo hệ thống.
 */
object NotificationTexts {

    fun of(context: Context, notification: AppNotification, data: AppData?): NotificationText {
        val assignment = notification.assignmentId?.let { id -> data?.assignments?.find { it.id == id } }
        val course = notification.courseId?.let { id -> data?.courses?.find { it.id == id } }
        val assignmentTitle = assignment?.title ?: notification.body
        val courseTitle = course?.shortTitle ?: assignment?.courseShortTitle ?: ""

        return when (notification.kind) {
            NotificationKind.GRADE -> {
                val score = notification.score ?: assignment?.score
                val scoreText = if (score != null) Formatters.score(score, notification.maxScore ?: MAX_SCORE) else ""
                val body = if (notification.late) context.getString(R.string.notif_grade_body_late, courseTitle, scoreText)
                else context.getString(R.string.notif_grade_body, courseTitle, scoreText)
                NotificationText(context.getString(R.string.notif_grade_title, assignmentTitle), body)
            }
            NotificationKind.ASSIGNMENT_NEW -> {
                val due = notification.dueAtEpochMs ?: assignment?.dueAtEpochMs
                NotificationText(
                    context.getString(R.string.notif_assignment_new_title, courseTitle),
                    context.getString(R.string.notif_assignment_new_body, assignmentTitle, if (due != null) Formatters.dueDate(due) else "")
                )
            }
            NotificationKind.DEADLINE_CHANGED -> {
                val old = notification.oldDueAtEpochMs
                val new = notification.dueAtEpochMs ?: assignment?.dueAtEpochMs
                NotificationText(
                    context.getString(R.string.notif_deadline_changed_title, courseTitle),
                    context.getString(
                        R.string.notif_deadline_changed_body, assignmentTitle,
                        if (old != null) Formatters.dueDate(old) else "—", if (new != null) Formatters.dueDate(new) else "—"
                    )
                )
            }
            NotificationKind.ANNOUNCEMENT -> NotificationText(
                if (courseTitle.isEmpty()) notification.title else context.getString(R.string.notif_class_post_title, courseTitle, notification.title),
                notification.body
            )
            NotificationKind.EVENT -> {
                val eventAt = notification.eventAtEpochMs
                val whenText = when {
                    eventAt == null -> null
                    notification.location.isNullOrBlank() -> Formatters.dateTime(eventAt)
                    else -> context.getString(R.string.notif_event_when, Formatters.dateTime(eventAt), notification.location)
                }
                NotificationText(notification.title, if (whenText == null) notification.body else "$whenText\n${notification.body}")
            }
            NotificationKind.NOTICE -> NotificationText(notification.title, notification.body)
        }
    }
}
