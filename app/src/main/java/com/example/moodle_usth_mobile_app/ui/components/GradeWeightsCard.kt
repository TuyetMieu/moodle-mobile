package com.example.moodle_usth_mobile_app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle_usth_mobile_app.R
import com.example.moodle_usth_mobile_app.data.model.Course
import com.example.moodle_usth_mobile_app.data.model.GradeComponent
import com.example.moodle_usth_mobile_app.data.model.GradeStatus
import com.example.moodle_usth_mobile_app.data.model.MAX_SCORE
import com.example.moodle_usth_mobile_app.data.model.formatGrade
import com.example.moodle_usth_mobile_app.data.model.formatScore
import com.example.moodle_usth_mobile_app.ui.theme.CardBorderLight
import com.example.moodle_usth_mobile_app.ui.theme.CardSurface
import com.example.moodle_usth_mobile_app.ui.theme.TextPrimary
import com.example.moodle_usth_mobile_app.ui.theme.TextSecondary

/** Bảng màu cho các thành phần điểm, theo thứ tự giáo viên đặt (thành phần thứ 7 trở đi lặp lại màu). */
private val WEIGHT_TONES = listOf("blue", "amber", "purple", "sky", "coral", "green")

/** Tone của thành phần điểm thứ [index] trong môn. */
fun weightTone(index: Int): String = WEIGHT_TONES[index.coerceAtLeast(0) % WEIGHT_TONES.size]

/**
 * Thẻ "Know what counts": các thành phần điểm do giáo viên đặt (tên, trọng số %), thanh tỉ lệ theo trọng số.
 * [detailed] = true: thêm điểm từng thành phần (thang 20), dòng bonus (nếu môn bật bonus) và điểm môn.
 */
@Composable
fun GradeWeightsCard(
    course: Course,
    detailed: Boolean = false,
    modifier: Modifier = Modifier
) {
    val components = course.gradeComponents
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.course_weights_title),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            if (detailed) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (course.bonusEnabled) {
                        stringResource(R.string.course_weights_intro_bonus, formatScore(course.bonusMax))
                    } else {
                        stringResource(R.string.course_weights_intro)
                    },
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Thanh tỉ lệ: mỗi đoạn dài theo trọng số của thành phần.
            if (components.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    components.forEachIndexed { index, component ->
                        if (index > 0) Spacer(modifier = Modifier.width(2.dp))
                        Box(
                            modifier = Modifier
                                .weight(component.weight.coerceAtLeast(1).toFloat())
                                .height(8.dp)
                                .background(toneColor(weightTone(index)))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Chú thích: tối đa 3 mục một hàng để tên không bị cắt quá ngắn.
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    components.withIndex().chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            row.forEach { (index, component) ->
                                WeightLegendItem(
                                    percent = stringResource(R.string.course_weight_percent, component.weight),
                                    label = component.name,
                                    color = toneColor(weightTone(index)),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Giữ độ rộng cột đều nhau ở hàng cuối.
                            repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
                }
            }

            if (detailed) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    components.forEachIndexed { index, component ->
                        val (text, tone) = componentStatus(component)
                        WeightDetailRow(
                            label = stringResource(R.string.course_component_with_weight, component.name, component.weight),
                            dotColor = toneColor(weightTone(index)),
                            statusText = text,
                            tone = tone
                        )
                    }

                    if (course.bonusEnabled) {
                        val bonus = course.bonus ?: 0.0
                        WeightDetailRow(
                            label = stringResource(R.string.common_bonus),
                            dotColor = toneColor("green"),
                            statusText = stringResource(R.string.course_bonus_status, formatScore(bonus), formatScore(course.bonusMax)),
                            tone = if (bonus > 0) "green" else "sky"
                        )
                    }

                    HorizontalDivider(color = CardBorderLight, modifier = Modifier.padding(vertical = 4.dp))

                    // Dòng tổng: TB có trọng số + bonus = điểm môn (tối đa 20).
                    val grade = course.grade
                    val base = course.baseGrade
                    val total = if (grade != null) stringResource(R.string.common_score_out_of, formatGrade(grade), formatScore(MAX_SCORE))
                    else stringResource(R.string.course_not_graded)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.course_grade_total),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (course.bonusEnabled && base != null) {
                                Text(
                                    text = stringResource(
                                        R.string.course_grade_formula,
                                        formatGrade(base),
                                        formatScore(course.bonus ?: 0.0),
                                        formatScore(MAX_SCORE)
                                    ),
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        BadgeItem(text = total, tone = if (grade != null) "blue" else "purple")
                    }
                }
            }
        }
    }
}

/** Chữ và tone của trạng thái chấm một thành phần. */
@Composable
private fun componentStatus(component: GradeComponent): Pair<String, String> {
    val score = component.score
    val max = formatScore(MAX_SCORE)
    return when {
        component.status == GradeStatus.GRADED && score != null ->
            stringResource(R.string.common_score_out_of, formatGrade(score), max) to "green"
        component.status == GradeStatus.IN_PROGRESS && score != null ->
            stringResource(R.string.course_component_in_progress, formatGrade(score), max, component.gradedShare) to "amber"
        else -> stringResource(R.string.course_not_graded) to "purple"
    }
}

@Composable
private fun WeightLegendItem(percent: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = percent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WeightDetailRow(label: String, dotColor: Color, statusText: String, tone: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dotColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, fontSize = 13.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(8.dp))
        BadgeItem(text = statusText, tone = tone)
    }
}
