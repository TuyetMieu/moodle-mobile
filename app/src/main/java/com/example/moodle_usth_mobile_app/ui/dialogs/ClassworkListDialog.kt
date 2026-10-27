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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle_usth_mobile_app.data.model.Assignment
import com.example.moodle_usth_mobile_app.data.model.AssignmentStatus
import com.example.moodle_usth_mobile_app.data.repository.LearningRules
import com.example.moodle_usth_mobile_app.ui.MainViewModel
import com.example.moodle_usth_mobile_app.ui.components.AssignmentRow
import com.example.moodle_usth_mobile_app.ui.theme.CardBorderLight
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigo
import com.example.moodle_usth_mobile_app.ui.theme.TextPrimary
import com.example.moodle_usth_mobile_app.ui.theme.TextSecondary

private val FILTERS = listOf("todo" to "To do", "urgent" to "Due soon", "done" to "Done")
private const val RECENT_WINDOW_MS = 14 * 24 * 3_600_000L

/**
 * Khớp từ khoá với tên bài hoặc tên môn (không phân biệt hoa/thường). Cố ý không khớp tên thành phần điểm:
 * "Mid-term & labs" khiến từ khoá "lab" trả về cả quiz và bài đọc.
 */
fun matchesSearch(assignment: Assignment, query: String): Boolean {
    val text = query.trim()
    if (text.isEmpty()) return true
    if (assignment.title.contains(text, ignoreCase = true)) return true
    if (assignment.courseTitle.contains(text, ignoreCase = true)) return true
    if (assignment.courseShortTitle.contains(text, ignoreCase = true)) return true
    return false
}

/** Bài có thuộc tab [filter] không: "todo" = chưa xong, "urgent" = sắp hạn/quá hạn, "done" = đã xong. */
private fun matchesFilter(status: AssignmentStatus, filter: String): Boolean {
    if (filter == "done") return status == AssignmentStatus.DONE
    if (filter == "urgent") return LearningRules.isUrgent(status)
    return status != AssignmentStatus.DONE
}

@Composable
fun ClassworkListDialog(
    initialFilter: String = "todo",
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var activeFilter by rememberSaveable(initialFilter) { mutableStateOf(initialFilter) }
    var selectedCourseId by rememberSaveable { mutableStateOf("all") }
    var query by rememberSaveable { mutableStateOf("") }

    val assignments = viewModel.assignments
    val courses = viewModel.courses

    // Bước 1: lọc theo môn và từ khoá.
    val scoped = assignments.filter { assignment ->
        val rightCourse = selectedCourseId == "all" || assignment.courseId == selectedCourseId
        rightCourse && matchesSearch(assignment, query)
    }
    fun matches(assignment: Assignment, filter: String): Boolean =
        matchesFilter(viewModel.getAssignmentStatus(assignment), filter)

    // Bước 2: lọc theo tab. Tab "Done" xếp bài mới xong lên đầu, các tab khác xếp theo hạn nộp.
    val filtered = scoped.filter { matches(it, activeFilter) }
    val visible = if (activeFilter == "done") {
        filtered.sortedByDescending { it.completedAtEpochMs }
    } else {
        filtered.sortedBy { it.dueAtEpochMs }
    }

    AppModal(title = "My classwork", onDismiss = onDismiss) {
        ModalSubtitle("Every class. Every deadline. One clear list.")

        // Search
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search assignments or classes", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Xoá tìm kiếm", tint = TextSecondary)
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryIndigo,
                unfocusedBorderColor = CardBorderLight
            )
        )

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
                subtitle = if (query.isBlank()) "Try another class or status to see more assignments."
                else "No assignments match “${query.trim()}”. Try another keyword, class or status."
            )
        } else if (activeFilter == "done") {
            // Khớp với ô "Recent done" ở Home: tách bài xong trong 14 ngày gần đây và bài cũ hơn.
            val recent = visible.filter { viewModel.nowMs - (it.completedAtEpochMs ?: 0L) <= RECENT_WINDOW_MS }
            val earlier = visible.filter { viewModel.nowMs - (it.completedAtEpochMs ?: 0L) > RECENT_WINDOW_MS }
            if (recent.isNotEmpty()) {
                GroupTitle("GẦN ĐÂY · 14 NGÀY (${recent.size})")
                recent.forEach { assignment -> ClassworkRow(assignment, viewModel) }
            }
            if (earlier.isNotEmpty()) {
                GroupTitle("TRƯỚC ĐÓ (${earlier.size})")
                earlier.forEach { assignment -> ClassworkRow(assignment, viewModel) }
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
private fun GroupTitle(text: String) {
    Text(text = text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 0.5.sp)
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
        onClick = { viewModel.openDialog("assignment", assignment.id) }
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
