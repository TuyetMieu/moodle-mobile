package com.example.moodle_usth_mobile_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle_usth_mobile_app.R
import com.example.moodle_usth_mobile_app.data.model.Course
import com.example.moodle_usth_mobile_app.data.model.MAX_SCORE
import com.example.moodle_usth_mobile_app.data.model.formatGrade
import com.example.moodle_usth_mobile_app.data.model.formatScore
import com.example.moodle_usth_mobile_app.ui.MainViewModel
import com.example.moodle_usth_mobile_app.ui.components.AppProgressBar
import com.example.moodle_usth_mobile_app.ui.components.BadgeItem
import com.example.moodle_usth_mobile_app.ui.components.SubjectIcon
import com.example.moodle_usth_mobile_app.ui.components.courseIcon
import com.example.moodle_usth_mobile_app.ui.components.toneColor
import com.example.moodle_usth_mobile_app.ui.theme.AccentGreen
import com.example.moodle_usth_mobile_app.ui.theme.CardBorderLight
import com.example.moodle_usth_mobile_app.ui.theme.CardSurface
import com.example.moodle_usth_mobile_app.ui.theme.OnAccent
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigo
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigoDark
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigoLight
import com.example.moodle_usth_mobile_app.ui.theme.SurfaceLight
import com.example.moodle_usth_mobile_app.ui.theme.TextPrimary
import com.example.moodle_usth_mobile_app.ui.theme.TextSecondary

/** Điểm trung bình (thang 20) từ mức này trở lên thì khen "Keep it up!". */
private const val GOOD_AVERAGE = 16.0

/** Gradient thẻ tín chỉ: luôn là chàm đậm ở cả hai chế độ để chữ trắng đọc rõ. */
private val CreditsGradient = listOf(Color(0xFF4F46E5), Color(0xFF3730A3))

@Composable
fun TrackerScreen(
    viewModel: MainViewModel,
    courses: List<Course>,
    onCourseClick: (Course) -> Unit,
    modifier: Modifier = Modifier
) {
    val semester = viewModel.semester
    val summary = viewModel.getStudySummary()
    val maxText = "/${formatScore(MAX_SCORE)}"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceLight)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.course_tracker_kicker), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 0.5.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = stringResource(R.string.course_tracker_title), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(22.dp))
                }
            }
            IconButton(
                onClick = { viewModel.openDialog("gradeReport") },
                modifier = Modifier
                    .size(42.dp)
                    .background(CardSurface, CircleShape)
            ) {
                Icon(Icons.Default.Download, contentDescription = stringResource(R.string.course_report_title), tint = PrimaryIndigo)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Semester selector
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardSurface)
                    .clickable { viewModel.openDialog("semester") }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = semester?.title ?: "—", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.ExpandMore, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            }

            // Semester credits card
            val earned = semester?.creditsEarned ?: 0
            val milestone = semester?.creditsMilestone ?: 0
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(CreditsGradient))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            modifier = Modifier
                                .background(OnAccent.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(7.dp).background(Color(0xFF6EE7B7), CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = stringResource(R.string.course_in_progress_badge), color = OnAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.School, contentDescription = null, tint = OnAccent, modifier = Modifier.size(27.dp))
                    }
                    Text(text = stringResource(R.string.course_semester_tagline), color = OnAccent.copy(alpha = 0.85f), fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(text = "$earned", color = OnAccent, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.padding(bottom = 6.dp)) {
                            Text(text = "/ $milestone", color = OnAccent.copy(alpha = 0.9f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(text = stringResource(R.string.course_credits_earned_lower), color = OnAccent.copy(alpha = 0.75f), fontSize = 11.sp)
                        }
                    }
                    AppProgressBar(progress = if (milestone == 0) 0f else earned.toFloat() / milestone, color = OnAccent, trackColor = OnAccent.copy(alpha = 0.25f), height = 8.dp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val left = (milestone - earned).coerceAtLeast(0)
                        Text(
                            text = if (left > 0) pluralStringResource(R.plurals.course_credits_to_milestone, left, left)
                            else stringResource(R.string.course_milestone_reached),
                            color = OnAccent.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = OnAccent, modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Stat cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(modifier = Modifier.weight(1f).fillMaxHeight(), icon = Icons.Default.CheckCircle, tone = "green", label = stringResource(R.string.course_overall_completion)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BigNumber("${summary.overallProgressPercent}", "%")
                        Spacer(modifier = Modifier.width(6.dp))
                        BadgeItem(
                            text = if (summary.urgentCount == 0) stringResource(R.string.course_on_track)
                            else pluralStringResource(R.plurals.course_due_soon_count, summary.urgentCount, summary.urgentCount),
                            tone = if (summary.urgentCount == 0) "green" else "coral"
                        )
                    }
                    AppProgressBar(progress = summary.overallProgressPercent / 100f, color = AccentGreen, trackColor = CardBorderLight, height = 6.dp)
                    Text(
                        text = stringResource(
                            R.string.course_tasks_summary,
                            summary.totalDone,
                            summary.totalAssignments,
                            pluralStringResource(R.plurals.course_courses_count, courses.size, courses.size)
                        ),
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                StatCard(modifier = Modifier.weight(1f).fillMaxHeight(), icon = Icons.Default.EmojiEvents, tone = "blue", label = stringResource(R.string.course_average_grade)) {
                    val average = semester?.averageGrade
                    BigNumber(if (average != null) formatGrade(average) else "—", maxText)
                    BadgeItem(
                        text = stringResource(if (average != null && average >= GOOD_AVERAGE) R.string.course_keep_it_up else R.string.course_you_got_this),
                        tone = "sky"
                    )
                    Text(text = stringResource(R.string.course_weighted_by_credits), fontSize = 11.sp, color = TextSecondary)
                }
            }

            // Subject breakdown
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeadingRow(title = stringResource(R.string.course_subject_breakdown), detailCount = String.format("%02d", courses.size))
                courses.forEach { course -> SubjectCard(course, onClick = { onCourseClick(course) }) }
            }

            // Encouragement
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimaryIndigoLight, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(CardSurface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = stringResource(R.string.course_encourage_title), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = stringResource(R.string.course_encourage_body), fontSize = 12.sp, color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tone: String,
    label: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SubjectIcon(icon = icon, tone = tone, size = 30.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            }
            content()
        }
    }
}

@Composable
private fun BigNumber(value: String, unit: String) {
    Text(
        text = buildAnnotatedString {
            append(value)
            withStyle(SpanStyle(fontSize = 14.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)) { append(unit) }
        },
        fontSize = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        color = TextPrimary
    )
}

@Composable
private fun SubjectCard(course: Course, onClick: () -> Unit) {
    val toneMain = toneColor(course.tone)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SubjectIcon(icon = courseIcon(course.id), tone = course.tone)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = course.shortTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(
                        text = stringResource(
                            R.string.course_report_row,
                            pluralStringResource(R.plurals.course_credits_count, course.credits, course.credits),
                            course.code
                        ),
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) { append("${course.done}") }
                        append(" ")
                        append(pluralStringResource(R.plurals.course_tasks_completed_of, course.total, course.total))
                    },
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = buildAnnotatedString {
                        append(course.gradeText)
                        withStyle(SpanStyle(fontSize = 11.sp, color = TextSecondary)) { append("/${formatScore(MAX_SCORE)}") }
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = toneMain
                )
            }
            // Môn có bonus: ghi chú phần bonus đã cộng.
            val bonus = course.bonus
            if (course.bonusEnabled && bonus != null && bonus > 0) {
                Text(
                    text = stringResource(R.string.course_bonus_included, formatScore(bonus)),
                    fontSize = 11.sp,
                    color = AccentGreen
                )
            }
            AppProgressBar(progress = course.progress / 100f, color = toneMain, trackColor = CardBorderLight, height = 6.dp)
        }
    }
}
