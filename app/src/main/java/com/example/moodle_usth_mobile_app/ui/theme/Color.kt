package com.example.moodle_usth_mobile_app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Bảng màu của app cho một chế độ (sáng hoặc tối).
 *
 * Màn hình dùng các tên quen thuộc (TextPrimary, CardSurface, AccentGreenLight...) ở cuối file.
 * Mỗi tên là một getter đọc [LocalAppColors], nên tự đổi theo chế độ sáng/tối mà không phải sửa từng màn.
 * Lưu ý: getter là @Composable, trong lambda không phải composable (VD drawBehind) hãy đọc ra biến trước.
 */
@Immutable
data class AppColors(
    val isDark: Boolean,
    val primary: Color,
    val primaryDark: Color,
    val primaryLight: Color,
    val background: Color,      // nền màn hình
    val card: Color,            // nền thẻ, dialog, thanh điều hướng
    val cardBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val green: Color,
    val greenLight: Color,
    val amber: Color,
    val amberLight: Color,
    val coral: Color,
    val coralLight: Color,
    val blue: Color,
    val blueLight: Color,
    val purple: Color,
    val purpleLight: Color
)

val LightAppColors = AppColors(
    isDark = false,
    primary = Color(0xFF4F46E5),
    primaryDark = Color(0xFF3730A3),
    primaryLight = Color(0xFFEEF2FF),
    background = Color(0xFFF8FAFC),
    card = Color.White,
    cardBorder = Color(0xFFE2E8F0),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF64748B),
    green = Color(0xFF10B981),
    greenLight = Color(0xFFECFDF5),
    amber = Color(0xFFF59E0B),
    amberLight = Color(0xFFFEF3C7),
    coral = Color(0xFFEF4444),
    coralLight = Color(0xFFFEE2E2),
    blue = Color(0xFF0284C7),
    blueLight = Color(0xFFE0F2FE),
    purple = Color(0xFF8B5CF6),
    purpleLight = Color(0xFFF3E8FF)
)

val DarkAppColors = AppColors(
    isDark = true,
    primary = Color(0xFF818CF8),
    primaryDark = Color(0xFFA5B4FC),
    primaryLight = Color(0xFF1E1B4B),
    background = Color(0xFF0B1120),
    card = Color(0xFF151E2E),
    cardBorder = Color(0xFF283548),
    textPrimary = Color(0xFFF1F5F9),
    textSecondary = Color(0xFF94A3B8),
    green = Color(0xFF34D399),
    greenLight = Color(0xFF0F2E25),
    amber = Color(0xFFFBBF24),
    amberLight = Color(0xFF3A2A0A),
    coral = Color(0xFFF87171),
    coralLight = Color(0xFF3B1517),
    blue = Color(0xFF38BDF8),
    blueLight = Color(0xFF0C2A3F),
    purple = Color(0xFFA78BFA),
    purpleLight = Color(0xFF2A1B4A)
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

val PrimaryIndigo: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.primary
val PrimaryIndigoDark: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.primaryDark
val PrimaryIndigoLight: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.primaryLight

val AccentGreen: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.green
val AccentGreenLight: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.greenLight
val AccentAmber: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.amber
val AccentAmberLight: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.amberLight
val AccentCoral: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.coral
val AccentCoralLight: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.coralLight
val AccentBlue: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.blue
val AccentBlueLight: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.blueLight
val AccentPurple: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.purple
val AccentPurpleLight: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.purpleLight

/** Nền màn hình (tên cũ giữ lại để không phải đổi code; ở chế độ tối là màu tối). */
val SurfaceLight: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.background
/** Nền thẻ/dialog/thanh điều hướng — dùng thay cho Color.White. */
val CardSurface: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.card
val CardBorderLight: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.cardBorder
val TextPrimary: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.textPrimary
val TextSecondary: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.textSecondary

/** Chữ/icon trên nền màu đậm (nút chính, banner gradient) — luôn trắng ở cả hai chế độ. */
val OnAccent: Color = Color.White
