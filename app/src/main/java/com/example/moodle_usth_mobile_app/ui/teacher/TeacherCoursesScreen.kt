package com.example.moodle_usth_mobile_app.ui.teacher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle_usth_mobile_app.R
import com.example.moodle_usth_mobile_app.data.model.MAX_SCORE
import com.example.moodle_usth_mobile_app.data.model.TeacherData
import com.example.moodle_usth_mobile_app.data.model.TeachingCourse
import com.example.moodle_usth_mobile_app.data.model.formatGrade
import com.example.moodle_usth_mobile_app.data.model.formatScore
import com.example.moodle_usth_mobile_app.ui.Formatters
import com.example.moodle_usth_mobile_app.ui.TeacherViewModel
import com.example.moodle_usth_mobile_app.ui.components.BadgeItem
import com.example.moodle_usth_mobile_app.ui.components.courseImageRes
import com.example.moodle_usth_mobile_app.ui.theme.CardBorderLight
import com.example.moodle_usth_mobile_app.ui.theme.CardSurface
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigo
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigoLight
import com.example.moodle_usth_mobile_app.ui.theme.TextPrimary
import com.example.moodle_usth_mobile_app.ui.theme.TextSecondary

/** Danh sách lớp giáo viên đang dạy trong học kỳ. */
@Composable
fun TeacherCoursesScreen(data: TeacherData, viewModel: TeacherViewModel) {
    val courses = data.courses
    val students = courses.sumOf { it.students.size }
    // Bài đã nộp nhưng máy chưa chấm (chưa tới hạn) — giáo viên có thể bấm "Chấm ngay".
    val waiting = courses.sumOf { course -> course.assignments.sumOf { it.submitted - it.graded } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            TeacherHeader(
                overline = "${data.semesterLabel} · ${data.academicYear}",
                title = stringResource(R.string.teacher_hello, data.profile.shortName),
                initials = data.profile.initials
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(stringResource(R.string.teacher_stat_courses), courses.size.toString(), Modifier.weight(1f))
                StatTile(stringResource(R.string.teacher_stat_students), students.toString(), Modifier.weight(1f))
                StatTile(stringResource(R.string.teacher_stat_waiting), waiting.toString(), Modifier.weight(1f))
            }
        }
        item { SectionTitle(stringResource(R.string.teacher_my_courses)) }
        if (courses.isEmpty()) {
            item { Text(stringResource(R.string.teacher_no_courses), color = TextSecondary, fontSize = 13.sp) }
        }
        items(courses, key = { it.id }) { course ->
            TeachingCourseCard(course, viewModel.nowMs) { viewModel.openCourse(course.id) }
        }
    }
}

@Composable
private fun TeachingCourseCard(course: TeachingCourse, nowMs: Long, onClick: () -> Unit) {
    val next = course.assignments.filter { it.dueAtEpochMs > nowMs }.minByOrNull { it.dueAtEpochMs }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, CardBorderLight)
    ) {
        Box {
            Image(
                painter = painterResource(courseImageRes(course.imageResName)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
            )
            // Lớp tối phủ ảnh để chữ trắng đọc được.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(0x66000000))
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.teacher_course_code_credits, course.code, course.credits),
                    color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold
                )
                Text(
                    text = course.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BadgeItem(pluralStringResource(R.plurals.teacher_students_count, course.students.size, course.students.size), "blue")
                BadgeItem(pluralStringResource(R.plurals.teacher_assignments_count, course.assignments.size, course.assignments.size), "purple")
                if (course.bonusEnabled) {
                    BadgeItem(stringResource(R.string.common_bonus_up_to, formatScore(course.bonusMax)), "green")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.teacher_next_deadline), fontSize = 11.sp, color = TextSecondary)
                    Text(
                        text = if (next == null) stringResource(R.string.teacher_no_deadline)
                        else "${next.title} · ${Formatters.dueDate(next.dueAtEpochMs)}",
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.teacher_class_average), fontSize = 11.sp, color = TextSecondary)
                    Text(
                        text = course.averageGrade?.let { stringResource(R.string.common_score_out_of, formatGrade(it), formatScore(MAX_SCORE)) } ?: "—",
                        fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ component dùng chung cho các màn giáo viên

@Composable
fun TeacherHeader(overline: String, title: String, initials: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(overline.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo, letterSpacing = 0.8.sp)
            Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        if (initials != null) Avatar(initials, 44)
    }
}

@Composable
fun Avatar(initials: String, sizeDp: Int) {
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(PrimaryIndigoLight),
        contentAlignment = Alignment.Center
    ) {
        Text(initials, fontWeight = FontWeight.Bold, color = PrimaryIndigo, fontSize = (sizeDp * 0.36).sp)
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(CardSurface, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(label, fontSize = 11.sp, color = TextSecondary, maxLines = 2)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = modifier.padding(top = 6.dp))
}
