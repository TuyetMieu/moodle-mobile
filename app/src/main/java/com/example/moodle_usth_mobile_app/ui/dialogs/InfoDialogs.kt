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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle_usth_mobile_app.R
import com.example.moodle_usth_mobile_app.data.model.MAX_SCORE
import com.example.moodle_usth_mobile_app.data.model.ResourceKind
import com.example.moodle_usth_mobile_app.data.model.SessionKind
import com.example.moodle_usth_mobile_app.data.model.formatGrade
import com.example.moodle_usth_mobile_app.data.model.formatScore
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

/** Overlay "Course information": ảnh, giảng viên, lịch học của môn. */
@Composable
fun CourseInfoDialog(courseId: String, viewModel: MainViewModel, onDismiss: () -> Unit) {
    val courses = viewModel.courses
    val sessions = viewModel.classSessions
    val course = courses.firstOrNull { it.id == courseId } ?: return
    // Tên thứ viết tắt, index 0 = Chủ nhật (giống ClassSession.weekday).
    val weekdays = stringArrayResource(R.array.course_weekdays_short)

    AppModal(title = stringResource(R.string.course_info_title), onDismiss = onDismiss) {
        Image(
            painter = painterResource(courseImageRes(course.imageResName)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(16.dp))
        )
        BadgeItem(text = pluralStringResource(R.plurals.course_code_credits, course.credits, course.code, course.credits), tone = course.tone)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = course.title, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = stringResource(R.string.course_coordinator, course.teacher), fontSize = 13.sp, color = TextSecondary)
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
                            text = "${weekdays.getOrElse(session.weekday) { "" }} · ${session.start}–${session.end}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        val kind = stringResource(
                            if (session.kind == SessionKind.LAB) R.string.course_session_lab else R.string.course_session_lecture
                        )
                        Text(text = "$kind · ${session.room}", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
            TextButton(onClick = {
                viewModel.closeDialog()
                viewModel.navigateTo(AppDestination.TIMETABLE)
            }) {
                Text(text = stringResource(R.string.course_view_timetable), fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(15.dp))
            }
        }
        if (course.resources.isNotEmpty()) {
            Text(text = stringResource(R.string.course_resources_title), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            course.resources.groupBy { it.week }.forEach { (week, items) ->
                Text(
                    text = stringResource(R.string.course_week_label, week),
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
                                text = "${stringResource(resourceLabel(resource.kind))} · ${resource.fileType} · ${resource.sizeLabel}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
        ModalSubtitle(stringResource(R.string.course_resources_note))
    }
}

private fun resourceIcon(kind: ResourceKind): ImageVector = when (kind) {
    ResourceKind.SLIDES -> Icons.Default.Slideshow
    ResourceKind.NOTES -> Icons.Default.Description
    ResourceKind.EXERCISE -> Icons.Default.EditNote
    ResourceKind.DATASET -> Icons.Default.Dataset
}

private fun resourceLabel(kind: ResourceKind): Int = when (kind) {
    ResourceKind.SLIDES -> R.string.course_resource_slides
    ResourceKind.NOTES -> R.string.course_resource_notes
    ResourceKind.EXERCISE -> R.string.course_resource_exercise
    ResourceKind.DATASET -> R.string.course_resource_dataset
}

/** Overlay "Your weekly planner": các deadline chưa xong, sắp theo hạn nộp. */
@Composable
fun PlannerDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val assignments = viewModel.assignments
    val pending = LearningRules.pending(assignments)

    AppModal(title = stringResource(R.string.course_planner_title), onDismiss = onDismiss) {
        ModalSubtitle(stringResource(R.string.course_planner_subtitle))
        if (pending.isEmpty()) EmptyState(title = stringResource(R.string.course_planner_empty))
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
            text = stringResource(R.string.course_planner_tip),
            icon = Icons.Default.CalendarMonth
        )
    }
}

/** Overlay "Your grade report" + nút tải CSV từ backend. */
@Composable
fun GradeReportDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val courses = viewModel.courses
    val semester = viewModel.semester
    val maxText = "/${formatScore(MAX_SCORE)}"

    AppModal(title = stringResource(R.string.course_report_title), onDismiss = onDismiss) {
        BadgeItem(text = if (semester != null) "${semester.label} · ${semester.academicYear}".uppercase() else "", tone = "sky")
        ModalSubtitle(stringResource(R.string.course_report_subtitle))
        courses.forEach { course ->
            val credits = pluralStringResource(R.plurals.course_credits_count, course.credits, course.credits)
            // Môn có bonus: ghi rõ phần bonus đã cộng vào điểm.
            val bonus = course.bonus
            val subtitle = if (course.bonusEnabled && bonus != null && bonus > 0) {
                stringResource(R.string.course_report_row_bonus, credits, course.code, formatScore(bonus))
            } else {
                stringResource(R.string.course_report_row, credits, course.code)
            }
            ReportRow(
                leading = { SubjectIcon(icon = courseIcon(course.id), tone = course.tone) },
                title = course.shortTitle,
                subtitle = subtitle,
                value = course.gradeText,
                unit = maxText
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
            Text(text = stringResource(R.string.course_report_download), fontWeight = FontWeight.Bold)
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
    val left = (current.creditsMilestone - current.creditsEarned).coerceAtLeast(0)

    AppModal(title = stringResource(R.string.course_semester_title), onDismiss = onDismiss) {
        BadgeItem(text = "${current.label} · ${current.academicYear}".uppercase(), tone = "sky")
        ModalSubtitle(stringResource(R.string.course_semester_subtitle))
        ReportRow(
            leading = { LeadingIcon(Icons.Default.School) },
            title = stringResource(R.string.course_credits_earned),
            subtitle = if (left > 0) pluralStringResource(R.plurals.course_credits_to_milestone, left, left)
            else stringResource(R.string.course_milestone_reached),
            value = "${current.creditsEarned}",
            unit = "/${current.creditsMilestone}"
        )
        ReportRow(
            leading = { LeadingIcon(Icons.Default.CheckCircle) },
            title = stringResource(R.string.course_tasks_completed),
            subtitle = pluralStringResource(
                R.plurals.course_tasks_across, courses.size, courses.size,
                pluralStringResource(R.plurals.course_credits_count, current.enrolledCredits, current.enrolledCredits)
            ),
            value = "${summary.totalDone}",
            unit = "/${summary.totalAssignments}"
        )
        val average = current.averageGrade
        ReportRow(
            leading = { LeadingIcon(Icons.Default.EmojiEvents) },
            title = stringResource(R.string.course_average_grade),
            subtitle = stringResource(R.string.course_weighted_by_credits),
            value = if (average != null) formatGrade(average) else "—",
            unit = "/${formatScore(MAX_SCORE)}"
        )
        InlineInfo(
            stringResource(if (viewModel.isDemoMode) R.string.course_semester_demo_note else R.string.course_semester_note)
        )
        Button(
            onClick = { viewModel.openDialog("gradeReport") },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
        ) {
            Text(text = stringResource(R.string.course_view_report), fontWeight = FontWeight.Bold)
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
