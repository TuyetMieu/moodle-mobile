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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle_usth_mobile_app.R
import com.example.moodle_usth_mobile_app.data.model.Announcement
import com.example.moodle_usth_mobile_app.data.model.AnnouncementKind
import com.example.moodle_usth_mobile_app.data.model.Assignment
import com.example.moodle_usth_mobile_app.data.model.AssignmentStatus
import com.example.moodle_usth_mobile_app.data.model.Course
import com.example.moodle_usth_mobile_app.data.model.MAX_SCORE
import com.example.moodle_usth_mobile_app.data.model.formatScore
import com.example.moodle_usth_mobile_app.data.repository.LearningRules
import com.example.moodle_usth_mobile_app.ui.Formatters
import com.example.moodle_usth_mobile_app.ui.MainViewModel
import com.example.moodle_usth_mobile_app.ui.components.AssignmentRow
import com.example.moodle_usth_mobile_app.ui.components.BadgeItem
import com.example.moodle_usth_mobile_app.ui.components.GradeWeightsCard
import com.example.moodle_usth_mobile_app.ui.components.ProgressRing
import com.example.moodle_usth_mobile_app.ui.components.courseImageRes
import com.example.moodle_usth_mobile_app.ui.components.toneColor
import com.example.moodle_usth_mobile_app.ui.components.weightTone
import com.example.moodle_usth_mobile_app.ui.dialogs.EmptyState
import com.example.moodle_usth_mobile_app.ui.theme.AccentAmber
import com.example.moodle_usth_mobile_app.ui.theme.AccentAmberLight
import com.example.moodle_usth_mobile_app.ui.theme.CardBorderLight
import com.example.moodle_usth_mobile_app.ui.theme.CardSurface
import com.example.moodle_usth_mobile_app.ui.theme.OnAccent
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigo
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigoLight
import com.example.moodle_usth_mobile_app.ui.theme.SurfaceLight
import com.example.moodle_usth_mobile_app.ui.theme.TextPrimary
import com.example.moodle_usth_mobile_app.ui.theme.TextSecondary

/** 3 tab của trang môn học: id → tên hiển thị. */
private val COURSE_TABS = listOf(
    "stream" to R.string.course_tab_stream,
    "classwork" to R.string.course_tab_classwork,
    "grades" to R.string.course_tab_grades
)

/** Khoá thu gọn của nhóm bài bonus (các nhóm khác dùng id thành phần điểm). */
private const val BONUS_GROUP = "__bonus"
private const val OTHER_GROUP = "__other"

@Composable
fun CourseDetailScreen(
    course: Course,
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable(course.id) { mutableStateOf("stream") }
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = TextPrimary)
            }
            Text(text = stringResource(R.string.course_details_title), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            IconButton(onClick = { viewModel.openDialog("courseInfo", course.id) }) {
                Icon(Icons.Default.MoreHoriz, contentDescription = stringResource(R.string.course_info_title), tint = TextPrimary)
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
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetricItem(Icons.Default.TaskAlt, "${pending.size}", stringResource(R.string.course_metric_todo))
                    MetricItem(Icons.AutoMirrored.Filled.TrendingUp, "${course.done}/${course.total}", stringResource(R.string.course_metric_completed))
                    MetricItem(Icons.Default.School, "${course.credits}", pluralStringResource(R.plurals.course_metric_credits, course.credits))
                }
            }

            // Tabs Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBorderLight, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                COURSE_TABS.forEach { (tab, label) ->
                    val selected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) CardSurface else Color.Transparent)
                            .clickable { selectedTab = tab }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(label),
                            maxLines = 1,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) TextPrimary else TextSecondary
                        )
                    }
                }
            }

            when (selectedTab) {
                "classwork" -> ClassworkTab(course, courseAssignments, viewModel)
                "grades" -> GradeWeightsTab(course, viewModel)
                else -> StreamTab(course, pending, viewModel, onViewClasswork = { selectedTab = "classwork" })
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
                    text = pluralStringResource(R.plurals.course_code_credits, course.credits, course.code, course.credits),
                    color = OnAccent.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = course.title, color = OnAccent, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TeacherAvatar(course.initials, OnAccent.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = course.teacher, color = OnAccent.copy(alpha = 0.9f), fontSize = 13.sp)
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
            title = stringResource(R.string.course_next_steps),
            detailCount = pending.size.toString(),
            actionText = stringResource(R.string.course_view_classwork),
            onActionClick = onViewClasswork
        )
        if (pending.isEmpty()) {
            EmptyState(title = stringResource(R.string.course_all_caught_up))
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
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TeacherAvatar(announcement.authorInitials, PrimaryIndigo)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = announcement.author, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = Formatters.postedAt(context, announcement.postedAtEpochMs, nowMs), fontSize = 11.sp, color = TextSecondary)
                }
                if (announcement.pinned) {
                    Icon(Icons.Default.PushPin, contentDescription = stringResource(R.string.course_pinned), tint = TextSecondary, modifier = Modifier.size(15.dp))
                }
            }
            if (announcement.kind == AnnouncementKind.ANNOUNCEMENT) {
                BadgeItem(text = stringResource(R.string.course_announcement_badge), tone = "sky")
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
                        text = stringResource(R.string.course_view_resources),
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
                    Text(text = stringResource(R.string.course_ask_in_class), fontSize = 12.sp, color = TextSecondary)
                }
            }
        }
    }
}

// --------------------------------------------------------------- Classwork

/** Một nhóm bài trong tab Classwork: một thành phần điểm, nhóm bonus hoặc nhóm "khác". */
private class ClassworkGroup(
    val key: String,
    val title: String,
    val badge: String,
    val tone: String,
    val items: List<Assignment>
)

/** Bài chưa xong lên trước (hạn gần trước); bài đã xong xuống dưới (mới xong trước). */
private fun sortForClasswork(list: List<Assignment>): List<Assignment> =
    list.sortedWith(compareBy<Assignment> { it.isCompleted }.thenBy { if (it.isCompleted) -it.dueAtEpochMs else it.dueAtEpochMs })

/** Tên thành phần có phải điểm danh/chuyên cần không (theo cả tiếng Anh, Việt, Pháp). */
private fun isAttendance(name: String): Boolean {
    val lower = name.lowercase()
    return listOf("attend", "điểm danh", "chuyên cần", "présence", "assiduité").any { lower.contains(it) }
}

@Composable
private fun ClassworkTab(course: Course, courseAssignments: List<Assignment>, viewModel: MainViewModel) {
    // Nhóm theo thành phần điểm của môn, đúng thứ tự giáo viên đặt; bài bonus là một nhóm riêng ở cuối.
    val groups = mutableListOf<ClassworkGroup>()
    course.gradeComponents.forEachIndexed { index, component ->
        val items = courseAssignments.filter { !it.isBonus && it.componentId == component.id }
        if (items.isNotEmpty()) {
            groups.add(
                ClassworkGroup(
                    key = component.id,
                    title = component.name,
                    badge = stringResource(R.string.course_weight_percent, component.weight),
                    tone = weightTone(index),
                    items = sortForClasswork(items)
                )
            )
        }
    }
    // Bài thuộc thành phần không còn trong thang điểm (dữ liệu cũ) → nhóm "Khác".
    val knownIds = course.gradeComponents.map { it.id }.toSet()
    val others = courseAssignments.filter { !it.isBonus && it.componentId !in knownIds }
    if (others.isNotEmpty()) {
        groups.add(ClassworkGroup(OTHER_GROUP, stringResource(R.string.course_group_other), "", "sky", sortForClasswork(others)))
    }
    val bonusItems = courseAssignments.filter { it.isBonus }
    if (bonusItems.isNotEmpty()) {
        groups.add(
            ClassworkGroup(
                key = BONUS_GROUP,
                title = stringResource(R.string.common_bonus),
                badge = stringResource(R.string.course_bonus_max, formatScore(course.bonusMax)),
                tone = "green",
                items = sortForClasswork(bonusItems)
            )
        )
    }

    // Mặc định thu gọn nhóm điểm danh và nhóm đã xong hết, để bài cần làm nổi lên trước.
    var collapsed by remember(course.id) {
        mutableStateOf(groups.filter { group -> isAttendance(group.title) || group.items.all { it.isCompleted } }.map { it.key }.toSet())
    }

    if (course.gradeComponents.isNotEmpty()) GradeWeightsCard(course = course)

    SectionHeadingRow(title = stringResource(R.string.course_your_classwork), detailCount = String.format("%02d", courseAssignments.size))

    groups.forEach { group ->
        val isCollapsed = group.key in collapsed
        val chevronRotation by animateFloatAsState(if (isCollapsed) -90f else 0f, label = "chevron")

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { collapsed = if (isCollapsed) collapsed - group.key else collapsed + group.key }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = group.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                if (group.badge.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    BadgeItem(text = group.badge, tone = group.tone)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.course_done_count, group.items.count { it.isCompleted }, group.items.size),
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = stringResource(if (isCollapsed) R.string.course_expand else R.string.course_collapse),
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(chevronRotation)
                )
            }
            AnimatedVisibility(visible = !isCollapsed) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    group.items.forEach { assignment ->
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

    // Gợi ý: thành phần có trọng số lớn nhất là nơi đáng dồn sức nhất.
    val heaviest = course.gradeComponents.maxByOrNull { it.weight }
    if (heaviest != null) {
        val title = stringResource(R.string.course_focus_title, heaviest.name)
        val body = stringResource(R.string.course_focus_body, heaviest.weight)
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
                    append(title)
                    append("\n")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(body) }
                },
                fontSize = 13.sp,
                color = TextPrimary,
                lineHeight = 18.sp
            )
        }
    }
}

// ----------------------------------------------------------- Grade Weights

@Composable
private fun GradeWeightsTab(course: Course, viewModel: MainViewModel) {
    GradeWeightsCard(course = course, detailed = true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(24.dp))
            Text(text = stringResource(R.string.course_grade_purpose), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            val gradeValue = stringResource(R.string.common_score_out_of, course.gradeText, formatScore(MAX_SCORE))
            Text(
                text = if (course.grade != null) {
                    boldPart(stringResource(R.string.course_grade_current, gradeValue), gradeValue, TextPrimary)
                } else {
                    AnnotatedString(stringResource(R.string.course_grade_none))
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
                Text(text = stringResource(R.string.course_see_overview), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PrimaryIndigo)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// ----------------------------------------------------------------- shared

/** In đậm [part] bên trong câu [full] (câu đã dịch, vị trí phần in đậm tuỳ ngôn ngữ). */
private fun boldPart(full: String, part: String, color: Color): AnnotatedString = buildAnnotatedString {
    val start = full.indexOf(part)
    if (start < 0) {
        append(full)
        return@buildAnnotatedString
    }
    append(full.substring(0, start))
    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = color)) { append(part) }
    append(full.substring(start + part.length))
}

@Composable
private fun TeacherAvatar(initials: String, background: Color) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(background, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = initials, color = OnAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
