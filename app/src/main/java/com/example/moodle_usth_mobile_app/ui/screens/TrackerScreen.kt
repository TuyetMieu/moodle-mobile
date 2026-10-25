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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle_usth_mobile_app.data.model.Course
import com.example.moodle_usth_mobile_app.data.model.formatGrade
import com.example.moodle_usth_mobile_app.ui.DialogState
import com.example.moodle_usth_mobile_app.ui.MainViewModel
import com.example.moodle_usth_mobile_app.ui.components.AppProgressBar
import com.example.moodle_usth_mobile_app.ui.components.BadgeItem
import com.example.moodle_usth_mobile_app.ui.components.SubjectIcon
import com.example.moodle_usth_mobile_app.ui.components.courseIcon
import com.example.moodle_usth_mobile_app.ui.components.toneColor
import com.example.moodle_usth_mobile_app.ui.theme.AccentGreen
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigo
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigoDark
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigoLight
import com.example.moodle_usth_mobile_app.ui.theme.SurfaceLight
import com.example.moodle_usth_mobile_app.ui.theme.TextPrimary
import com.example.moodle_usth_mobile_app.ui.theme.TextSecondary

@Composable
fun TrackerScreen(
    viewModel: MainViewModel,
    courses: List<Course>,
    onCourseClick: (Course) -> Unit,
    modifier: Modifier = Modifier
) {
    val semester by viewModel.semester.collectAsState()
    val summary = viewModel.getStudySummary()

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
                Text(text = "THE BIGGER PICTURE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 0.5.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Your progress", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(22.dp))
                }
            }
            IconButton(
                onClick = { viewModel.openDialog(DialogState.GradeReport) },
                modifier = Modifier
                    .size(42.dp)
                    .background(Color.White, CircleShape)
            ) {
                Icon(Icons.Default.Download, contentDescription = "Báo cáo điểm", tint = PrimaryIndigo)
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
                    .background(Color.White)
                    .clickable { viewModel.openDialog(DialogState.SemesterOverview) }
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
                    .background(Brush.linearGradient(listOf(PrimaryIndigo, PrimaryIndigoDark)))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(7.dp).background(Color(0xFF6EE7B7), CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "IN PROGRESS", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.School, contentDescription = null, tint = Color.White, modifier = Modifier.size(27.dp))
                    }
                    Text(text = "A semester of possibilities.", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(text = "$earned", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.padding(bottom = 6.dp)) {
                            Text(text = "/ $milestone", color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(text = "credits earned", color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
                        }
                    }
                    AppProgressBar(progress = if (milestone == 0) 0f else earned.toFloat() / milestone, color = Color.White, trackColor = Color.White.copy(alpha = 0.25f), height = 8.dp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val left = (milestone - earned).coerceAtLeast(0)
                        Text(
                            text = if (left > 0) "$left credits to your next milestone" else "Milestone reached!",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
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
                StatCard(modifier = Modifier.weight(1f).fillMaxHeight(), icon = Icons.Default.CheckCircle, tone = "green", label = "Overall completion") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BigNumber("${summary.overallProgressPercent}", "%")
                        Spacer(modifier = Modifier.width(6.dp))
                        BadgeItem(text = if (summary.urgentCount == 0) "On track" else "${summary.urgentCount} due soon", tone = if (summary.urgentCount == 0) "green" else "coral")
                    }
                    AppProgressBar(progress = summary.overallProgressPercent / 100f, color = AccentGreen, trackColor = Color(0xFFE2E8F0), height = 6.dp)
                    Text(
                        text = "${summary.totalDone} of ${summary.totalAssignments} tasks · ${courses.size} courses",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                StatCard(modifier = Modifier.weight(1f).fillMaxHeight(), icon = Icons.Default.EmojiEvents, tone = "blue", label = "Average grade") {
                    val average = semester?.averageGrade
                    BigNumber(average?.let { formatGrade(it) } ?: "—", "/10")
                    BadgeItem(text = if ((average ?: 0.0) >= 8.0) "Keep it up!" else "You've got this", tone = "sky")
                    Text(text = "Weighted by credits.", fontSize = 11.sp, color = TextSecondary)
                }
            }

            // Subject breakdown
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeadingRow(title = "Subject breakdown", detailCount = String.format("%02d", courses.size))
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
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = "Look how far you've come.", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = "Every completed task is a step forward.", fontSize = 12.sp, color = TextSecondary)
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SubjectIcon(icon = courseIcon(course.id), tone = course.tone)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = course.shortTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = "${course.credits} credits · ${course.code}", fontSize = 11.sp, color = TextSecondary)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) { append("${course.done}") }
                        append(" / ${course.total} tasks completed")
                    },
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = buildAnnotatedString {
                        append(course.gradeText)
                        withStyle(SpanStyle(fontSize = 11.sp, color = TextSecondary)) { append("/10") }
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = toneColor(course.tone)
                )
            }
            AppProgressBar(progress = course.progress / 100f, color = toneColor(course.tone), trackColor = Color(0xFFE2E8F0), height = 6.dp)
        }
    }
}
