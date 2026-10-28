package com.example.moodle_usth_mobile_app.ui.teacher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle_usth_mobile_app.R
import com.example.moodle_usth_mobile_app.data.model.MAX_SCORE
import com.example.moodle_usth_mobile_app.data.model.PostAudience
import com.example.moodle_usth_mobile_app.data.model.SchemeComponent
import com.example.moodle_usth_mobile_app.data.model.TeachingAssignment
import com.example.moodle_usth_mobile_app.data.model.TeachingCourse
import com.example.moodle_usth_mobile_app.data.model.formatGrade
import com.example.moodle_usth_mobile_app.data.model.formatScore
import com.example.moodle_usth_mobile_app.data.repository.LearningRules
import com.example.moodle_usth_mobile_app.ui.Formatters
import com.example.moodle_usth_mobile_app.ui.TeacherCourseTab
import com.example.moodle_usth_mobile_app.ui.TeacherViewModel
import com.example.moodle_usth_mobile_app.ui.components.BadgeItem
import com.example.moodle_usth_mobile_app.ui.dialogs.InlineInfo
import com.example.moodle_usth_mobile_app.ui.theme.AccentCoral
import com.example.moodle_usth_mobile_app.ui.theme.AccentGreen
import com.example.moodle_usth_mobile_app.ui.theme.CardBorderLight
import com.example.moodle_usth_mobile_app.ui.theme.CardSurface
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigo
import com.example.moodle_usth_mobile_app.ui.theme.SurfaceLight
import com.example.moodle_usth_mobile_app.ui.theme.TextPrimary
import com.example.moodle_usth_mobile_app.ui.theme.TextSecondary

/** Tone của thành phần điểm theo vị trí (bonus luôn xanh lá). */
private val COMPONENT_TONES = listOf("blue", "amber", "purple", "sky", "coral")

private fun componentTone(course: TeachingCourse, componentId: String?): String {
    if (componentId == null) return "green"
    val index = course.components.indexOfFirst { it.id == componentId }
    return COMPONENT_TONES[(if (index < 0) 0 else index) % COMPONENT_TONES.size]
}

/** Trang một môn của giáo viên: Bài tập / Thang điểm / Sinh viên / Bảng tin. */
@Composable
fun TeacherCourseScreen(course: TeachingCourse, viewModel: TeacherViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.openCourse(null) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = TextPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(course.code, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                Text(course.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        val tabs = listOf(
            TeacherCourseTab.ASSIGNMENTS to stringResource(R.string.teacher_tab_assignments),
            TeacherCourseTab.GRADING to stringResource(R.string.teacher_tab_grading),
            TeacherCourseTab.STUDENTS to stringResource(R.string.teacher_tab_students),
            TeacherCourseTab.STREAM to stringResource(R.string.teacher_tab_stream)
        )
        PrimaryScrollableTabRow(
            selectedTabIndex = tabs.indexOfFirst { it.first == viewModel.courseTab },
            containerColor = SurfaceLight,
            contentColor = PrimaryIndigo,
            edgePadding = 16.dp
        ) {
            tabs.forEach { (tab, label) ->
                Tab(
                    selected = viewModel.courseTab == tab,
                    onClick = { viewModel.courseTab = tab },
                    text = { Text(label, fontWeight = if (viewModel.courseTab == tab) FontWeight.Bold else FontWeight.Medium) },
                    selectedContentColor = PrimaryIndigo,
                    unselectedContentColor = TextSecondary
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (viewModel.courseTab) {
                TeacherCourseTab.ASSIGNMENTS -> assignmentsTab(course, viewModel)
                TeacherCourseTab.GRADING -> item(key = "grading-${course.id}") { GradingEditor(course, viewModel) }
                TeacherCourseTab.STUDENTS -> studentsTab(course)
                TeacherCourseTab.STREAM -> streamTab(course, viewModel)
            }
        }
    }
}

// ------------------------------------------------------------------ Bài tập

private fun LazyListScope.assignmentsTab(course: TeachingCourse, viewModel: TeacherViewModel) {
    item {
        Button(
            onClick = { viewModel.assignmentEditor = "" },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.teacher_new_assignment), fontWeight = FontWeight.Bold)
        }
    }
    item { InlineInfo(stringResource(R.string.teacher_auto_grade_info)) }

    // Nhóm theo thành phần điểm, rồi nhóm bonus.
    val groups = course.components.map { component ->
        Triple(component.id, "${component.name} · ${component.weight}%", course.assignments.filter { !it.isBonus && it.componentId == component.id })
    } + Triple(null, null, course.assignments.filter { it.isBonus })

    for ((componentId, label, items) in groups) {
        if (componentId == null && items.isEmpty() && !course.bonusEnabled) continue
        item(key = "header-${componentId ?: "bonus"}") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                Text(
                    text = label ?: stringResource(R.string.common_bonus_up_to, formatScore(course.bonusMax)),
                    fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f)
                )
                BadgeItem(pluralStringResource(R.plurals.teacher_assignments_count, items.size, items.size), componentTone(course, componentId))
            }
        }
        if (items.isEmpty()) {
            item(key = "empty-${componentId ?: "bonus"}") {
                Text(stringResource(R.string.teacher_group_empty), fontSize = 12.sp, color = TextSecondary)
            }
        }
        items(items, key = { it.id }) { assignment -> TeachingAssignmentCard(course, assignment, viewModel) }
    }
}

@Composable
private fun TeachingAssignmentCard(course: TeachingCourse, assignment: TeachingAssignment, viewModel: TeacherViewModel) {
    val context = LocalContext.current
    val overdue = assignment.dueAtEpochMs <= viewModel.nowMs
    val waiting = assignment.submitted - assignment.graded
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, CardBorderLight)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(assignment.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                val tone = componentTone(course, assignment.componentId)
                if (assignment.isBonus) BadgeItem(stringResource(R.string.common_bonus), tone)
                else BadgeItem(assignment.componentName ?: "", tone)
                if (assignment.contribution != null) {
                    BadgeItem(stringResource(R.string.teacher_contribution, formatScore(assignment.contribution)), "sky")
                }
                if (overdue) BadgeItem(stringResource(R.string.teacher_closed), "coral")
            }
            Text(
                text = stringResource(
                    R.string.teacher_due_line, Formatters.dueDate(assignment.dueAtEpochMs),
                    Formatters.timeLeft(context, LearningRules.timeLeft(assignment.dueAtEpochMs, viewModel.nowMs))
                ),
                fontSize = 12.sp, color = if (overdue) AccentCoral else TextSecondary
            )
            Text(
                text = stringResource(
                    R.string.teacher_submission_stats, assignment.submitted, assignment.students, assignment.graded,
                    assignment.averageScore?.let { stringResource(R.string.common_score_out_of, formatGrade(it), formatScore(MAX_SCORE)) } ?: "—"
                ),
                fontSize = 12.sp, color = TextPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.assignmentEditor = assignment.id }, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.common_edit))
                }
                OutlinedButton(
                    onClick = { viewModel.autoGrade(assignment.id) },
                    enabled = waiting > 0 && !viewModel.isSaving,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.teacher_auto_grade_now, waiting))
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Thang điểm

/** Một dòng đang sửa. key ổn định để Compose không nhầm dòng khi thêm/xoá. */
private data class EditableComponent(val key: Int, val id: String?, val name: String, val weightText: String, val tasks: Int)

@Composable
private fun GradingEditor(course: TeachingCourse, viewModel: TeacherViewModel) {
    // Bản nháp, làm mới mỗi khi server trả thang điểm mới.
    val rows = remember(course.components) {
        mutableStateListOf<EditableComponent>().apply {
            course.components.forEachIndexed { index, c -> add(EditableComponent(index, c.id, c.name, c.weight.toString(), c.tasks)) }
        }
    }
    var nextKey by remember(course.components) { mutableStateOf(course.components.size) }
    var bonusEnabled by remember(course.bonusEnabled) { mutableStateOf(course.bonusEnabled) }
    var bonusMax by remember(course.bonusMax) { mutableStateOf(if (course.bonusMax > 0) course.bonusMax else 1.0) }

    val weights = rows.map { it.weightText.toIntOrNull() }
    val total = weights.sumOf { it ?: 0 }
    val namesOk = rows.all { it.name.isNotBlank() } && rows.map { it.name.trim().lowercase() }.toSet().size == rows.size
    val weightsOk = weights.all { it != null && it in 1..100 }
    val valid = rows.isNotEmpty() && namesOk && weightsOk && total == 100
    val bonusTasks = course.assignments.count { it.isBonus }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InlineInfo(stringResource(R.string.teacher_grading_info))

        rows.forEachIndexed { index, row ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = row.name,
                    onValueChange = { rows[index] = row.copy(name = it.take(40)) },
                    label = { Text(stringResource(R.string.teacher_component_name)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = row.weightText,
                    onValueChange = { text -> rows[index] = row.copy(weightText = text.filter { it.isDigit() }.take(3)) },
                    label = { Text("%") },
                    singleLine = true,
                    isError = row.weightText.toIntOrNull()?.let { it !in 1..100 } ?: true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(76.dp)
                )
                IconButton(onClick = { rows.removeAt(index) }, enabled = row.tasks == 0 && rows.size > 1) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.teacher_remove_component, row.name),
                        tint = if (row.tasks == 0 && rows.size > 1) AccentCoral else CardBorderLight
                    )
                }
            }
            if (row.tasks > 0) {
                Text(
                    pluralStringResource(R.plurals.teacher_component_in_use, row.tasks, row.tasks),
                    fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        OutlinedButton(
            onClick = {
                rows.add(EditableComponent(nextKey, null, "", "", 0))
                nextKey += 1
            },
            enabled = rows.size < 8,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.teacher_add_component))
        }

        Text(
            text = stringResource(R.string.teacher_weight_total, total),
            fontWeight = FontWeight.Bold,
            color = if (total == 100) AccentGreen else AccentCoral
        )
        if (!namesOk) Text(stringResource(R.string.teacher_names_invalid), fontSize = 12.sp, color = AccentCoral)

        // Bonus
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, CardBorderLight)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.teacher_bonus_title), fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(stringResource(R.string.teacher_bonus_desc), fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
                    }
                    Switch(
                        checked = bonusEnabled,
                        onCheckedChange = { bonusEnabled = it },
                        enabled = !(bonusEnabled && bonusTasks > 0), // còn bài bonus thì không tắt được
                        colors = SwitchDefaults.colors(checkedTrackColor = PrimaryIndigo)
                    )
                }
                if (bonusEnabled && bonusTasks > 0) {
                    Text(pluralStringResource(R.plurals.teacher_bonus_in_use, bonusTasks, bonusTasks), fontSize = 11.sp, color = TextSecondary)
                }
                if (bonusEnabled) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.teacher_bonus_max), color = TextPrimary, modifier = Modifier.weight(1f))
                        IconButton(onClick = { bonusMax = (bonusMax - 0.5).coerceAtLeast(0.5) }) {
                            Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.teacher_decrease))
                        }
                        Text("+${formatScore(bonusMax)}", fontWeight = FontWeight.Bold, color = PrimaryIndigo, fontSize = 16.sp)
                        IconButton(onClick = { bonusMax = (bonusMax + 0.5).coerceAtMost(5.0) }) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.teacher_increase))
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                val components = rows.map { SchemeComponent(it.id, it.name.trim(), it.weightText.toInt(), it.tasks) }
                viewModel.saveGrading(course.id, components, bonusEnabled, bonusMax)
            },
            enabled = valid && !viewModel.isSaving,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
        ) {
            Text(stringResource(R.string.teacher_save_grading), fontWeight = FontWeight.Bold)
        }
    }
}

// ------------------------------------------------------------------ Sinh viên

private fun LazyListScope.studentsTab(course: TeachingCourse) {
    item { InlineInfo(stringResource(R.string.teacher_students_info)) }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(stringResource(R.string.teacher_stat_students), course.students.size.toString(), Modifier.weight(1f))
            StatTile(
                stringResource(R.string.teacher_class_average),
                course.averageGrade?.let { "${formatGrade(it)}/20" } ?: "—",
                Modifier.weight(1f)
            )
            StatTile(stringResource(R.string.teacher_stat_missing), course.students.sumOf { it.missing }.toString(), Modifier.weight(1f))
        }
    }
    if (course.students.isEmpty()) {
        item { Text(stringResource(R.string.teacher_no_students), color = TextSecondary, fontSize = 13.sp) }
    }
    items(course.students, key = { it.id }) { student ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardSurface, RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(student.initials, 40)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(student.fullName, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                Text("${student.memberCode} · ${student.programName}", fontSize = 11.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(R.string.teacher_student_progress, student.done, student.total),
                    fontSize = 11.sp, color = TextSecondary
                )
                if (student.missing > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    BadgeItem(pluralStringResource(R.plurals.teacher_missing_count, student.missing, student.missing), "coral")
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = student.grade?.let { formatGrade(it) } ?: "—",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo
                )
                Text("/20", fontSize = 11.sp, color = TextSecondary)
                if (student.bonus != null && student.bonus > 0) {
                    Text(stringResource(R.string.teacher_bonus_short, formatGrade(student.bonus)), fontSize = 11.sp, color = AccentGreen)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Bảng tin

private fun LazyListScope.streamTab(course: TeachingCourse, viewModel: TeacherViewModel) {
    item {
        Button(
            onClick = { viewModel.openComposer(PostAudience.COURSE, course.id) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.teacher_post_to_class), fontWeight = FontWeight.Bold)
        }
    }
    items(course.announcements, key = { it.id }) { announcement ->
        val context = LocalContext.current
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardSurface, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(announcement.title, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                if (announcement.pinned) {
                    Icon(Icons.Default.PushPin, contentDescription = stringResource(R.string.teacher_pinned), tint = TextSecondary, modifier = Modifier.size(15.dp))
                }
            }
            Text(Formatters.postedAt(context, announcement.postedAtEpochMs, viewModel.nowMs), fontSize = 11.sp, color = TextSecondary)
            Text(announcement.body, fontSize = 13.sp, color = TextPrimary, lineHeight = 18.sp)
            Spacer(modifier = Modifier.height(2.dp))
            ConfirmDeleteButton(
                question = stringResource(R.string.teacher_delete_post_question, announcement.title),
                enabled = !viewModel.isSaving
            ) { viewModel.deletePost(announcement.id) }
        }
    }
}
