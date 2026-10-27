package com.example.moodle_usth_mobile_app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle_usth_mobile_app.data.model.Announcement
import com.example.moodle_usth_mobile_app.data.model.AnnouncementKind
import com.example.moodle_usth_mobile_app.data.model.Assignment
import com.example.moodle_usth_mobile_app.data.model.AssignmentCategory
import com.example.moodle_usth_mobile_app.data.model.AssignmentStatus
import com.example.moodle_usth_mobile_app.data.model.Course
import com.example.moodle_usth_mobile_app.data.repository.LearningRules
import com.example.moodle_usth_mobile_app.ui.Formatters
import com.example.moodle_usth_mobile_app.ui.MainViewModel
import com.example.moodle_usth_mobile_app.ui.components.AssignmentRow
import com.example.moodle_usth_mobile_app.ui.components.BadgeItem
import com.example.moodle_usth_mobile_app.ui.components.GradeWeightsCard
import com.example.moodle_usth_mobile_app.ui.components.ProgressRing
import com.example.moodle_usth_mobile_app.ui.components.courseImageRes
import com.example.moodle_usth_mobile_app.ui.components.toneColor
import com.example.moodle_usth_mobile_app.ui.dialogs.EmptyState
import com.example.moodle_usth_mobile_app.ui.theme.AccentAmber
import com.example.moodle_usth_mobile_app.ui.theme.AccentAmberLight
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigo
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigoLight
import com.example.moodle_usth_mobile_app.ui.theme.SurfaceLight
import com.example.moodle_usth_mobile_app.ui.theme.TextPrimary
import com.example.moodle_usth_mobile_app.ui.theme.TextSecondary

private val COURSE_TABS = listOf("Stream", "Classwork", "Grade Weights")

@Composable
fun CourseDetailScreen(
    course: Course,
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable(course.id) { mutableStateOf("Stream") }
    val assignments = viewModel.assignments
    val courseAssignments = assignments.filter { it.courseId == course.id }
    val pending = LearningRules.pending(assignments, course.id)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceLight)
    ) {
        // Top Compact Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = TextPrimary)
            }
            Text(text = "Course details", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            IconButton(onClick = { viewModel.openDialog("courseInfo", course.id) }) {
                Icon(Icons.Default.MoreHoriz, contentDescription = "Thông tin môn học", tint = TextPrimary)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CourseHero(course)

            // Study Metrics Row
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetricItem(Icons.Default.TaskAlt, "${pending.size}", "to do")
                    MetricItem(Icons.AutoMirrored.Filled.TrendingUp, "${course.done}/${course.total}", "completed")
                    MetricItem(Icons.Default.School, "${course.credits}", "credits")
                }
            }

            // Tabs Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                COURSE_TABS.forEach { tab ->
                    val selected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) Color.White else Color.Transparent)
                            .clickable { selectedTab = tab }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) TextPrimary else TextSecondary
                        )
                    }
                }
            }

            when (selectedTab) {
                "Classwork" -> ClassworkTab(course, courseAssignments, viewModel)
                "Grade Weights" -> GradeWeightsTab(course, viewModel)
                else -> StreamTab(course, pending, viewModel, onViewClasswork = { selectedTab = "Classwork" })
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun CourseHero(course: Course) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(20.dp))
    ) {
        Image(
            painter = painterResource(id = courseImageRes(course.imageResName)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.2f), Color.Black.copy(alpha = 0.85f))))
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${course.code} · ${course.credits} CREDITS",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = course.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TeacherAvatar(course.initials, Color.White.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = course.teacher, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            ProgressRing(progressPercent = course.progress, color = toneColor(course.tone), size = 75.dp)
        }
    }
}

// ------------------------------------------------------------------ Stream

@Composable
private fun StreamTab(
    course: Course,
    pending: List<Assignment>,
    viewModel: MainViewModel,
    onViewClasswork: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeadingRow(
            title = "Your next steps",
            detailCount = pending.size.toString(),
            actionText = "View classwork",
            onActionClick = onViewClasswork
        )
        if (pending.isEmpty()) {
            EmptyState(title = "All caught up in this class.")
        } else {
            pending.take(2).forEach { assignment ->
                AssignmentRow(
                    assignment = assignment,
                    status = viewModel.getAssignmentStatus(assignment),
                    timeRemainingText = viewModel.formatTimeRemaining(assignment),
                    showCourse = false,
                    onClick = { viewModel.openDialog("assignment", assignment.id) }
                )
            }
        }
    }

    course.announcements.forEach { announcement ->
        AnnouncementCard(
            announcement = announcement,
            nowMs = viewModel.nowMs,
            onResourcesClick = { viewModel.openDialog("courseInfo", course.id) }
        )
    }
}

@Composable
private fun AnnouncementCard(announcement: Announcement, nowMs: Long, onResourcesClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TeacherAvatar(announcement.authorInitials, PrimaryIndigo)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = announcement.author, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = Formatters.postedAt(announcement.postedAtEpochMs, nowMs), fontSize = 11.sp, color = TextSecondary)
                }
                if (announcement.pinned) {
                    Icon(Icons.Default.PushPin, contentDescription = "Đã ghim", tint = TextSecondary, modifier = Modifier.size(15.dp))
                }
            }
            if (announcement.kind == AnnouncementKind.ANNOUNCEMENT) {
                BadgeItem(text = "COURSE ANNOUNCEMENT", tone = "sky")
            }
            Text(text = announcement.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = announcement.body, fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
            if (announcement.kind == AnnouncementKind.RESOURCE) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryIndigoLight)
                        .clickable(onClick = onResourcesClick)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "View course resources",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryIndigo,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(16.dp))
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Questions? Ask in your next class.", fontSize = 12.sp, color = TextSecondary)
                }
            }
        }
    }
}

// --------------------------------------------------------------- Classwork

@Composable
private fun ClassworkTab(course: Course, courseAssignments: List<Assignment>, viewModel: MainViewModel) {
    var collapsed by remember(course.id) { mutableStateOf(setOf(AssignmentCategory.ATTENDANCE)) }

    if (course.gradeComponents.isNotEmpty()) GradeWeightsCard()

    SectionHeadingRow(title = "Your classwork", detailCount = String.format("%02d", courseAssignments.size))

    // Thứ tự nhóm theo mức ảnh hưởng: Final project → Mid-term & labs → Attendance.
    AssignmentCategory.entries.sortedByDescending { it.weight }.forEach { category ->
        val items = courseAssignments.filter { it.category == category }
            .sortedWith(compareBy<Assignment> { it.isCompleted }.thenBy { if (it.isCompleted) -it.dueAtEpochMs else it.dueAtEpochMs })
        if (items.isEmpty()) return@forEach
        val isCollapsed = category in collapsed
        val chevronRotation by animateFloatAsState(if (isCollapsed) -90f else 0f, label = "chevron")

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { collapsed = if (isCollapsed) collapsed - category else collapsed + category }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = category.label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                BadgeItem(text = "${category.weight}%", tone = category.tone)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${items.count { it.isCompleted }}/${items.size} done",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = if (isCollapsed) "Mở rộng" else "Thu gọn",
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(chevronRotation)
                )
            }
            AnimatedVisibility(visible = !isCollapsed) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEach { assignment ->
                        val status = viewModel.getAssignmentStatus(assignment)
                        AssignmentRow(
                            assignment = assignment,
                            status = status,
                            timeRemainingText = viewModel.formatTimeRemaining(assignment),
                            showCourse = false,
                            compact = status == AssignmentStatus.DONE,
                            onClick = { viewModel.openDialog("assignment", assignment.id) }
                        )
                    }
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AccentAmberLight, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.TrackChanges, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = buildAnnotatedString {
                append("Focus on your final project.\n")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("It makes the biggest difference.") }
            },
            fontSize = 13.sp,
            color = TextPrimary,
            lineHeight = 18.sp
        )
    }
}

// ----------------------------------------------------------- Grade Weights

@Composable
private fun GradeWeightsTab(course: Course, viewModel: MainViewModel) {
    GradeWeightsCard(detailed = true, components = course.gradeComponents)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(24.dp))
            Text(text = "Every point has a purpose.", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(
                text = buildAnnotatedString {
                    if (course.grade != null) {
                        append("Your current accumulated grade is ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) { append("${course.gradeText}/10") }
                        append(". Keep completing assessments to build your final result.")
                    } else {
                        append("Nothing has been graded yet. Your accumulated grade appears here once the first assessment is marked.")
                    }
                },
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { viewModel.openDialog("gradeReport") }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "See academic overview", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PrimaryIndigo)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// ----------------------------------------------------------------- shared

@Composable
private fun TeacherAvatar(initials: String, background: Color) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(background, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = initials, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MetricItem(icon: ImageVector, value: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(value) }
                append(" $label")
            },
            fontSize = 12.sp,
            color = TextPrimary
        )
    }
}
