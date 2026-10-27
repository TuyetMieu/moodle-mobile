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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle_usth_mobile_app.data.model.AssignmentStatus
import com.example.moodle_usth_mobile_app.data.model.formatGrade
import com.example.moodle_usth_mobile_app.ui.Formatters
import com.example.moodle_usth_mobile_app.ui.MainViewModel
import com.example.moodle_usth_mobile_app.ui.components.BadgeItem
import com.example.moodle_usth_mobile_app.ui.theme.AccentGreen
import com.example.moodle_usth_mobile_app.ui.theme.AccentGreenLight
import com.example.moodle_usth_mobile_app.ui.theme.CardBorderLight
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigo
import com.example.moodle_usth_mobile_app.ui.theme.SurfaceLight
import com.example.moodle_usth_mobile_app.ui.theme.TextPrimary
import com.example.moodle_usth_mobile_app.ui.theme.TextSecondary
import java.util.Locale

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

    AppModal(title = "Assignment details", onDismiss = onDismiss) {
        BadgeItem(
            text = when (status) {
                AssignmentStatus.DONE -> "Completed"
                AssignmentStatus.OVERDUE -> "Overdue"
                AssignmentStatus.URGENT -> "Due within 24 hours"
                AssignmentStatus.UPCOMING -> "Upcoming"
            },
            tone = when (status) {
                AssignmentStatus.DONE -> "green"
                AssignmentStatus.OVERDUE, AssignmentStatus.URGENT -> "coral"
                AssignmentStatus.UPCOMING -> "amber"
            }
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = assignment.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary, lineHeight = 25.sp)
            Text(text = assignment.courseTitle, fontSize = 13.sp, color = TextSecondary)
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MetaLine(
                Icons.Default.Schedule,
                "${if (isDone) "Completed" else viewModel.formatTimeRemaining(assignment)} · Due ${Formatters.dueDate(assignment.dueAtEpochMs)}"
            )
            MetaLine(Icons.Outlined.TrackChanges, assignment.weightLabel)
        }

        Text(text = assignment.description, fontSize = 13.sp, color = TextPrimary, lineHeight = 19.sp)

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
                    Text(text = "Your grade", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                    Text(
                        text = buildAnnotatedString {
                            append(formatGrade(score))
                            withStyle(SpanStyle(fontSize = 12.sp, color = TextSecondary)) { append(" / ${assignment.maxScore.toInt()}") }
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AccentGreen
                    )
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
                Text(text = "How this task counts", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            ImpactRow("Component") {
                Text(text = assignment.category.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.width(6.dp))
                BadgeItem(text = "${assignment.category.weight}% of course", tone = assignment.category.tone)
            }
            ImpactRow("This assignment") {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryIndigo)) { append("${assignment.contribution}%") }
                        append(" of the final course grade")
                    },
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
            if (course != null) {
                ImpactRow("Course credits") {
                    Icon(Icons.Default.School, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${course.credits} credits", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
            }
            Text(
                text = "A 10/10 score on this task contributes up to ${String.format(Locale.US, "%.2f", assignment.contribution / 10.0)} points to your course grade out of 10.",
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp
            )
        }

        InlineInfo(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Course weight is not GPA weight. ") }
                append("This task contributes ${assignment.contribution}% to this course. GPA uses final course grades weighted by credits, with the university's official grade conversion.")
            }
        )

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
                ButtonContent(isSaving, Icons.Default.Check, "Mark as done")
            }
            assignment.isGraded -> Button(
                onClick = {},
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                ButtonContent(false, Icons.Default.Check, "Graded · completed")
            }
            else -> OutlinedButton(
                onClick = { viewModel.toggleAssignmentCompleted(assignment.id) },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                ButtonContent(isSaving, Icons.Default.Replay, "Marked as complete · Undo")
            }
        }

        Text(
            text = "Đánh dấu hoàn thành chỉ để theo dõi tiến độ. Hãy nộp bài chính thức trên USTH Moodle.",
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
            Text(text = "Back to all classwork", fontWeight = FontWeight.SemiBold)
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
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
    } else {
        Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp))
    }
    Spacer(modifier = Modifier.width(8.dp))
    Text(text = label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
}
