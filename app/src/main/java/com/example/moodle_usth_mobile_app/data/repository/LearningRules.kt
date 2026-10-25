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

    fun timeRemaining(assignment: Assignment, nowMs: Long): String {
        val remaining = assignment.dueAtEpochMs - nowMs
        if (remaining <= 0) return "Overdue"
        val minutes = ((remaining + 59_999) / 60_000).toInt()
        if (minutes < 60) return "$minutes min left"
        val hours = minutes / 60
        if (hours < 24) return "${hours}h ${minutes % 60}m left"
        val days = ((remaining + DAY_MS - 1) / DAY_MS).toInt()
        return "$days days left"
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
