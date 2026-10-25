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
            courses = json.getJSONArray("courses").objects().map(::course),
            assignments = json.getJSONArray("assignments").objects().map(::assignment),
            sessions = timetable.getJSONArray("sessions").objects().map(::session),
            semesterStartsAtEpochMs = timetable.getLong("semesterStartsAt") + timeShiftMs,
            semesterEndsAtEpochMs = timetable.getLong("semesterEndsAt") + timeShiftMs,
            notifications = json.getJSONObject("notifications").getJSONArray("items").objects().map(::notification)
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
        completedAtEpochMs = json.optLongOrNull("completedAt")?.plus(timeShiftMs),
        score = json.optDoubleOrNull("score"),
        maxScore = json.optDouble("maxScore", 10.0),
        feedback = json.optStringOrNull("feedback")
    )

    private fun semester(json: JSONObject) = SemesterOverview(
        label = json.getString("label"),
        academicYear = json.getString("academicYear"),
        creditsEarned = json.getInt("creditsEarned"),
        creditsMilestone = json.getInt("creditsMilestone"),
        enrolledCredits = json.getInt("enrolledCredits"),
        coursesCount = json.getInt("coursesCount"),
        averageGrade = json.optDoubleOrNull("averageGrade")
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
        grade = json.optDoubleOrNull("grade"),
        done = json.optInt("done"),
        total = json.optInt("total"),
        gradeComponents = json.optJSONArray("gradeComponents")?.objects()?.map(::gradeComponent).orEmpty(),
        announcements = json.optJSONArray("announcements")?.objects()?.map(::announcement).orEmpty(),
        resources = json.optJSONArray("resources")?.objects()?.map(::resource).orEmpty()
    )

    private fun resource(json: JSONObject) = CourseResource(
        id = json.getString("id"),
        week = json.getInt("week"),
        title = json.getString("title"),
        kind = runCatching { ResourceKind.valueOf(json.getString("kind")) }.getOrDefault(ResourceKind.NOTES),
        fileType = json.getString("fileType"),
        sizeKb = json.getInt("sizeKb")
    )

    private fun gradeComponent(json: JSONObject) = GradeComponent(
        category = AssignmentCategory.fromKey(json.getString("key")),
        weight = json.getInt("weight"),
        score = json.optDoubleOrNull("score"),
        status = runCatching { GradeStatus.valueOf(json.getString("status")) }.getOrDefault(GradeStatus.NOT_GRADED)
    )

    private fun announcement(json: JSONObject) = Announcement(
        id = json.getString("id"),
        courseId = json.getString("courseId"),
        kind = runCatching { AnnouncementKind.valueOf(json.getString("kind")) }.getOrDefault(AnnouncementKind.ANNOUNCEMENT),
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
        kind = runCatching { NotificationKind.valueOf(json.getString("kind")) }.getOrDefault(NotificationKind.MESSAGE),
        title = json.getString("title"),
        body = json.getString("body"),
        createdAtEpochMs = json.getLong("createdAt") + timeShiftMs,
        read = json.optBoolean("read"),
        assignmentId = json.optStringOrNull("assignmentId"),
        courseId = json.optStringOrNull("courseId")
    )
}

private fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }

private fun JSONObject.optLongOrNull(key: String): Long? = if (isNull(key)) null else getLong(key)

private fun JSONObject.optDoubleOrNull(key: String): Double? = if (isNull(key)) null else getDouble(key)

private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key)) null else getString(key)
