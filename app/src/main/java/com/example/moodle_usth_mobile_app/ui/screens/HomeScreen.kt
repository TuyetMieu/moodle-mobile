package com.example.moodle_usth_mobile_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle_usth_mobile_app.data.model.Course
import com.example.moodle_usth_mobile_app.ui.MainViewModel
import com.example.moodle_usth_mobile_app.ui.components.AppHeader
import com.example.moodle_usth_mobile_app.ui.components.AssignmentRow
import com.example.moodle_usth_mobile_app.ui.components.CountdownCard
import com.example.moodle_usth_mobile_app.ui.components.CourseCard
import com.example.moodle_usth_mobile_app.ui.components.StudyOverviewCard
import com.example.moodle_usth_mobile_app.ui.theme.AccentGreen
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigo
import com.example.moodle_usth_mobile_app.ui.theme.SurfaceLight
import com.example.moodle_usth_mobile_app.ui.theme.TextPrimary
import com.example.moodle_usth_mobile_app.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    assignments: List<com.example.moodle_usth_mobile_app.data.model.Assignment>,
    courses: List<Course>,
    onCourseClick: (Course) -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = viewModel.profile
    val firstName = profile?.shortName.orEmpty()
    val summary = viewModel.getStudySummary()
    val pendingAssignments = assignments
        .filter { viewModel.getAssignmentStatus(it) != com.example.moodle_usth_mobile_app.data.model.AssignmentStatus.DONE }
        .sortedBy { it.dueAtEpochMs }

    val nextAssignment = pendingAssignments.firstOrNull()
    val nextStatus = if (nextAssignment != null) viewModel.getAssignmentStatus(nextAssignment)
    else com.example.moodle_usth_mobile_app.data.model.AssignmentStatus.DONE

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceLight)
    ) {
        // App Header
        AppHeader(
            firstName = firstName,
            initials = profile?.initials.orEmpty(),
            urgentCount = summary.urgentCount,
            onNotificationClick = { viewModel.openDialog("notifications") }
        )

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Study Space Overview Card
            StudyOverviewCard(
                pendingCount = summary.pendingCount,
                urgentCount = summary.urgentCount,
                recentDoneCount = summary.recentDoneCount,
                onFilterClick = { filter ->
                    viewModel.openDialog("classwork", filter)
                }
            )

            // Countdown Card
            CountdownCard(
                firstName = firstName,
                nextAssignment = nextAssignment,
                status = nextStatus,
                onViewTaskClick = {
                    if (nextAssignment != null) {
                        viewModel.openDialog("assignment", nextAssignment.id)
                    } else {
                        viewModel.openDialog("classwork", "done")
                    }
                }
            )

            // Up Next Section
            Column {
                SectionHeadingRow(
                    title = "Up next",
                    detailCount = String.format("%02d", pendingAssignments.size),
                    actionText = "All assignments",
                    onActionClick = { viewModel.openDialog("classwork", "todo") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (pendingAssignments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "No assignments left. Enjoy the breathing room.",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        pendingAssignments.take(2).forEach { assignment ->
                            AssignmentRow(
                                assignment = assignment,
                                status = viewModel.getAssignmentStatus(assignment),
                                timeRemainingText = viewModel.formatTimeRemaining(assignment),
                                showCourse = true,
                                onClick = {
                                    viewModel.openDialog("assignment", assignment.id)
                                }
                            )
                        }
                    }
                }
            }

            // Your Courses Section
            Column {
                SectionHeadingRow(
                    title = "Your courses",
                    detailCount = String.format("%02d", courses.size),
                    actionText = "See all",
                    onActionClick = { viewModel.navigateTo(com.example.moodle_usth_mobile_app.ui.AppDestination.COURSES) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    courses.forEach { course ->
                        CourseCard(
                            course = viewModel.getCourseWithCompletion(course),
                            onClick = { onCourseClick(course) }
                        )
                    }
                }
            }

            // Footer note
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = PrimaryIndigo,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "A little progress, every single day.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SectionHeadingRow(
    title: String,
    detailCount: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            if (detailCount != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = detailCount,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }
            }
        }

        if (actionText != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = actionText,
                        fontSize = 13.sp,
                        color = PrimaryIndigo,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
