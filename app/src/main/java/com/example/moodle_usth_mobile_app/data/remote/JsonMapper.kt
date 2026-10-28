package com.example.moodle_usth_mobile_app.data.remote

import com.example.moodle_usth_mobile_app.data.model.Announcement
import com.example.moodle_usth_mobile_app.data.model.AnnouncementKind
import com.example.moodle_usth_mobile_app.data.model.AppData
import com.example.moodle_usth_mobile_app.data.model.AppNotification
import com.example.moodle_usth_mobile_app.data.model.Assignment
import com.example.moodle_usth_mobile_app.data.model.ClassSession
import com.example.moodle_usth_mobile_app.data.model.Course
import com.example.moodle_usth_mobile_app.data.model.CourseResource
import com.example.moodle_usth_mobile_app.data.model.CourseStudent
import com.example.moodle_usth_mobile_app.data.model.GradeComponent
import com.example.moodle_usth_mobile_app.data.model.GradeStatus
import com.example.moodle_usth_mobile_app.data.model.MAX_SCORE
import com.example.moodle_usth_mobile_app.data.model.NotificationChannelKind
import com.example.moodle_usth_mobile_app.data.model.NotificationKind
import com.example.moodle_usth_mobile_app.data.model.PostAudience
import com.example.moodle_usth_mobile_app.data.model.PostKind
import com.example.moodle_usth_mobile_app.data.model.ResourceKind
import com.example.moodle_usth_mobile_app.data.model.SchemeComponent
import com.example.moodle_usth_mobile_app.data.model.SemesterOverview
import com.example.moodle_usth_mobile_app.data.model.SessionKind
import com.example.moodle_usth_mobile_app.data.model.TeacherData
import com.example.moodle_usth_mobile_app.data.model.TeacherPost
import com.example.moodle_usth_mobile_app.data.model.TeachingAssignment
import com.example.moodle_usth_mobile_app.data.model.TeachingCourse
import com.example.moodle_usth_mobile_app.data.model.UserProfile
import com.example.moodle_usth_mobile_app.data.model.UserRole
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
            notifications = notifications(json.getJSONObject("notifications"))
        )
    }

    fun notifications(json: JSONObject): List<AppNotification> =
        toList(json.getJSONArray("items")).map { notification(it) }

    fun profile(json: JSONObject) = UserProfile(
        username = json.getString("username"),
        role = if (json.optString("role") == "TEACHER") UserRole.TEACHER else UserRole.STUDENT,
        fullName = json.getString("fullName"),
        shortName = json.getString("shortName"),
        initials = json.getString("initials"),
        memberCode = json.getString("memberCode"),
        programCode = json.optString("programCode"),
        programName = json.optString("programName"),
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
        description = json.getString("description"),
        componentId = getStringOrNull(json, "componentId"),
        componentName = getStringOrNull(json, "componentName"),
        componentWeight = getIntOrNull(json, "componentWeight"),
        isBonus = json.optBoolean("isBonus"),
        bonusMax = getDoubleOrNull(json, "bonusMax"),
        weight = json.optDouble("weight", 1.0),
        contribution = getDoubleOrNull(json, "contribution"),
        dueAtEpochMs = json.getLong("dueAt") + timeShiftMs,
        createdAtEpochMs = json.optLong("createdAt") + timeShiftMs,
        completedAtEpochMs = getLongOrNull(json, "completedAt")?.plus(timeShiftMs),
        late = json.optBoolean("late"),
        score = getDoubleOrNull(json, "score"),
        maxScore = json.optDouble("maxScore", MAX_SCORE),
        feedback = getStringOrNull(json, "feedback"),
        gradedBy = getStringOrNull(json, "gradedBy")
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
        baseGrade = getDoubleOrNull(json, "baseGrade"),
        bonusEnabled = json.optBoolean("bonusEnabled"),
        bonusMax = json.optDouble("bonusMax", 0.0),
        bonus = getDoubleOrNull(json, "bonus"),
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
        id = json.getString("id"),
        name = json.getString("name"),
        weight = json.getInt("weight"),
        score = getDoubleOrNull(json, "score"),
        status = GradeStatus.entries.find { it.name == json.getString("status") } ?: GradeStatus.NOT_GRADED,
        gradedShare = json.optInt("gradedShare"),
        tasks = json.optInt("tasks")
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
        kind = if (json.getString("kind") == "LAB") SessionKind.LAB else SessionKind.LECTURE,
        room = json.getString("room")
    )

    private fun notification(json: JSONObject): AppNotification {
        val data = json.optJSONObject("data")
        return AppNotification(
            id = json.getString("id"),
            channel = if (json.optString("channel") == "PROGRAM") NotificationChannelKind.PROGRAM else NotificationChannelKind.GENERAL,
            kind = NotificationKind.entries.find { it.name == json.getString("kind") } ?: NotificationKind.NOTICE,
            title = json.getString("title"),
            body = json.getString("body"),
            createdAtEpochMs = json.getLong("createdAt") + timeShiftMs,
            read = json.optBoolean("read"),
            assignmentId = getStringOrNull(json, "assignmentId"),
            courseId = getStringOrNull(json, "courseId"),
            author = getStringOrNull(json, "author"),
            eventAtEpochMs = getLongOrNull(json, "eventAt")?.plus(timeShiftMs),
            location = getStringOrNull(json, "location"),
            score = data?.let { getDoubleOrNull(it, "score") },
            maxScore = data?.let { getDoubleOrNull(it, "maxScore") },
            late = data?.optBoolean("late") ?: false,
            dueAtEpochMs = data?.let { getLongOrNull(it, "dueAt") }?.plus(timeShiftMs),
            oldDueAtEpochMs = data?.let { getLongOrNull(it, "oldDueAt") }?.plus(timeShiftMs)
        )
    }

    // -------------------------------------------------------------- giáo viên

    fun teacherData(json: JSONObject): TeacherData {
        val semester = json.getJSONObject("semester")
        return TeacherData(
            serverTimeEpochMs = json.getLong("serverTime"),
            profile = profile(json.getJSONObject("profile")),
            semesterLabel = semester.getString("label"),
            academicYear = semester.getString("academicYear"),
            courses = toList(json.getJSONArray("courses")).map { teachingCourse(it) },
            posts = toList(json.getJSONArray("posts")).map { teacherPost(it) }
        )
    }

    fun teachingCourse(json: JSONObject) = TeachingCourse(
        id = json.getString("id"),
        title = json.getString("title"),
        shortTitle = json.getString("shortTitle"),
        code = json.getString("code"),
        imageResName = json.getString("image"),
        tone = json.getString("tone"),
        credits = json.getInt("credits"),
        summary = json.optString("summary"),
        bonusEnabled = json.optBoolean("bonusEnabled"),
        bonusMax = json.optDouble("bonusMax", 0.0),
        components = toList(json.getJSONArray("components")).map {
            SchemeComponent(id = it.getString("id"), name = it.getString("name"), weight = it.getInt("weight"), tasks = it.optInt("tasks"))
        },
        assignments = toList(json.getJSONArray("assignments")).map { teachingAssignment(it) },
        students = toList(json.getJSONArray("students")).map { courseStudent(it) },
        averageGrade = getDoubleOrNull(json, "averageGrade"),
        sessions = toList(json.optJSONArray("sessions")).map { session(it) },
        announcements = toList(json.optJSONArray("announcements")).map { announcement(it) }
    )

    private fun teachingAssignment(json: JSONObject) = TeachingAssignment(
        id = json.getString("id"),
        title = json.getString("title"),
        description = json.optString("description"),
        componentId = getStringOrNull(json, "componentId"),
        componentName = getStringOrNull(json, "componentName"),
        isBonus = json.optBoolean("isBonus"),
        weight = json.optDouble("weight", 1.0),
        contribution = getDoubleOrNull(json, "contribution"),
        dueAtEpochMs = json.getLong("dueAt"),
        updatedAtEpochMs = json.optLong("updatedAt"),
        students = json.optInt("students"),
        submitted = json.optInt("submitted"),
        graded = json.optInt("graded"),
        averageScore = getDoubleOrNull(json, "averageScore")
    )

    private fun courseStudent(json: JSONObject) = CourseStudent(
        id = json.getLong("id"),
        fullName = json.getString("fullName"),
        initials = json.getString("initials"),
        memberCode = json.getString("memberCode"),
        programName = json.optString("programName"),
        done = json.optInt("done"),
        total = json.optInt("total"),
        missing = json.optInt("missing"),
        grade = getDoubleOrNull(json, "grade"),
        baseGrade = getDoubleOrNull(json, "baseGrade"),
        bonus = getDoubleOrNull(json, "bonus")
    )

    fun teacherPost(json: JSONObject) = TeacherPost(
        id = json.getString("id"),
        audience = PostAudience.entries.find { it.name == json.getString("audience") } ?: PostAudience.ALL,
        kind = if (json.optString("kind") == "EVENT") PostKind.EVENT else PostKind.NOTICE,
        title = json.getString("title"),
        body = json.getString("body"),
        eventAtEpochMs = getLongOrNull(json, "eventAt"),
        location = getStringOrNull(json, "location"),
        createdAtEpochMs = json.getLong("createdAt")
    )
}

/** Đổi JSONArray thành List<JSONObject> để dùng được .map { }. */
private fun toList(array: JSONArray?): List<JSONObject> {
    val list = mutableListOf<JSONObject>()
    if (array == null) return list
    for (i in 0 until array.length()) list.add(array.getJSONObject(i))
    return list
}

// org.json trả về 0 hoặc "null" cho giá trị null, nên phải kiểm tra has/isNull trước.
private fun getLongOrNull(json: JSONObject, key: String): Long? = if (json.isNull(key)) null else json.getLong(key)

private fun getIntOrNull(json: JSONObject, key: String): Int? = if (json.isNull(key)) null else json.getInt(key)

private fun getDoubleOrNull(json: JSONObject, key: String): Double? = if (json.isNull(key)) null else json.getDouble(key)

private fun getStringOrNull(json: JSONObject, key: String): String? = if (json.isNull(key)) null else json.getString(key)
