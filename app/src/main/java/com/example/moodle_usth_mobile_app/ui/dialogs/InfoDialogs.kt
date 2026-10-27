package com.example.moodle_usth_mobile_app.ui.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle_usth_mobile_app.data.model.ResourceKind
import com.example.moodle_usth_mobile_app.data.repository.LearningRules
import com.example.moodle_usth_mobile_app.ui.AppDestination
import com.example.moodle_usth_mobile_app.ui.MainViewModel
import com.example.moodle_usth_mobile_app.ui.components.AssignmentRow
import com.example.moodle_usth_mobile_app.ui.components.BadgeItem
import com.example.moodle_usth_mobile_app.ui.components.SubjectIcon
import com.example.moodle_usth_mobile_app.ui.components.courseIcon
import com.example.moodle_usth_mobile_app.ui.components.courseImageRes
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigo
import com.example.moodle_usth_mobile_app.ui.theme.SurfaceLight
import com.example.moodle_usth_mobile_app.ui.theme.TextPrimary
import com.example.moodle_usth_mobile_app.ui.theme.TextSecondary

private val WEEKDAY_LABELS = listOf("CN", "T2", "T3", "T4", "T5", "T6", "T7")

/** Overlay "Course information": ảnh, giảng viên, lịch học của môn. */
@Composable
fun CourseInfoDialog(courseId: String, viewModel: MainViewModel, onDismiss: () -> Unit) {
    val courses = viewModel.courses
    val sessions = viewModel.classSessions
    val course = courses.firstOrNull { it.id == courseId } ?: return

    AppModal(title = "Course information", onDismiss = onDismiss) {
        Image(
            painter = painterResource(courseImageRes(course.imageResName)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(16.dp))
        )
        BadgeItem(text = "${course.code} · ${course.credits} CREDITS", tone = course.tone)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = course.title, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = "Course coordinator: ${course.teacher}", fontSize = 13.sp, color = TextSecondary)
        }
        if (course.summary.isNotBlank()) {
            Text(text = course.summary, fontSize = 13.sp, color = TextPrimary, lineHeight = 19.sp)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceLight, RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            sessions.filter { it.courseId == course.id }.forEach { session ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${WEEKDAY_LABELS[session.weekday]} · ${session.start}–${session.end}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(text = "${session.kind} · ${session.room}", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
            TextButton(onClick = {
                viewModel.closeDialog()
                viewModel.navigateTo(AppDestination.TIMETABLE)
            }) {
                Text(text = "Xem thời khóa biểu", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(15.dp))
            }
        }
        if (course.resources.isNotEmpty()) {
            Text(text = "Course resources", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            course.resources.groupBy { it.week }.forEach { (week, items) ->
                Text(
                    text = "WEEK ${String.format("%02d", week)}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )
                items.forEach { resource ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceLight, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SubjectIcon(icon = resourceIcon(resource.kind), tone = course.tone, size = 36.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = resource.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text(
                                text = "${resource.kind.label} · ${resource.fileType} · ${resource.sizeLabel}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
        ModalSubtitle("Tài liệu được lưu trên USTH Moodle; nộp bài và tải file chính thức tại cổng Moodle của trường.")
    }
}

private fun resourceIcon(kind: ResourceKind): ImageVector = when (kind) {
    ResourceKind.SLIDES -> Icons.Default.Slideshow
    ResourceKind.NOTES -> Icons.Default.Description
    ResourceKind.EXERCISE -> Icons.Default.EditNote
    ResourceKind.DATASET -> Icons.Default.Dataset
}

/** Overlay "Your weekly planner": các deadline chưa xong, sắp theo hạn nộp. */
@Composable
fun PlannerDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val assignments = viewModel.assignments
    val pending = LearningRules.pending(assignments)

    AppModal(title = "Your weekly planner", onDismiss = onDismiss) {
        ModalSubtitle("Upcoming deadlines · shown in your local time")
        if (pending.isEmpty()) EmptyState(title = "No deadlines ahead. Enjoy the breathing room.")
        pending.forEach { assignment ->
            AssignmentRow(
                assignment = assignment,
                status = viewModel.getAssignmentStatus(assignment),
                timeRemainingText = viewModel.formatTimeRemaining(assignment),
                showCourse = true,
                onClick = { viewModel.openDialog("assignment", assignment.id) }
            )
        }
        InlineInfo(
            text = "Plan around the things that count. Start with the highest-impact assignment.",
            icon = Icons.Default.CalendarMonth
        )
    }
}

/** Overlay "Your grade report" + nút tải CSV từ backend. */
@Composable
fun GradeReportDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val courses = viewModel.courses
    val semester = viewModel.semester

    AppModal(title = "Your grade report", onDismiss = onDismiss) {
        BadgeItem(text = if (semester != null) "${semester.label} · ${semester.academicYear}".uppercase() else "", tone = "sky")
        ModalSubtitle("Your accumulated grades so far. Final results may change as remaining assessments are graded.")
        courses.forEach { course ->
            ReportRow(
                leading = { SubjectIcon(icon = courseIcon(course.id), tone = course.tone) },
                title = course.shortTitle,
                subtitle = "${course.credits} credits · ${course.code}",
                value = course.gradeText,
                unit = "/10"
            )
        }
        Button(
            onClick = { viewModel.downloadGradeReport() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
        ) {
            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Download grade report (CSV)", fontWeight = FontWeight.Bold)
        }
    }
}

/** Overlay "Semester overview". */
@Composable
fun SemesterOverviewDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val semester = viewModel.semester
    val courses = viewModel.courses
    val summary = viewModel.getStudySummary()
    val current = semester ?: return

    AppModal(title = "Semester overview", onDismiss = onDismiss) {
        BadgeItem(text = "${current.label} · ${current.academicYear}".uppercase(), tone = "sky")
        ModalSubtitle("Your current semester at a glance.")
        ReportRow(
            leading = { LeadingIcon(Icons.Default.School) },
            title = "Credits earned",
            subtitle = "${(current.creditsMilestone - current.creditsEarned).coerceAtLeast(0)} credits to your next milestone",
            value = "${current.creditsEarned}",
            unit = "/${current.creditsMilestone}"
        )
        ReportRow(
            leading = { LeadingIcon(Icons.Default.CheckCircle) },
            title = "Tasks completed",
            subtitle = "Across your ${courses.size} courses · ${current.enrolledCredits} credits",
            value = "${summary.totalDone}",
            unit = "/${summary.totalAssignments}"
        )
        InlineInfo(
            if (viewModel.isDemoMode) "Chế độ xem thử: đánh dấu hoàn thành chỉ lưu tạm trên máy này."
            else "Đánh dấu hoàn thành được lưu trên máy chủ. Tín chỉ và điểm chỉ thay đổi sau khi có kết quả chính thức."
        )
        Button(
            onClick = { viewModel.openDialog("gradeReport") },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
        ) {
            Text(text = "View grade report", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun LeadingIcon(icon: ImageVector) {
    Icon(icon, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(24.dp))
}

@Composable
private fun ReportRow(
    leading: @Composable () -> Unit,
    title: String,
    subtitle: String,
    value: String,
    unit: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceLight, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading()
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = subtitle, fontSize = 11.sp, color = TextSecondary)
        }
        Text(
            text = buildAnnotatedString {
                append(value)
                withStyle(SpanStyle(fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)) { append(unit) }
            },
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary
        )
    }
}
