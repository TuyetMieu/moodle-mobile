package com.example.moodle_usth_mobile_app.data.local

import android.content.Context
import androidx.core.content.edit
import com.example.moodle_usth_mobile_app.BuildConfig
import java.io.File

/** Lưu token, địa chỉ server và bản sao bootstrap gần nhất để mở app khi mất mạng. */
class SessionStore(context: Context) {

    private val prefs = context.getSharedPreferences("moodle_session", Context.MODE_PRIVATE)
    private val cacheFile = File(context.filesDir, "bootstrap_cache.json")

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, null) ?: BuildConfig.API_BASE_URL
        set(value) = prefs.edit { putString(KEY_BASE_URL, value.trim().trimEnd('/')) }

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit { if (value == null) remove(KEY_TOKEN) else putString(KEY_TOKEN, value) }

    /** true khi người dùng chọn "Chế độ xem thử" với dữ liệu mẫu, không cần server. */
    var demoMode: Boolean
        get() = prefs.getBoolean(KEY_DEMO, false)
        set(value) = prefs.edit { putBoolean(KEY_DEMO, value) }

    fun readCache(): String? = cacheFile.takeIf { it.exists() }?.readText()

    fun writeCache(json: String) = cacheFile.writeText(json)

    fun clear() {
        prefs.edit {
            remove(KEY_TOKEN)
            remove(KEY_DEMO)
        }
        cacheFile.delete()
    }

    private companion object {
        const val KEY_BASE_URL = "base_url"
        const val KEY_TOKEN = "token"
        const val KEY_DEMO = "demo_mode"
    }
}
