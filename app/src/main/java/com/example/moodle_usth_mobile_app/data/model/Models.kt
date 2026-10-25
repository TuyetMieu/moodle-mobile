package com.example.moodle_usth_mobile_app.data.model

import java.util.Locale

enum class AssignmentCategory(val label: String, val weight: Int, val tone: String) {
    ATTENDANCE("Attendance", 10, "blue"),
    MIDTERM_LABS("Mid-term & labs", 30, "amber"),
    FINAL_PROJECT("Final project", 60, "purple");

    companion object {
        fun fromKey(key: String): AssignmentCategory =
            entries.firstOrNull { it.name == key } ?: MIDTERM_LABS
    }
}

enum class AssignmentStatus {
    DONE, OVERDUE, URGENT, UPCOMING
}

data class Assignment(
    val id: String,
    val courseId: String,
    val courseTitle: String,
    val courseShortTitle: String,
    val title: String,
    val category: AssignmentCategory,
    val contribution: Int,
    val description: String,
    val dueAtEpochMs: Long,
    val completedAtEpochMs: Long? = null,
    val score: Double? = null,
    val maxScore: Double = 10.0,
    val feedback: String? = null
) {
    val isCompleted: Boolean get() = completedAtEpochMs != null
    val isGraded: Boolean get() = score != null
    val weightLabel: String get() = "$contribution% of course grade"
}

data class Course(
    val id: String,
    val title: String,
    val shortTitle: String,
    val code: String,
    val teacher: String,
    val initials: String,
    val imageResName: String,
    val credits: Int,
    val tone: String, // "blue", "green", "purple"
    val summary: String,
    val grade: Double?,
    val done: Int = 0,
    val total: Int = 0,
    val gradeComponents: List<GradeComponent> = emptyList(),
    val announcements: List<Announcement> = emptyList(),
    val resources: List<CourseResource> = emptyList()
) {
    val progress: Int get() = if (total == 0) 0 else Math.round(done * 100f / total)
    val gradeText: String get() = grade?.let { formatGrade(it) } ?: "—"
}

enum class GradeStatus { GRADED, IN_PROGRESS, NOT_GRADED }

data class GradeComponent(
    val category: AssignmentCategory,
    val weight: Int,
    val score: Double?,
    val status: GradeStatus
)

enum class AnnouncementKind { ANNOUNCEMENT, RESOURCE }

enum class ResourceKind(val label: String) { SLIDES("Slides"), NOTES("Notes"), EXERCISE("Exercise"), DATASET("Dataset") }

data class CourseResource(
    val id: String,
    val week: Int,
    val title: String,
    val kind: ResourceKind,
    val fileType: String,
    val sizeKb: Int
) {
    val sizeLabel: String
        get() = if (sizeKb >= 1024) String.format(Locale.US, "%.1f MB", sizeKb / 1024.0) else "$sizeKb KB"
}

data class Announcement(
    val id: String,
    val courseId: String,
    val kind: AnnouncementKind,
    val title: String,
    val body: String,
    val author: String,
    val authorInitials: String,
    val postedAtEpochMs: Long,
    val pinned: Boolean
)

data class ClassSession(
    val id: String,
    val courseId: String,
    val weekday: Int, // 0 = Sunday, 1 = Monday, ..., 6 = Saturday
    val start: String,
    val end: String,
    val kind: String, // "Lý thuyết" | "Thực hành"
    val room: String
)

data class StudentProfile(
    val username: String,
    val fullName: String,
    val shortName: String,
    val initials: String,
    val studentId: String,
    val program: String,
    val cohort: String,
    val academicYear: String,
    val semester: String
)

data class SemesterOverview(
    val label: String,
    val academicYear: String,
    val creditsEarned: Int,
    val creditsMilestone: Int,
    val enrolledCredits: Int,
    val coursesCount: Int,
    val averageGrade: Double?
) {
    val title: String get() = "$label, $academicYear"
}

enum class NotificationKind { GRADE, MESSAGE }

data class AppNotification(
    val id: String,
    val kind: NotificationKind,
    val title: String,
    val body: String,
    val createdAtEpochMs: Long,
    val read: Boolean,
    val assignmentId: String?,
    val courseId: String?
)

/** Toàn bộ dữ liệu một sinh viên cần, tương ứng với GET /api/v1/bootstrap. */
data class AppData(
    val serverTimeEpochMs: Long,
    val profile: StudentProfile,
    val semester: SemesterOverview,
    val courses: List<Course>,
    val assignments: List<Assignment>,
    val sessions: List<ClassSession>,
    val semesterStartsAtEpochMs: Long,
    val semesterEndsAtEpochMs: Long,
    val notifications: List<AppNotification>
)

fun formatGrade(value: Double): String = String.format(Locale.US, "%.1f", value)
