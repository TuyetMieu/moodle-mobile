package com.example.moodle_usth_mobile_app.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle_usth_mobile_app.data.model.Assignment
import com.example.moodle_usth_mobile_app.data.model.AssignmentStatus
import com.example.moodle_usth_mobile_app.data.repository.LearningRules
import com.example.moodle_usth_mobile_app.ui.DialogState
import com.example.moodle_usth_mobile_app.ui.MainViewModel
import com.example.moodle_usth_mobile_app.ui.components.AssignmentRow
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigo
import com.example.moodle_usth_mobile_app.ui.theme.TextPrimary
import com.example.moodle_usth_mobile_app.ui.theme.TextSecondary

private val FILTERS = listOf("todo" to "To do", "urgent" to "Due soon", "done" to "Done")
private const val RECENT_WINDOW_MS = 14 * 24 * 3_600_000L

@Composable
fun ClassworkListDialog(
    initialFilter: String = "todo",
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var activeFilter by rememberSaveable(initialFilter) { mutableStateOf(initialFilter) }
    var selectedCourseId by rememberSaveable { mutableStateOf("all") }

    val assignments by viewModel.assignments.collectAsState()
    val courses by viewModel.courses.collectAsState()

    val scoped = assignments.filter { selectedCourseId == "all" || it.courseId == selectedCourseId }
    fun matches(assignment: Assignment, filter: String): Boolean {
        val status = viewModel.getAssignmentStatus(assignment)
        return when (filter) {
            "done" -> status == AssignmentStatus.DONE
            "urgent" -> LearningRules.isUrgent(status)
            else -> status != AssignmentStatus.DONE
        }
    }
    val visible = scoped.filter { matches(it, activeFilter) }.let { list ->
        // "Done" hiện bài mới hoàn thành trước; các tab còn lại theo hạn nộp gần nhất.
        if (activeFilter == "done") list.sortedByDescending { it.completedAtEpochMs } else list.sortedBy { it.dueAtEpochMs }
    }

    AppModal(title = "My classwork", onDismiss = onDismiss) {
        ModalSubtitle("Every class. Every deadline. One clear list.")

        // Status filter tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                .padding(3.dp)
        ) {
            FILTERS.forEach { (id, label) ->
                val selected = activeFilter == id
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) Color.White else Color.Transparent)
                        .clickable { activeFilter = id }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) TextPrimary else TextSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = scoped.count { matches(it, id) }.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) PrimaryIndigo else TextSecondary
                    )
                }
            }
        }

        // Course filters
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CourseChip("All classes", selectedCourseId == "all") { selectedCourseId = "all" }
            courses.forEach { course ->
                CourseChip(course.shortTitle, selectedCourseId == course.id) { selectedCourseId = course.id }
            }
        }

        if (visible.isEmpty()) {
            EmptyState(
                title = when (activeFilter) {
                    "urgent" -> "Nothing urgent. You're on track."
                    "done" -> "Your next small win starts here."
                    else -> "All caught up!"
                },
                subtitle = "Try another class or status to see more assignments."
            )
        } else if (activeFilter == "done") {
            // Khớp với ô "Recent done" ở Home: tách bài xong trong 14 ngày gần đây và bài cũ hơn.
            val (recent, earlier) = visible.partition { viewModel.nowMs - (it.completedAtEpochMs ?: 0L) <= RECENT_WINDOW_MS }
            listOf("Gần đây · 14 ngày" to recent, "Trước đó" to earlier).forEach { (heading, items) ->
                if (items.isEmpty()) return@forEach
                Text(
                    text = "${heading.uppercase()} (${items.size})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )
                items.forEach { assignment -> ClassworkRow(assignment, viewModel) }
            }
        } else {
            visible.forEach { assignment -> ClassworkRow(assignment, viewModel) }
        }

        Text(
            text = "Due soon means within 24 hours, or overdue. Times use your device's local time.",
            fontSize = 11.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun ClassworkRow(assignment: Assignment, viewModel: MainViewModel) {
    val status = viewModel.getAssignmentStatus(assignment)
    AssignmentRow(
        assignment = assignment,
        status = status,
        timeRemainingText = viewModel.formatTimeRemaining(assignment),
        showCourse = true,
        compact = status == AssignmentStatus.DONE,
        onClick = { viewModel.openDialog(DialogState.AssignmentDetail(assignment.id)) }
    )
}

@Composable
private fun CourseChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) PrimaryIndigo else Color(0xFFF1F5F9))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else TextSecondary
        )
    }
}
