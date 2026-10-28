package com.example.moodle_usth_mobile_app.data.model

import java.util.Locale

/** Thang điểm của USTH: mọi điểm đều trên 20. */
const val MAX_SCORE = 20.0

enum class UserRole { STUDENT, TEACHER }

enum class AssignmentStatus {
    DONE, OVERDUE, URGENT, UPCOMING
}

data class Assignment(
    val id: String,
    val courseId: String,
    val courseTitle: String,
    val courseShortTitle: String,
    val title: String,
    val description: String,
    /** Thành phần điểm (Attendance, Midterm...). null nếu là bài bonus. */
    val componentId: String?,
    val componentName: String?,
    /** Trọng số % của thành phần trong điểm môn. */
    val componentWeight: Int?,
    val isBonus: Boolean = false,
    /** Bài bonus: bonus tối đa của môn. */
    val bonusMax: Double? = null,
    /** Trọng số tương đối trong thành phần (do giáo viên đặt). */
    val weight: Double = 1.0,
    /** % điểm môn mà bài này đóng góp. null với bài bonus. */
    val contribution: Double?,
    val dueAtEpochMs: Long,
    val createdAtEpochMs: Long = 0L,
    val completedAtEpochMs: Long? = null,
    val late: Boolean = false,
    val score: Double? = null,
    val maxScore: Double = MAX_SCORE,
    val feedback: String? = null,
    /** "AUTO" = máy chấm, "TEACHER" = giáo viên chấm. */
    val gradedBy: String? = null
) {
    val isCompleted: Boolean get() = completedAtEpochMs != null
    val isGraded: Boolean get() = score != null
    val isAutoGraded: Boolean get() = gradedBy == "AUTO"
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
    /** Điểm môn hiện tại (thang 20, đã cộng bonus). null = chưa có bài nào được chấm. */
    val grade: Double?,
    /** Điểm trung bình có trọng số, chưa cộng bonus. */
    val baseGrade: Double? = grade,
    val bonusEnabled: Boolean = false,
    val bonusMax: Double = 0.0,
    /** Bonus đang có (null nếu môn không có bonus). */
    val bonus: Double? = null,
    val done: Int = 0,
    val total: Int = 0,
    val gradeComponents: List<GradeComponent> = emptyList(),
    val announcements: List<Announcement> = emptyList(),
    val resources: List<CourseResource> = emptyList()
) {
    val progress: Int get() = if (total == 0) 0 else Math.round(done * 100f / total)
    val gradeText: String get() = if (grade != null) formatGrade(grade) else "—"
}

enum class GradeStatus { GRADED, IN_PROGRESS, NOT_GRADED }

/** Một thành phần điểm của môn, kèm điểm hiện tại của sinh viên. */
data class GradeComponent(
    val id: String,
    val name: String,
    val weight: Int,
    val score: Double?,
    val status: GradeStatus,
    /** % khối lượng bài của thành phần đã được chấm. */
    val gradedShare: Int = 0,
    val tasks: Int = 0
)

enum class AnnouncementKind { ANNOUNCEMENT, RESOURCE }

enum class ResourceKind { SLIDES, NOTES, EXERCISE, DATASET }

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

enum class SessionKind { LECTURE, LAB }

data class ClassSession(
    val id: String,
    val courseId: String,
    val weekday: Int, // 0 = Sunday, 1 = Monday, ..., 6 = Saturday
    val start: String,
    val end: String,
    val kind: SessionKind,
    val room: String
)

/** Hồ sơ người dùng (sinh viên hoặc giáo viên). */
data class UserProfile(
    val username: String,
    val role: UserRole,
    val fullName: String,
    val shortName: String,
    val initials: String,
    /** Mã sinh viên hoặc mã cán bộ. */
    val memberCode: String,
    val programCode: String,
    val programName: String,
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

/**
 * Luồng thông báo:
 * - PROGRAM: tin/sự kiện dành cho ngành của sinh viên.
 * - GENERAL: việc thầy cô làm (điểm, bài mới, đổi deadline, tin của lớp) và tin gửi toàn trường.
 */
enum class NotificationChannelKind { PROGRAM, GENERAL }

enum class NotificationKind {
    GRADE, ANNOUNCEMENT, ASSIGNMENT_NEW, DEADLINE_CHANGED, EVENT, NOTICE
}

data class AppNotification(
    val id: String,
    val channel: NotificationChannelKind,
    val kind: NotificationKind,
    val title: String,
    val body: String,
    val createdAtEpochMs: Long,
    val read: Boolean,
    val assignmentId: String?,
    val courseId: String?,
    val author: String? = null,
    /** Sự kiện (EVENT): thời điểm diễn ra và địa điểm. */
    val eventAtEpochMs: Long? = null,
    val location: String? = null,
    /** Tham số để app tự viết câu thông báo theo ngôn ngữ đang chọn. */
    val score: Double? = null,
    val maxScore: Double? = null,
    val late: Boolean = false,
    val dueAtEpochMs: Long? = null,
    val oldDueAtEpochMs: Long? = null
)

/** Toàn bộ dữ liệu một sinh viên cần, tương ứng với GET /api/v1/bootstrap. */
data class AppData(
    val serverTimeEpochMs: Long,
    val profile: UserProfile,
    val semester: SemesterOverview,
    val courses: List<Course>,
    val assignments: List<Assignment>,
    val sessions: List<ClassSession>,
    val semesterStartsAtEpochMs: Long,
    val semesterEndsAtEpochMs: Long,
    val notifications: List<AppNotification>
)

// ------------------------------------------------------------------ giáo viên

/** Thành phần điểm nhìn từ phía giáo viên (để sửa thang điểm). */
data class SchemeComponent(
    val id: String?,
    val name: String,
    val weight: Int,
    /** Số bài tập đang thuộc thành phần này (không xoá được nếu > 0). */
    val tasks: Int = 0
)

data class TeachingAssignment(
    val id: String,
    val title: String,
    val description: String,
    val componentId: String?,
    val componentName: String?,
    val isBonus: Boolean,
    val weight: Double,
    val contribution: Double?,
    val dueAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val students: Int,
    val submitted: Int,
    val graded: Int,
    val averageScore: Double?
)

data class CourseStudent(
    val id: Long,
    val fullName: String,
    val initials: String,
    val memberCode: String,
    val programName: String,
    val done: Int,
    val total: Int,
    val missing: Int,
    val grade: Double?,
    val baseGrade: Double?,
    val bonus: Double?
)

data class TeachingCourse(
    val id: String,
    val title: String,
    val shortTitle: String,
    val code: String,
    val imageResName: String,
    val tone: String,
    val credits: Int,
    val summary: String,
    val bonusEnabled: Boolean,
    val bonusMax: Double,
    val components: List<SchemeComponent>,
    val assignments: List<TeachingAssignment>,
    val students: List<CourseStudent>,
    val averageGrade: Double?,
    val sessions: List<ClassSession>,
    val announcements: List<Announcement>
)

enum class PostAudience { COURSE, PROGRAM, ALL }

enum class PostKind { NOTICE, EVENT }

/** Tin giáo viên đã đăng cho ngành / toàn trường. */
data class TeacherPost(
    val id: String,
    val audience: PostAudience,
    val kind: PostKind,
    val title: String,
    val body: String,
    val eventAtEpochMs: Long?,
    val location: String?,
    val createdAtEpochMs: Long
)

/** Dữ liệu của giáo viên, tương ứng với GET /api/v1/teacher/bootstrap. */
data class TeacherData(
    val serverTimeEpochMs: Long,
    val profile: UserProfile,
    val semesterLabel: String,
    val academicYear: String,
    val courses: List<TeachingCourse>,
    val posts: List<TeacherPost>
)

fun formatGrade(value: Double): String = String.format(Locale.US, "%.1f", value)

/** Điểm gọn: 18 → "18", 17.5 → "17.5". */
fun formatScore(value: Double): String =
    if (value == Math.floor(value)) value.toLong().toString() else String.format(Locale.US, "%.1f", value)
