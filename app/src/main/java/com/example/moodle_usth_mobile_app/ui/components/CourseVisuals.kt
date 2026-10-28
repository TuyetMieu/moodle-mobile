package com.example.moodle_usth_mobile_app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.moodle_usth_mobile_app.R
import com.example.moodle_usth_mobile_app.ui.theme.AccentAmber
import com.example.moodle_usth_mobile_app.ui.theme.AccentAmberLight
import com.example.moodle_usth_mobile_app.ui.theme.AccentBlue
import com.example.moodle_usth_mobile_app.ui.theme.AccentBlueLight
import com.example.moodle_usth_mobile_app.ui.theme.AccentCoral
import com.example.moodle_usth_mobile_app.ui.theme.AccentCoralLight
import com.example.moodle_usth_mobile_app.ui.theme.AccentGreen
import com.example.moodle_usth_mobile_app.ui.theme.AccentGreenLight
import com.example.moodle_usth_mobile_app.ui.theme.AccentPurple
import com.example.moodle_usth_mobile_app.ui.theme.AccentPurpleLight
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigo
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigoLight

@DrawableRes
fun courseImageRes(imageName: String): Int = when (imageName) {
    "biology" -> R.drawable.biology
    "earth" -> R.drawable.earth
    else -> R.drawable.development
}

/** Màu chính của một "tone" trong thiết kế (blue/green/purple/amber/coral). Đổi theo chế độ sáng/tối. */
@Composable
@ReadOnlyComposable
fun toneColor(tone: String): Color = when (tone) {
    "green" -> AccentGreen
    "purple" -> AccentPurple
    "amber" -> AccentAmber
    "coral" -> AccentCoral
    "sky" -> AccentBlue
    else -> PrimaryIndigo
}

/** Nền nhạt của một tone (ở chế độ tối là nền tối pha màu). */
@Composable
@ReadOnlyComposable
fun toneLightColor(tone: String): Color = when (tone) {
    "green" -> AccentGreenLight
    "purple" -> AccentPurpleLight
    "amber" -> AccentAmberLight
    "coral" -> AccentCoralLight
    "sky" -> AccentBlueLight
    else -> PrimaryIndigoLight
}

/**
 * Tone ổn định cho một thành phần điểm (cùng id luôn cùng màu). Bài bonus → green.
 * Dùng chung cho danh sách bài tập, chi tiết bài và bảng thang điểm.
 */
fun componentTone(componentId: String?): String {
    if (componentId == null) return "green"
    val tones = listOf("blue", "amber", "purple", "sky", "coral")
    return tones[Math.floorMod(componentId.hashCode(), tones.size)]
}

fun courseIcon(courseId: String): ImageVector = when (courseId) {
    "bio" -> Icons.Default.Biotech
    "earth" -> Icons.Default.Public
    else -> Icons.Default.Code
}

/** Ô vuông bo góc chứa icon theo tone, dùng cho danh sách môn học/thông báo. */
@Composable
fun SubjectIcon(
    icon: ImageVector,
    tone: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .background(toneLightColor(tone), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = toneColor(tone), modifier = Modifier.size(size * 0.5f))
    }
}
