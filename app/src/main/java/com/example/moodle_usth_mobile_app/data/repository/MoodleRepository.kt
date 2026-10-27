package com.example.moodle_usth_mobile_app.data.repository

import android.content.Context
import com.example.moodle_usth_mobile_app.data.local.SessionStore
import com.example.moodle_usth_mobile_app.data.model.AppData
import com.example.moodle_usth_mobile_app.data.model.Assignment
import com.example.moodle_usth_mobile_app.data.remote.ApiException
import com.example.moodle_usth_mobile_app.data.remote.DownloadedFile
import com.example.moodle_usth_mobile_app.data.remote.JsonMapper
import com.example.moodle_usth_mobile_app.data.remote.MoodleApi
import org.json.JSONObject
import java.io.IOException

/**
 * Kết quả tải dữ liệu.
 * fromCache = true: không gọi được server, đang dùng bản đã lưu.
 * syncedAtMs: lúc dữ liệu được lấy từ server (null ở chế độ xem thử).
 */
class LoadResult(val data: AppData, val fromCache: Boolean, val syncedAtMs: Long?)

/**
 * Nơi duy nhất ViewModel lấy dữ liệu. Có 3 trường hợp:
 * 1. Online: gọi server và lưu cache.
 * 2. Mất mạng: đọc cache đã lưu.
 * 3. Chế độ xem thử: đọc file mẫu trong assets, không cần server.
 */
class MoodleRepository(context: Context) {

    private val appContext = context.applicationContext
    val session = SessionStore(appContext)
    private val api = MoodleApi(session)
    private val mapper = JsonMapper()

    /** Ở chế độ xem thử, bài đã đánh dấu xong chỉ nhớ trong bộ nhớ: id bài → thời điểm xong (null = bỏ đánh dấu). */
    private val demoCompletions = mutableMapOf<String, Long?>()

    val isSignedIn: Boolean get() = session.token != null || session.demoMode
    val isDemoMode: Boolean get() = session.demoMode

    suspend fun login(baseUrl: String, username: String, password: String) {
        session.baseUrl = baseUrl
        val response = api.login(username.trim(), password)
        session.demoMode = false
        session.token = response.getString("token")
    }

    fun startDemo() {
        demoCompletions.clear()
        session.token = null
        session.demoMode = true
    }

    suspend fun logout() {
        if (!session.demoMode) {
            try {
                api.logout()
            } catch (error: Exception) {
                // Không gọi được server cũng không sao, vẫn xoá dữ liệu trên máy.
            }
        }
        session.clear()
    }

    /**
     * Tải toàn bộ dữ liệu. Mất mạng → dùng bản cache (nếu có).
     * Token hết hạn (401) → ném lỗi để ViewModel đưa về màn đăng nhập.
     */
    suspend fun load(): LoadResult {
        if (session.demoMode) return LoadResult(loadSample(), false, null)

        try {
            val json = api.bootstrap()
            val now = System.currentTimeMillis()
            session.writeCache(json.toString())
            session.lastSyncedAtMs = now
            return LoadResult(mapper.appData(json), false, now)
        } catch (error: ApiException) {
            // Lỗi do server trả về (401, 500...) → không dùng cache, báo lỗi luôn.
            if (error.status == 401) session.clear()
            throw error
        } catch (error: IOException) {
            // Lỗi mạng → thử đọc cache.
            val cached = session.readCache() ?: throw error
            val syncedAt = if (session.lastSyncedAtMs > 0) session.lastSyncedAtMs else null
            return LoadResult(mapper.appData(JSONObject(cached)), true, syncedAt)
        }
    }

    /** Đánh dấu hoàn thành / bỏ đánh dấu. Ở chế độ xem thử chỉ đổi trong bộ nhớ. */
    suspend fun setCompleted(assignment: Assignment, completed: Boolean): Assignment {
        if (session.demoMode) {
            if (!completed && assignment.isGraded) {
                throw ApiException(409, "already_graded", "Bài đã được chấm điểm, không thể bỏ đánh dấu hoàn thành")
            }
            val completedAt = if (completed) System.currentTimeMillis() else null
            demoCompletions[assignment.id] = completedAt
            return assignment.copy(completedAtEpochMs = completedAt)
        }
        val json = api.setCompleted(assignment.id, completed)
        return mapper.assignment(json)
    }

    suspend fun markNotificationRead(notificationId: String) {
        if (!session.demoMode) api.markNotificationRead(notificationId)
    }

    suspend fun downloadGradeReport(data: AppData): DownloadedFile {
        if (!session.demoMode) return api.downloadGradeReport()

        // Chế độ xem thử: tự tạo file CSV từ dữ liệu đang có.
        var csv = "Subject,Course code,Credits,Completed tasks,Total tasks,Accumulated grade"
        for (course in data.courses) {
            val updated = LearningRules.courseWithProgress(course, data.assignments)
            val grade = if (course.grade == null) "" else course.grade.toString()
            csv += "\r\n${updated.title},${updated.code},${updated.credits},${updated.done},${updated.total},$grade"
        }
        return DownloadedFile("USTH-sample-grade-report.csv", "text/csv", csv.toByteArray())
    }

    /**
     * Dữ liệu mẫu trong assets/sample_bootstrap.json (tạo bằng `npm run export-sample` ở backend).
     * Mọi mốc thời gian được dời theo giờ hiện tại để deadline luôn "sắp tới".
     */
    private fun loadSample(): AppData {
        val text = appContext.assets.open("sample_bootstrap.json").bufferedReader().readText()
        val json = JSONObject(text)
        val shift = System.currentTimeMillis() - json.getLong("serverTime")
        val data = JsonMapper(shift).appData(json)

        // Áp lại các bài người dùng đã đánh dấu trong lần xem thử này.
        val assignments = data.assignments.map { assignment ->
            if (demoCompletions.containsKey(assignment.id)) {
                assignment.copy(completedAtEpochMs = demoCompletions[assignment.id])
            } else {
                assignment
            }
        }
        return data.copy(assignments = assignments)
    }
}
