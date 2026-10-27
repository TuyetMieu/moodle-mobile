package com.example.moodle_usth_mobile_app.data.remote

import com.example.moodle_usth_mobile_app.data.model.Announcement
import com.example.moodle_usth_mobile_app.data.model.AnnouncementKind
import com.example.moodle_usth_mobile_app.data.model.AppData
import com.example.moodle_usth_mobile_app.data.model.AppNotification
import com.example.moodle_usth_mobile_app.data.model.Assignment
import com.example.moodle_usth_mobile_app.data.model.AssignmentCategory
import com.example.moodle_usth_mobile_app.data.model.ClassSession
import com.example.moodle_usth_mobile_app.data.model.Course
import com.example.moodle_usth_mobile_app.data.model.CourseResource
import com.example.moodle_usth_mobile_app.data.model.ResourceKind
import com.example.moodle_usth_mobile_app.data.model.GradeComponent
import com.example.moodle_usth_mobile_app.data.model.GradeStatus
import com.example.moodle_usth_mobile_app.data.model.NotificationKind
import com.example.moodle_usth_mobile_app.data.model.SemesterOverview
import com.example.moodle_usth_mobile_app.data.model.StudentProfile
import org.json.JSONArray
import org.json.JSONObject

/**
 * Chuyển JSON của API thành model. [timeShiftMs] cộng thêm vào mọi mốc thời gian —
 * dùng khi đọc dữ liệu mẫu đóng gói sẵn để deadline luôn tương đối so với hiện tại.
 */
class JsonMapper(private val timeShiftMs: Long = 0L) {

    fun appData(json: JSONObject): AppData {
        val timetable = json.getJSONObject("timetable")
        return AppData(
            serverTimeEpochMs = json.getLong("serverTime") + timeShiftMs,
            profile = profile(json.getJSONObject("profile")),
            semester = semester(json.getJSONObject("semester")),
            courses = toList(json.getJSONArray("courses")).map { course(it) },
            assignments = toList(json.getJSONArray("assignments")).map { assignment(it) },
            sessions = toList(timetable.getJSONArray("sessions")).map { session(it) },
            semesterStartsAtEpochMs = timetable.getLong("semesterStartsAt") + timeShiftMs,
            semesterEndsAtEpochMs = timetable.getLong("semesterEndsAt") + timeShiftMs,
            notifications = toList(json.getJSONObject("notifications").getJSONArray("items")).map { notification(it) }
        )
    }

    fun profile(json: JSONObject) = StudentProfile(
        username = json.getString("username"),
        fullName = json.getString("fullName"),
        shortName = json.getString("shortName"),
        initials = json.getString("initials"),
        studentId = json.getString("studentId"),
        program = json.getString("program"),
        cohort = json.getString("cohort"),
        academicYear = json.getString("academicYear"),
        semester = json.getString("semester")
    )

    fun assignment(json: JSONObject) = Assignment(
        id = json.getString("id"),
        courseId = json.getString("courseId"),
        courseTitle = json.getString("courseTitle"),
        courseShortTitle = json.getString("courseShortTitle"),
        title = json.getString("title"),
        category = AssignmentCategory.fromKey(json.getString("category")),
        contribution = json.getInt("contribution"),
        description = json.getString("description"),
        dueAtEpochMs = json.getLong("dueAt") + timeShiftMs,
        completedAtEpochMs = getLongOrNull(json, "completedAt")?.plus(timeShiftMs),
        score = getDoubleOrNull(json, "score"),
        maxScore = json.optDouble("maxScore", 10.0),
        feedback = getStringOrNull(json, "feedback")
    )

    private fun semester(json: JSONObject) = SemesterOverview(
        label = json.getString("label"),
        academicYear = json.getString("academicYear"),
        creditsEarned = json.getInt("creditsEarned"),
        creditsMilestone = json.getInt("creditsMilestone"),
        enrolledCredits = json.getInt("enrolledCredits"),
        coursesCount = json.getInt("coursesCount"),
        averageGrade = getDoubleOrNull(json, "averageGrade")
    )

    private fun course(json: JSONObject) = Course(
        id = json.getString("id"),
        title = json.getString("title"),
        shortTitle = json.getString("shortTitle"),
        code = json.getString("code"),
        teacher = json.getString("teacher"),
        initials = json.getString("teacherInitials"),
        imageResName = json.getString("image"),
        credits = json.getInt("credits"),
        tone = json.getString("tone"),
        summary = json.optString("summary"),
        grade = getDoubleOrNull(json, "grade"),
        done = json.optInt("done"),
        total = json.optInt("total"),
        gradeComponents = toList(json.optJSONArray("gradeComponents")).map { gradeComponent(it) },
        announcements = toList(json.optJSONArray("announcements")).map { announcement(it) },
        resources = toList(json.optJSONArray("resources")).map { resource(it) }
    )

    private fun resource(json: JSONObject) = CourseResource(
        id = json.getString("id"),
        week = json.getInt("week"),
        title = json.getString("title"),
        kind = ResourceKind.entries.find { it.name == json.getString("kind") } ?: ResourceKind.NOTES,
        fileType = json.getString("fileType"),
        sizeKb = json.getInt("sizeKb")
    )

    private fun gradeComponent(json: JSONObject) = GradeComponent(
        category = AssignmentCategory.fromKey(json.getString("key")),
        weight = json.getInt("weight"),
        score = getDoubleOrNull(json, "score"),
        status = GradeStatus.entries.find { it.name == json.getString("status") } ?: GradeStatus.NOT_GRADED
    )

    private fun announcement(json: JSONObject) = Announcement(
        id = json.getString("id"),
        courseId = json.getString("courseId"),
        kind = AnnouncementKind.entries.find { it.name == json.getString("kind") } ?: AnnouncementKind.ANNOUNCEMENT,
        title = json.getString("title"),
        body = json.getString("body"),
        author = json.getString("author"),
        authorInitials = json.getString("authorInitials"),
        postedAtEpochMs = json.getLong("postedAt") + timeShiftMs,
        pinned = json.optBoolean("pinned")
    )

    private fun session(json: JSONObject) = ClassSession(
        id = json.getString("id"),
        courseId = json.getString("courseId"),
        weekday = json.getInt("weekday"),
        start = json.getString("start"),
        end = json.getString("end"),
        kind = json.getString("kind"),
        room = json.getString("room")
    )

    private fun notification(json: JSONObject) = AppNotification(
        id = json.getString("id"),
        kind = NotificationKind.entries.find { it.name == json.getString("kind") } ?: NotificationKind.MESSAGE,
        title = json.getString("title"),
        body = json.getString("body"),
        createdAtEpochMs = json.getLong("createdAt") + timeShiftMs,
        read = json.optBoolean("read"),
        assignmentId = getStringOrNull(json, "assignmentId"),
        courseId = getStringOrNull(json, "courseId")
    )
}

/** Đổi JSONArray thành List<JSONObject> để dùng được .map { }. */
private fun toList(array: JSONArray?): List<JSONObject> {
    val list = mutableListOf<JSONObject>()
    if (array == null) return list
    for (i in 0 until array.length()) list.add(array.getJSONObject(i))
    return list
}

// org.json trả về 0 hoặc "null" cho giá trị null, nên phải kiểm tra isNull trước.
private fun getLongOrNull(json: JSONObject, key: String): Long? = if (json.isNull(key)) null else json.getLong(key)

private fun getDoubleOrNull(json: JSONObject, key: String): Double? = if (json.isNull(key)) null else json.getDouble(key)

private fun getStringOrNull(json: JSONObject, key: String): String? = if (json.isNull(key)) null else json.getString(key)
