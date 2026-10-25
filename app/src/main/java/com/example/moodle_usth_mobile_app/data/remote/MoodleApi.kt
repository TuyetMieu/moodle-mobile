package com.example.moodle_usth_mobile_app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Lỗi do server trả về (4xx/5xx) kèm mã lỗi trong body `{ error: { code, message } }`. */
class ApiException(val status: Int, val code: String, override val message: String) : IOException(message)

class DownloadedFile(val fileName: String, val mimeType: String, val bytes: ByteArray)

/**
 * Client REST cho backend Moodle USTH (xem backend/README.md).
 * Dùng HttpURLConnection + org.json có sẵn trong Android nên không cần thêm thư viện.
 */
class MoodleApi(
    private val baseUrl: () -> String,
    private val token: () -> String?
) {

    suspend fun login(username: String, password: String): JSONObject =
        request("POST", "/api/v1/auth/login", JSONObject().put("username", username).put("password", password))

    suspend fun logout() {
        request("POST", "/api/v1/auth/logout")
    }

    suspend fun bootstrap(): JSONObject = request("GET", "/api/v1/bootstrap")

    suspend fun setCompleted(assignmentId: String, completed: Boolean): JSONObject =
        request(if (completed) "POST" else "DELETE", "/api/v1/assignments/${assignmentId.encoded()}/completion")

    suspend fun markNotificationRead(notificationId: String): JSONObject =
        request("POST", "/api/v1/notifications/${notificationId.encoded()}/read")

    suspend fun downloadGradeReport(): DownloadedFile = withContext(Dispatchers.IO) {
        val connection = open("GET", "/api/v1/reports/grades.csv")
        try {
            val status = connection.responseCode
            if (status !in 200..299) throw errorFrom(connection, status)
            val disposition = connection.getHeaderField("Content-Disposition").orEmpty()
            val fileName = Regex("filename=\"([^\"]+)\"").find(disposition)?.groupValues?.get(1)
                ?: "USTH-grade-report.csv"
            DownloadedFile(fileName, "text/csv", connection.inputStream.use { it.readBytes() })
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun request(method: String, path: String, body: JSONObject? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val connection = open(method, path)
            try {
                if (body != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                }
                val status = connection.responseCode
                if (status !in 200..299) throw errorFrom(connection, status)
                val text = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                if (text.isBlank()) JSONObject() else JSONObject(text)
            } finally {
                connection.disconnect()
            }
        }

    private fun open(method: String, path: String): HttpURLConnection {
        val connection = URL(baseUrl().trimEnd('/') + path).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.setRequestProperty("Accept", "application/json")
        token()?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
        return connection
    }

    private fun errorFrom(connection: HttpURLConnection, status: Int): ApiException {
        val text = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        val error = runCatching { JSONObject(text).getJSONObject("error") }.getOrNull()
        return ApiException(
            status = status,
            code = error?.optString("code").orEmpty().ifEmpty { "http_$status" },
            message = error?.optString("message").orEmpty().ifEmpty { "Máy chủ trả về lỗi $status" }
        )
    }

    private fun String.encoded(): String = java.net.URLEncoder.encode(this, "UTF-8")

    private companion object {
        const val TIMEOUT_MS = 10_000
    }
}
