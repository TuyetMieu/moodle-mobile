package com.example.moodle_usth_mobile_app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle_usth_mobile_app.ui.theme.LocalAppColors

@Composable
fun BadgeItem(
    text: String,
    tone: String = "blue",
    modifier: Modifier = Modifier
) {
    val bgColor = toneLightColor(tone)
    // Chế độ sáng: chữ đậm hơn màu tone một bậc để đủ tương phản trên nền nhạt (đặc biệt amber/green).
    // Chế độ tối: nền đã tối nên dùng luôn màu tone (sáng).
    val isDark = LocalAppColors.current.isDark
    val textColor = when {
        isDark -> toneColor(tone)
        tone == "green" -> Color(0xFF047857)
        tone == "amber" -> Color(0xFFB45309)
        tone == "coral" -> Color(0xFFDC2626)
        else -> toneColor(tone)
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
