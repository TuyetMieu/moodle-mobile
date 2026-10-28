package com.example.moodle_usth_mobile_app.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle_usth_mobile_app.R
import com.example.moodle_usth_mobile_app.data.model.AssignmentStatus
import com.example.moodle_usth_mobile_app.data.model.MAX_SCORE
import com.example.moodle_usth_mobile_app.data.model.formatGrade
import com.example.moodle_usth_mobile_app.data.model.formatScore
import com.example.moodle_usth_mobile_app.ui.Formatters
import com.example.moodle_usth_mobile_app.ui.MainViewModel
import com.example.moodle_usth_mobile_app.ui.components.BadgeItem
import com.example.moodle_usth_mobile_app.ui.theme.AccentGreen
import com.example.moodle_usth_mobile_app.ui.theme.AccentGreenLight
import com.example.moodle_usth_mobile_app.ui.theme.CardBorderLight
import com.example.moodle_usth_mobile_app.ui.theme.OnAccent
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigo
import com.example.moodle_usth_mobile_app.ui.theme.SurfaceLight
import com.example.moodle_usth_mobile_app.ui.theme.TextPrimary
import com.example.moodle_usth_mobile_app.ui.theme.TextSecondary

@Composable
fun AssignmentDetailDialog(
    assignmentId: String,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val assignments = viewModel.assignments
    val assignment = assignments.firstOrNull { it.id == assignmentId } ?: return
    val course = viewModel.findCourse(assignment.courseId)
    val status = viewModel.getAssignmentStatus(assignment)
    val isDone = status == AssignmentStatus.DONE
    val isSaving = viewModel.savingAssignmentId == assignmentId
    val maxText = formatScore(assignment.maxScore)

    AppModal(title = stringResource(R.string.course_assignment_title), onDismiss = onDismiss) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            BadgeItem(
                text = stringResource(
                    when (status) {
                        AssignmentStatus.DONE -> R.string.course_status_done
                        AssignmentStatus.OVERDUE -> R.string.course_status_overdue
                        AssignmentStatus.URGENT -> R.string.course_status_urgent
                        AssignmentStatus.UPCOMING -> R.string.course_status_upcoming
                    }
                ),
                tone = when (status) {
                    AssignmentStatus.DONE -> "green"
                    AssignmentStatus.OVERDUE, AssignmentStatus.URGENT -> "coral"
                    AssignmentStatus.UPCOMING -> "amber"
                }
            )
            if (assignment.isBonus) BadgeItem(text = stringResource(R.string.common_bonus), tone = "green")
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = assignment.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary, lineHeight = 25.sp)
            Text(text = assignment.courseTitle, fontSize = 13.sp, color = TextSecondary)
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MetaLine(
                Icons.Default.Schedule,
                stringResource(
                    R.string.course_due_line,
                    if (isDone) stringResource(R.string.course_status_done) else viewModel.formatTimeRemaining(assignment),
                    Formatters.dueDate(assignment.dueAtEpochMs)
                )
            )
            val contribution = assignment.contribution
            MetaLine(
                Icons.Outlined.TrackChanges,
                if (assignment.isBonus) stringResource(R.string.common_bonus_up_to, formatScore(assignment.bonusMax ?: 0.0))
                else if (contribution != null) stringResource(R.string.course_contribution_line, formatScore(contribution))
                else assignment.componentName ?: ""
            )
        }

        if (assignment.description.isNotBlank()) {
            Text(text = assignment.description, fontSize = 13.sp, color = TextPrimary, lineHeight = 19.sp)
        }

        // Kết quả chấm (nếu có)
        val score = assignment.score
        if (score != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AccentGreenLight, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.course_your_grade),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = buildAnnotatedString {
                            append(formatGrade(score))
                            withStyle(SpanStyle(fontSize = 12.sp, color = TextSecondary)) { append(" / $maxText") }
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AccentGreen
                    )
                }
                if (assignment.isAutoGraded || assignment.late) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (assignment.isAutoGraded) BadgeItem(text = stringResource(R.string.common_auto_graded), tone = "sky")
                        if (assignment.late) BadgeItem(text = stringResource(R.string.course_late_penalty), tone = "coral")
                    }
                }
                val feedback = assignment.feedback
                if (feedback != null) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = feedback, fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp)
                    }
                }
            }
        } else if (isDone) {
            // Đã nộp, chưa có điểm: máy chấm sẽ chấm sau hạn nộp.
            InlineInfo(text = stringResource(R.string.course_waiting_auto_grade), icon = Icons.Default.Schedule)
        }

        // How this task counts
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorderLight, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.TrackChanges, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(19.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.course_how_it_counts), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            if (assignment.isBonus) {
                val bonusMax = formatScore(assignment.bonusMax ?: course?.bonusMax ?: 0.0)
                ImpactRow(stringResource(R.string.course_component)) {
                    Text(text = stringResource(R.string.common_bonus), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    BadgeItem(text = stringResource(R.string.course_bonus_max, bonusMax), tone = "green")
                }
                Text(
                    text = stringResource(R.string.course_bonus_explain, bonusMax, maxText),
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )
            } else {
                val componentName = assignment.componentName
                val componentWeight = assignment.componentWeight
                if (componentName != null) {
                    ImpactRow(stringResource(R.string.course_component)) {
                        Text(text = componentName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        if (componentWeight != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            BadgeItem(text = stringResource(R.string.course_of_course, componentWeight), tone = "purple")
                        }
                    }
                }
                val contribution = assignment.contribution
                if (contribution != null) {
                    ImpactRow(stringResource(R.string.course_this_assignment)) {
                        val percent = stringResource(R.string.course_percent_value, formatScore(contribution))
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryIndigo)) { append(percent) }
                                append(" ")
                                append(stringResource(R.string.course_of_final_grade))
                            },
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
                if (course != null) {
                    ImpactRow(stringResource(R.string.course_credits_label)) {
                        Icon(Icons.Default.School, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = pluralStringResource(R.plurals.course_credits_count, course.credits, course.credits),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }
                if (contribution != null) {
                    // 20/20 ở bài này đóng góp tối đa contribution% × 20 điểm vào điểm môn (thang 20).
                    val points = formatGrade(contribution / 100.0 * MAX_SCORE)
                    Text(
                        text = stringResource(R.string.course_max_points_explain, maxText, points),
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        val contribution = assignment.contribution
        if (!assignment.isBonus && contribution != null) {
            InlineInfo(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(stringResource(R.string.course_gpa_note_title)) }
                    append(" ")
                    append(stringResource(R.string.course_gpa_note_body, formatScore(contribution)))
                }
            )
        }

        when {
            !isDone -> Button(
                onClick = { viewModel.toggleAssignmentCompleted(assignment.id) },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                ButtonContent(isSaving, Icons.Default.Check, stringResource(R.string.course_mark_done))
            }
            assignment.isGraded -> Button(
                onClick = {},
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                ButtonContent(false, Icons.Default.Check, stringResource(R.string.course_graded_completed))
            }
            else -> OutlinedButton(
                onClick = { viewModel.toggleAssignmentCompleted(assignment.id) },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                ButtonContent(isSaving, Icons.Default.Replay, stringResource(R.string.course_marked_undo))
            }
        }

        Text(
            text = stringResource(R.string.course_mark_note),
            fontSize = 11.sp,
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth()
        )
        TextButton(
            onClick = { viewModel.openDialog("classwork") },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = stringResource(R.string.course_back_to_classwork), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MetaLine(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceLight, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}

@Composable
private fun ImpactRow(label: String, value: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) { value() }
    }
}

@Composable
private fun ButtonContent(isSaving: Boolean, icon: ImageVector, label: String) {
    if (isSaving) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = OnAccent)
    } else {
        Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp))
    }
    Spacer(modifier = Modifier.width(8.dp))
    Text(text = label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
}
