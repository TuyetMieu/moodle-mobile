package com.example.moodle_usth_mobile_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle_usth_mobile_app.R
import com.example.moodle_usth_mobile_app.data.model.Assignment
import com.example.moodle_usth_mobile_app.data.model.AssignmentStatus
import com.example.moodle_usth_mobile_app.data.model.Course
import com.example.moodle_usth_mobile_app.ui.AppDestination
import com.example.moodle_usth_mobile_app.ui.MainViewModel
import com.example.moodle_usth_mobile_app.ui.components.AppHeader
import com.example.moodle_usth_mobile_app.ui.components.AssignmentRow
import com.example.moodle_usth_mobile_app.ui.components.CountdownCard
import com.example.moodle_usth_mobile_app.ui.components.CourseCard
import com.example.moodle_usth_mobile_app.ui.components.StudyOverviewCard
import com.example.moodle_usth_mobile_app.ui.theme.AccentCoral
import com.example.moodle_usth_mobile_app.ui.theme.AccentCoralLight
import com.example.moodle_usth_mobile_app.ui.theme.AccentGreen
import com.example.moodle_usth_mobile_app.ui.theme.CardBorderLight
import com.example.moodle_usth_mobile_app.ui.theme.CardSurface
import com.example.moodle_usth_mobile_app.ui.theme.OnAccent
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigo
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigoLight
import com.example.moodle_usth_mobile_app.ui.theme.SurfaceLight
import com.example.moodle_usth_mobile_app.ui.theme.TextPrimary
import com.example.moodle_usth_mobile_app.ui.theme.TextSecondary
import java.util.Locale

/** Số bài tối đa hiện trong khu "Ưu tiên của bạn". */
private const val MAX_PRIORITIES = 5

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    assignments: List<Assignment>,
    courses: List<Course>,
    onCourseClick: (Course) -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = viewModel.profile
    val firstName = profile?.shortName.orEmpty()
    val summary = viewModel.getStudySummary()
    val unreadCount = viewModel.notifications.count { !it.read }

    // Ưu tiên của sinh viên: bài chưa xong, chưa quá hạn, deadline gần nhất đứng đầu.
    val priorities = viewModel.getPriorities()
    val overdue = viewModel.getOverdue()
    val nextAssignment = viewModel.getNextDeadline()
    val nextStatus = if (nextAssignment != null) viewModel.getAssignmentStatus(nextAssignment) else AssignmentStatus.DONE

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceLight)
    ) {
        // Chuông hiện số thông báo chưa đọc (tin ngành + thầy cô & toàn trường).
        AppHeader(
            firstName = firstName,
            initials = profile?.initials.orEmpty(),
            urgentCount = unreadCount,
            onNotificationClick = { viewModel.openDialog("notifications") }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StudyOverviewCard(
                pendingCount = summary.pendingCount,
                urgentCount = summary.urgentCount,
                recentDoneCount = summary.recentDoneCount,
                onFilterClick = { filter -> viewModel.openDialog("classwork", filter) }
            )

            // Thẻ đếm ngược tới deadline gần nhất
            CountdownCard(
                firstName = firstName,
                nextAssignment = nextAssignment,
                status = nextStatus,
                onViewTaskClick = {
                    if (nextAssignment != null) viewModel.openDialog("assignment", nextAssignment.id)
                    else viewModel.openDialog("classwork", "done")
                }
            )

            // Ưu tiên của bạn
            Column {
                SectionHeadingRow(
                    title = stringResource(R.string.home_priorities_title),
                    detailCount = String.format(Locale.US, "%02d", priorities.size),
                    actionText = stringResource(R.string.home_all_assignments),
                    onActionClick = { viewModel.openDialog("classwork", "todo") }
                )
                Text(
                    text = stringResource(R.string.home_priorities_hint),
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (overdue.isNotEmpty()) {
                    OverdueWarning(count = overdue.size, onClick = { viewModel.openDialog("classwork", "urgent") })
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (priorities.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardSurface, RoundedCornerShape(16.dp))
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
                                text = stringResource(R.string.home_priorities_empty),
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        priorities.take(MAX_PRIORITIES).forEachIndexed { index, assignment ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PriorityRank(rank = index + 1)
                                Spacer(modifier = Modifier.width(8.dp))
                                AssignmentRow(
                                    assignment = assignment,
                                    status = viewModel.getAssignmentStatus(assignment),
                                    timeRemainingText = viewModel.formatTimeRemaining(assignment),
                                    showCourse = true,
                                    compact = index > 0, // bài đầu tiên hiện đủ thành phần điểm & % đóng góp
                                    onClick = { viewModel.openDialog("assignment", assignment.id) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Môn học của bạn
            Column {
                SectionHeadingRow(
                    title = stringResource(R.string.home_your_courses),
                    detailCount = String.format(Locale.US, "%02d", courses.size),
                    actionText = stringResource(R.string.home_see_all),
                    onActionClick = { viewModel.navigateTo(AppDestination.COURSES) }
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

            // Lời nhắn cuối trang
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
                    text = stringResource(R.string.home_footer),
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/** Ô số thứ hạng ưu tiên (1 = deadline gần nhất). */
@Composable
private fun PriorityRank(rank: Int) {
    val first = rank == 1
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(if (first) PrimaryIndigo else PrimaryIndigoLight, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = rank.toString(),
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (first) OnAccent else PrimaryIndigo
        )
    }
}

/** Dòng cảnh báo "N bài quá hạn" — bấm để xem danh sách bài cần làm gấp. */
@Composable
private fun OverdueWarning(count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AccentCoralLight, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = AccentCoral, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = pluralStringResource(R.plurals.home_overdue_warning, count, count),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AccentCoral, modifier = Modifier.size(16.dp))
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
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
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
                        .background(CardBorderLight, RoundedCornerShape(10.dp))
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
