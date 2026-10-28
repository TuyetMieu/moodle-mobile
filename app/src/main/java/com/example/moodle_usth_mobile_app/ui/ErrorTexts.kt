package com.example.moodle_usth_mobile_app.ui

import android.content.Context
import com.example.moodle_usth_mobile_app.R
import com.example.moodle_usth_mobile_app.data.remote.ApiException

/**
 * Đổi lỗi thành câu dễ hiểu theo ngôn ngữ của app.
 * Lỗi đã biết mã (code) thì dịch; lỗi lạ thì dùng câu server gửi về.
 */
object ErrorTexts {

    private val BY_CODE = mapOf(
        "missing_credentials" to R.string.error_missing_credentials,
        "invalid_credentials" to R.string.error_invalid_credentials,
        "too_many_attempts" to R.string.error_too_many_attempts,
        "unauthorized" to R.string.error_session_expired,
        "already_graded" to R.string.error_already_graded,
        "forbidden" to R.string.error_forbidden,
        "weights_must_sum_100" to R.string.error_weights_sum,
        "component_in_use" to R.string.error_component_in_use,
        "bonus_in_use" to R.string.error_bonus_in_use,
        "bonus_disabled" to R.string.error_bonus_disabled,
        "assignment_graded" to R.string.error_assignment_graded,
        "invalid_dueAt" to R.string.error_due_in_past,
        "duplicate_component" to R.string.error_duplicate_component
    )

    fun of(context: Context, error: Throwable, baseUrl: String): String {
        if (error is ApiException) {
            val res = BY_CODE[error.code] ?: return error.message
            return context.getString(res)
        }
        return when (error) {
            is java.net.SocketTimeoutException -> context.getString(R.string.error_timeout, baseUrl)
            is java.net.ConnectException, is java.net.NoRouteToHostException -> context.getString(R.string.error_connect, baseUrl)
            else -> context.getString(R.string.error_generic, baseUrl)
        }
    }
}
