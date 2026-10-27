package com.example.moodle_usth_mobile_app.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle_usth_mobile_app.data.model.NotificationKind
import com.example.moodle_usth_mobile_app.data.repository.LearningRules
import com.example.moodle_usth_mobile_app.ui.Formatters
import com.example.moodle_usth_mobile_app.ui.MainViewModel
import com.example.moodle_usth_mobile_app.ui.components.AssignmentRow
import com.example.moodle_usth_mobile_app.ui.components.AppProgressBar
import com.example.moodle_usth_mobile_app.ui.components.BadgeItem
import com.example.moodle_usth_mobile_app.ui.components.SubjectIcon
import com.example.moodle_usth_mobile_app.ui.components.courseIcon
import com.example.moodle_usth_mobile_app.ui.components.toneColor
import com.example.moodle_usth_mobile_app.ui.screens.SectionHeadingRow
import com.example.moodle_usth_mobile_app.ui.theme.AccentCoral
import com.example.moodle_usth_mobile_app.ui.theme.SurfaceLight
import com.example.moodle_usth_mobile_app.ui.theme.TextPrimary
import com.example.moodle_usth_mobile_app.ui.theme.TextSecondary

@Composable
fun NotificationsDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val assignments = viewModel.assignments
    val courses = viewModel.courses
    val notifications = viewModel.notifications
    val urgent = LearningRules.pending(assignments).filter { LearningRules.isUrgent(viewModel.getAssignmentStatus(it)) }

    AppModal(title = "Your notifications", onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BadgeItem(text = "${urgent.size} due soon", tone = if (urgent.isEmpty()) "green" else "coral")
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "In-app reminders · synced from Moodle", fontSize = 11.sp, color = TextSecondary)
        }

        SectionHeadingRow(
            title = "Deadline reminders",
            actionText = "All classwork",
            onActionClick = { viewModel.openDialog("classwork") }
        )
        if (urgent.isEmpty()) {
            EmptyState(title = "No urgent work. You're on track.")
        } else {
            urgent.forEach { assignment ->
                AssignmentRow(
                    assignment = assignment,
                    status = viewModel.getAssignmentStatus(assignment),
                    timeRemainingText = viewModel.formatTimeRemaining(assignment),
                    showCourse = true,
                    onClick = { viewModel.openDialog("assignment", assignment.id) }
                )
            }
        }

        SectionHeadingRow(title = "Course progress")
        courses.forEach { course ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceLight)
                    .clickable { viewModel.selectCourse(course) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SubjectIcon(icon = courseIcon(course.id), tone = course.tone)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(text = "${course.shortTitle} · ${course.progress}% complete", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(
                        text = "${course.done}/${course.total} tasks completed · ${course.total - course.done} remaining · ${course.credits} credits",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    AppProgressBar(progress = course.progress / 100f, color = toneColor(course.tone), trackColor = Color(0xFFE2E8F0), height = 5.dp)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
        }

        SectionHeadingRow(title = "From your classroom")
        if (notifications.isEmpty()) {
            EmptyState(title = "Nothing new from your teachers yet.")
        }
        notifications.forEach { notification ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceLight)
                    .clickable {
                        viewModel.markNotificationRead(notification.id)
                        val assignmentId = notification.assignmentId
                        val course = if (notification.courseId != null) viewModel.findCourse(notification.courseId) else null
                        when {
                            assignmentId != null -> viewModel.openDialog("assignment", assignmentId)
                            course != null -> {
                                viewModel.selectCourse(course)
                                viewModel.openDialog("courseInfo", course.id)
                            }
                        }
                    }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SubjectIcon(
                    icon = if (notification.kind == NotificationKind.GRADE) Icons.Default.EmojiEvents else Icons.AutoMirrored.Outlined.Chat,
                    tone = "blue"
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = Formatters.relative(notification.createdAtEpochMs, viewModel.nowMs),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.5.sp
                        )
                        if (!notification.read) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(modifier = Modifier.size(7.dp).background(AccentCoral, CircleShape))
                        }
                    }
                    Text(
                        text = notification.title,
                        fontSize = 13.sp,
                        fontWeight = if (notification.read) FontWeight.SemiBold else FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(text = notification.body, fontSize = 11.sp, color = TextSecondary)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
        }
    }
}
