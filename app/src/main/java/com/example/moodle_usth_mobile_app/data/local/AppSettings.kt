package com.example.moodle_usth_mobile_app.data.local

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Ngôn ngữ giao diện. SYSTEM = theo ngôn ngữ máy (không phải vi/fr thì dùng tiếng Anh). */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null), VI("vi"), EN("en"), FR("fr")
}

/** Một mốc nhắc trước hạn nộp mà người dùng có thể chọn. */
enum class ReminderLead(val minutes: Long) {
    THREE_DAYS(3 * 24 * 60L),
    ONE_DAY(24 * 60L),
    TWELVE_HOURS(12 * 60L),
    SIX_HOURS(6 * 60L),
    TWO_HOURS(2 * 60L),
    ONE_HOUR(60L),
    THIRTY_MINUTES(30L);

    val millis: Long get() = minutes * 60_000L

    companion object {
        val DEFAULT = setOf(ONE_DAY, TWO_HOURS)
    }
}

/**
 * Cài đặt của người dùng trên máy này (không đồng bộ lên server):
 * giao diện sáng/tối, ngôn ngữ, mốc nhắc hạn nộp và loại thông báo muốn nhận.
 */
class AppSettings(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() = enumOr(prefs.getString("theme", null), ThemeMode.SYSTEM)
        set(value) = prefs.edit().putString("theme", value.name).apply()

    var language: AppLanguage
        get() = enumOr(prefs.getString("language", null), AppLanguage.SYSTEM)
        // commit() để Activity tạo lại ngay sau đó đọc được giá trị mới.
        set(value) {
            prefs.edit().putString("language", value.name).commit()
        }

    var reminderLeads: Set<ReminderLead>
        get() {
            val saved = prefs.getStringSet("reminder_leads", null) ?: return ReminderLead.DEFAULT
            return saved.mapNotNull { name -> ReminderLead.entries.find { it.name == name } }.toSet()
        }
        set(value) = prefs.edit().putStringSet("reminder_leads", value.map { it.name }.toSet()).apply()

    /** Hiện thông báo hệ thống khi có tin mới của ngành. */
    var notifyProgram: Boolean
        get() = prefs.getBoolean("notify_program", true)
        set(value) = prefs.edit().putBoolean("notify_program", value).apply()

    /** Hiện thông báo hệ thống khi thầy cô có điểm mới, bài mới, đổi deadline, tin của lớp hoặc tin toàn trường. */
    var notifyGeneral: Boolean
        get() = prefs.getBoolean("notify_general", true)
        set(value) = prefs.edit().putBoolean("notify_general", value).apply()

    private inline fun <reified T : Enum<T>> enumOr(name: String?, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: fallback

    companion object {
        const val PREFS = "moodle_settings"
    }
}

/**
 * Áp ngôn ngữ người dùng chọn cho một Context.
 * Activity gọi trong attachBaseContext; ViewModel/Receiver/Worker gọi [localized] để lấy chuỗi đúng ngôn ngữ.
 */
object LocaleHelper {

    /** Ngôn ngữ của máy lúc app khởi động (trước khi app đổi Locale mặc định). */
    private val systemLocale: Locale = Locale.getDefault()

    fun localeOf(context: Context): Locale? {
        val tag = AppSettings(context).language.tag ?: return null
        return Locale.forLanguageTag(tag)
    }

    fun localized(context: Context): Context {
        val locale = localeOf(context)
        if (locale == null) {
            Locale.setDefault(systemLocale) // vừa chuyển từ ngôn ngữ khác về "Theo hệ thống"
            return context
        }
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
