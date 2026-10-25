package com.example.moodle_usth_mobile_app.data.repository

import android.content.Context
import com.example.moodle_usth_mobile_app.data.local.SessionStore
import com.example.moodle_usth_mobile_app.data.model.AppData
import com.example.moodle_usth_mobile_app.data.model.Assignment
import com.example.moodle_usth_mobile_app.data.remote.ApiException
import com.example.moodle_usth_mobile_app.data.remote.DownloadedFile
import com.example.moodle_usth_mobile_app.data.remote.JsonMapper
import com.example.moodle_usth_mobile_app.data.remote.MoodleApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException

/** Kết quả tải dữ liệu: [fromCache] = true khi không gọi được server và đang dùng bản lưu gần nhất. */
data class LoadResult(val data: AppData, val fromCache: Boolean)

class MoodleRepository(context: Context) {

    private val appContext = context.applicationContext
    val session = SessionStore(appContext)
    private val api = MoodleApi(baseUrl = { session.baseUrl }, token = { session.token })
    private val mapper = JsonMapper()

    /** Ở chế độ xem thử, các thay đổi "đánh dấu hoàn thành" chỉ giữ trong bộ nhớ: id → completedAt (null = bỏ đánh dấu). */
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
        if (!session.demoMode) runCatching { api.logout() }
        session.clear()
    }

    /**
     * Tải toàn bộ dữ liệu. Mất mạng → trả về bản cache gần nhất (nếu có).
     * Token hết hạn (401) → ném [ApiException] để UI đưa về màn đăng nhập.
     */
    suspend fun load(): LoadResult {
        if (session.demoMode) return LoadResult(loadSample(), fromCache = false)
        return try {
            val json = api.bootstrap()
            withContext(Dispatchers.IO) { session.writeCache(json.toString()) }
            LoadResult(mapper.appData(json), fromCache = false)
        } catch (error: ApiException) {
            if (error.status == 401) session.clear()
            throw error
        } catch (error: IOException) {
            val cached = withContext(Dispatchers.IO) { session.readCache() } ?: throw error
            LoadResult(mapper.appData(JSONObject(cached)), fromCache = true)
        }
    }

    /** Đánh dấu hoàn thành / bỏ đánh dấu. Ở chế độ xem thử chỉ cập nhật cục bộ. */
    suspend fun setCompleted(assignment: Assignment, completed: Boolean): Assignment {
        if (session.demoMode) {
            if (!completed && assignment.isGraded) {
                throw ApiException(409, "already_graded", "Bài đã được chấm điểm, không thể bỏ đánh dấu hoàn thành")
            }
            val completedAt = if (completed) System.currentTimeMillis() else null
            demoCompletions[assignment.id] = completedAt
            return assignment.copy(completedAtEpochMs = completedAt)
        }
        return mapper.assignment(api.setCompleted(assignment.id, completed))
    }

    suspend fun markNotificationRead(notificationId: String) {
        if (!session.demoMode) api.markNotificationRead(notificationId)
    }

    suspend fun downloadGradeReport(data: AppData): DownloadedFile {
        if (!session.demoMode) return api.downloadGradeReport()
        val rows = listOf("Subject,Course code,Credits,Completed tasks,Total tasks,Accumulated grade") +
            data.courses.map { course ->
                val updated = LearningRules.courseWithProgress(course, data.assignments)
                "${updated.title},${updated.code},${updated.credits},${updated.done},${updated.total},${course.grade ?: ""}"
            }
        return DownloadedFile("USTH-sample-grade-report.csv", "text/csv", rows.joinToString("\r\n").toByteArray())
    }

    /** Dữ liệu mẫu đóng gói trong assets (sinh bằng `npm run export-sample` ở backend). */
    private suspend fun loadSample(): AppData = withContext(Dispatchers.IO) {
        val json = JSONObject(appContext.assets.open(SAMPLE_ASSET).bufferedReader().use { it.readText() })
        val shift = System.currentTimeMillis() - json.getLong("serverTime")
        val data = JsonMapper(timeShiftMs = shift).appData(json)
        data.copy(assignments = data.assignments.map { assignment ->
            if (assignment.id in demoCompletions) assignment.copy(completedAtEpochMs = demoCompletions[assignment.id])
            else assignment
        })
    }

    private companion object {
        const val SAMPLE_ASSET = "sample_bootstrap.json"
    }
}
