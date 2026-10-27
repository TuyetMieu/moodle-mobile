package com.example.moodle_usth_mobile_app.data.local

import android.content.Context
import com.example.moodle_usth_mobile_app.BuildConfig
import java.io.File

/**
 * Lưu những thứ cần nhớ giữa các lần mở app:
 * token đăng nhập, địa chỉ server, chế độ xem thử, thời điểm đồng bộ
 * và một bản sao dữ liệu (cache) để xem được khi mất mạng.
 */
class SessionStore(context: Context) {

    private val prefs = context.getSharedPreferences("moodle_session", Context.MODE_PRIVATE)
    private val cacheFile = File(context.filesDir, "bootstrap_cache.json")

    /** Địa chỉ backend. Mặc định lấy từ build.gradle (http://10.0.2.2:8080 cho emulator). */
    var baseUrl: String
        get() = prefs.getString("base_url", null) ?: BuildConfig.API_BASE_URL
        set(value) {
            prefs.edit().putString("base_url", value.trim().trimEnd('/')).apply()
        }

    var token: String?
        get() = prefs.getString("token", null)
        set(value) {
            prefs.edit().putString("token", value).apply()
        }

    /** true khi người dùng chọn "Xem thử với dữ liệu mẫu" (không cần server). */
    var demoMode: Boolean
        get() = prefs.getBoolean("demo_mode", false)
        set(value) {
            prefs.edit().putBoolean("demo_mode", value).apply()
        }

    /** Thời điểm tải dữ liệu thành công gần nhất. 0 = chưa có. */
    var lastSyncedAtMs: Long
        get() = prefs.getLong("last_synced_at", 0L)
        set(value) {
            prefs.edit().putLong("last_synced_at", value).apply()
        }

    fun readCache(): String? {
        if (!cacheFile.exists()) return null
        return cacheFile.readText()
    }

    fun writeCache(json: String) {
        cacheFile.writeText(json)
    }

    /** Đăng xuất: xoá token, chế độ xem thử, thời điểm đồng bộ và cache. Giữ lại địa chỉ server. */
    fun clear() {
        prefs.edit()
            .remove("token")
            .remove("demo_mode")
            .remove("last_synced_at")
            .apply()
        cacheFile.delete()
    }
}
