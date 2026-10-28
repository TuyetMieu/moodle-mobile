package com.example.moodle_usth_mobile_app.data.repository

import com.example.moodle_usth_mobile_app.data.model.Assignment
import com.example.moodle_usth_mobile_app.data.model.AssignmentStatus
import com.example.moodle_usth_mobile_app.data.model.Course

data class StudySummary(
    val pendingCount: Int,
    val urgentCount: Int,
    val recentDoneCount: Int,
    val totalDone: Int,
    val totalAssignments: Int,
    val overallProgressPercent: Int
)

/**
 * Thời gian còn lại tới hạn nộp (chưa thành chữ — màn hình tự viết theo ngôn ngữ đang chọn).
 * - overdue: đã quá hạn.
 * - Còn dưới 1 giờ: chỉ có [minutes]. Dưới 24 giờ: [hours] + [minutes]. Từ 24 giờ: [days] (làm tròn lên).
 */
data class TimeLeft(val overdue: Boolean, val days: Int = 0, val hours: Int = 0, val minutes: Int = 0)

/**
 * Quy tắc tính trạng thái/tiến độ — giống hệt backend (backend/src/services.js) để app
 * cập nhật tức thì theo đồng hồ mà không phải gọi lại API mỗi phút.
 */
object LearningRules {

    private const val HOUR_MS = 3_600_000L
    private const val DAY_MS = 24 * HOUR_MS
    private const val RECENT_DONE_WINDOW_MS = 14 * DAY_MS

    fun status(assignment: Assignment, nowMs: Long): AssignmentStatus = when {
        assignment.isCompleted -> AssignmentStatus.DONE
        assignment.dueAtEpochMs <= nowMs -> AssignmentStatus.OVERDUE
        assignment.dueAtEpochMs - nowMs <= DAY_MS -> AssignmentStatus.URGENT
        else -> AssignmentStatus.UPCOMING
    }

    fun isUrgent(status: AssignmentStatus) =
        status == AssignmentStatus.URGENT || status == AssignmentStatus.OVERDUE

    fun timeLeft(dueAtEpochMs: Long, nowMs: Long): TimeLeft {
        val remaining = dueAtEpochMs - nowMs
        if (remaining <= 0) return TimeLeft(overdue = true)
        val minutes = ((remaining + 59_999) / 60_000).toInt() // làm tròn lên
        if (minutes < 60) return TimeLeft(overdue = false, minutes = minutes)
        val hours = minutes / 60
        if (hours < 24) return TimeLeft(overdue = false, hours = hours, minutes = minutes % 60)
        return TimeLeft(overdue = false, days = ((remaining + DAY_MS - 1) / DAY_MS).toInt())
    }

    /** Tiến độ môn học tính lại từ danh sách bài tập hiện tại (kể cả bài vừa đánh dấu xong). */
    fun courseWithProgress(course: Course, assignments: List<Assignment>): Course {
        val own = assignments.filter { it.courseId == course.id }
        if (own.isEmpty()) return course
        return course.copy(done = own.count { it.isCompleted }, total = own.size)
    }

    fun pending(assignments: List<Assignment>, courseId: String? = null): List<Assignment> =
        assignments
            .filter { !it.isCompleted && (courseId == null || it.courseId == courseId) }
            .sortedBy { it.dueAtEpochMs }

    /**
     * Ưu tiên của sinh viên: bài CHƯA XONG và CHƯA QUÁ HẠN, deadline gần nhất đứng đầu.
     * Bài quá hạn không còn nộp đúng hạn được nên tách riêng ở [overdue].
     */
    fun priorities(assignments: List<Assignment>, nowMs: Long): List<Assignment> =
        assignments.filter { !it.isCompleted && it.dueAtEpochMs > nowMs }.sortedBy { it.dueAtEpochMs }

    /** Bài chưa xong đã quá hạn, quá hạn gần đây nhất đứng đầu. */
    fun overdue(assignments: List<Assignment>, nowMs: Long): List<Assignment> =
        assignments.filter { !it.isCompleted && it.dueAtEpochMs <= nowMs }.sortedByDescending { it.dueAtEpochMs }

    /** Việc nên làm tiếp theo: deadline gần nhất; nếu không còn bài nào sắp tới thì bài vừa quá hạn. */
    fun nextDeadline(assignments: List<Assignment>, nowMs: Long): Assignment? =
        priorities(assignments, nowMs).firstOrNull() ?: overdue(assignments, nowMs).firstOrNull()

    fun summary(assignments: List<Assignment>, nowMs: Long): StudySummary {
        val pending = assignments.filter { !it.isCompleted }
        val done = assignments.size - pending.size
        return StudySummary(
            pendingCount = pending.size,
            urgentCount = pending.count { isUrgent(status(it, nowMs)) },
            recentDoneCount = assignments.count {
                val completedAt = it.completedAtEpochMs
                completedAt != null && nowMs - completedAt <= RECENT_DONE_WINDOW_MS
            },
            totalDone = done,
            totalAssignments = assignments.size,
            overallProgressPercent = if (assignments.isEmpty()) 0 else Math.round(done * 100f / assignments.size)
        )
    }
}
