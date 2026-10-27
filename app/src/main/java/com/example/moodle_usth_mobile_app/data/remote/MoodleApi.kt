package com.example.moodle_usth_mobile_app.data.remote

import com.example.moodle_usth_mobile_app.data.local.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Lỗi do server trả về (4xx/5xx), body có dạng { "error": { "code", "message" } }. */
class ApiException(val status: Int, val code: String, override val message: String) : IOException(message)

/** File tải về (báo cáo điểm CSV). */
class DownloadedFile(val fileName: String, val mimeType: String, val bytes: ByteArray)

/**
 * Gọi API của backend (xem backend/README.md).
 * Dùng HttpURLConnection và org.json có sẵn trong Android, không cần thêm thư viện.
 * Địa chỉ server và token lấy từ [session] mỗi lần gọi.
 */
class MoodleApi(private val session: SessionStore) {

    suspend fun login(username: String, password: String): JSONObject {
        val body = JSONObject()
        body.put("username", username)
        body.put("password", password)
        return request("POST", "/api/v1/auth/login", body)
    }

    suspend fun logout() {
        request("POST", "/api/v1/auth/logout", null)
    }

    suspend fun bootstrap(): JSONObject = request("GET", "/api/v1/bootstrap", null)

    suspend fun setCompleted(assignmentId: String, completed: Boolean): JSONObject {
        val method = if (completed) "POST" else "DELETE"
        return request(method, "/api/v1/assignments/" + encode(assignmentId) + "/completion", null)
    }

    suspend fun markNotificationRead(notificationId: String): JSONObject =
        request("POST", "/api/v1/notifications/" + encode(notificationId) + "/read", null)

    suspend fun downloadGradeReport(): DownloadedFile = withContext(Dispatchers.IO) {
        val connection = openConnection("GET", "/api/v1/reports/grades.csv")
        try {
            val status = connection.responseCode
            if (status < 200 || status > 299) throw readError(connection, status)

            // Lấy tên file trong header: Content-Disposition: attachment; filename="abc.csv"
            var fileName = "USTH-grade-report.csv"
            val disposition = connection.getHeaderField("Content-Disposition") ?: ""
            val start = disposition.indexOf("filename=\"")
            if (start >= 0) {
                val end = disposition.indexOf("\"", start + 10)
                if (end > start) fileName = disposition.substring(start + 10, end)
            }

            val bytes = connection.inputStream.readBytes()
            DownloadedFile(fileName, "text/csv", bytes)
        } finally {
            connection.disconnect()
        }
    }

    /** Gửi một request và trả về JSON. Chạy trên Dispatchers.IO vì mạng không được chạy trên luồng giao diện. */
    private suspend fun request(method: String, path: String, body: JSONObject?): JSONObject = withContext(Dispatchers.IO) {
        val connection = openConnection(method, path)
        try {
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.write(body.toString().toByteArray(Charsets.UTF_8))
                connection.outputStream.close()
            }
            val status = connection.responseCode
            if (status < 200 || status > 299) throw readError(connection, status)

            val text = connection.inputStream.bufferedReader(Charsets.UTF_8).readText()
            if (text.isBlank()) JSONObject() else JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(method: String, path: String): HttpURLConnection {
        val url = URL(session.baseUrl.trimEnd('/') + path)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.setRequestProperty("Accept", "application/json")
        val token = session.token
        if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
        return connection
    }

    /** Đọc body lỗi { "error": { "code", "message" } } thành ApiException. */
    private fun readError(connection: HttpURLConnection, status: Int): ApiException {
        var code = "http_$status"
        var message = "Máy chủ trả về lỗi $status"
        try {
            val text = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.readText() ?: ""
            val error = JSONObject(text).getJSONObject("error")
            if (error.optString("code").isNotEmpty()) code = error.getString("code")
            if (error.optString("message").isNotEmpty()) message = error.getString("message")
        } catch (e: Exception) {
            // body không phải JSON → giữ thông báo mặc định
        }
        return ApiException(status, code, message)
    }

    private fun encode(text: String): String = URLEncoder.encode(text, "UTF-8")
}
