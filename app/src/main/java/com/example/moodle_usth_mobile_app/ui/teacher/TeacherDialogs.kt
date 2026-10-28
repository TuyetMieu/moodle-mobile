package com.example.moodle_usth_mobile_app.ui.teacher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle_usth_mobile_app.R
import com.example.moodle_usth_mobile_app.data.model.PostAudience
import com.example.moodle_usth_mobile_app.data.model.PostKind
import com.example.moodle_usth_mobile_app.data.model.formatScore
import com.example.moodle_usth_mobile_app.data.repository.AssignmentDraft
import com.example.moodle_usth_mobile_app.data.repository.PostDraft
import com.example.moodle_usth_mobile_app.ui.Formatters
import com.example.moodle_usth_mobile_app.ui.TeacherViewModel
import com.example.moodle_usth_mobile_app.ui.dialogs.AppModal
import com.example.moodle_usth_mobile_app.ui.dialogs.InlineInfo
import com.example.moodle_usth_mobile_app.ui.theme.AccentCoral
import com.example.moodle_usth_mobile_app.ui.theme.CardSurface
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigo
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigoLight
import com.example.moodle_usth_mobile_app.ui.theme.TextPrimary
import com.example.moodle_usth_mobile_app.ui.theme.TextSecondary
import java.util.Calendar
import java.util.TimeZone

private const val DAY_MS = 24 * 3_600_000L

// ------------------------------------------------------------------ form bài tập

/** Tạo bài mới (viewModel.assignmentEditor == "") hoặc sửa bài đang có. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssignmentEditorDialog(viewModel: TeacherViewModel) {
    val course = viewModel.selectedCourse ?: return
    val editingId = viewModel.assignmentEditor?.takeIf { it.isNotEmpty() }
    val existing = editingId?.let { id -> course.assignments.find { it.id == id } }
    val close = { viewModel.assignmentEditor = null }

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    // null = bài bonus
    var componentId by remember { mutableStateOf(if (existing != null) existing.componentId else course.components.firstOrNull()?.id) }
    var weightText by remember { mutableStateOf(existing?.weight?.let { formatScore(it) } ?: "1") }
    // Mặc định: 7 ngày nữa, 23:59.
    var dueAt by remember {
        mutableLongStateOf(existing?.dueAtEpochMs ?: Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 7)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis)
    }

    val weight = weightText.replace(',', '.').toDoubleOrNull()
    val dueChanged = existing == null || dueAt != existing.dueAtEpochMs
    val dueOk = !dueChanged || dueAt > viewModel.nowMs
    val valid = title.isNotBlank() && weight != null && weight >= 0.5 && weight <= 100 && dueOk &&
        (componentId != null || course.bonusEnabled)

    // Ước tính % điểm môn của bài này (server sẽ tính lại chính xác).
    val contribution = run {
        val component = course.components.find { it.id == componentId } ?: return@run null
        val others = course.assignments.filter { !it.isBonus && it.componentId == componentId && it.id != editingId }.sumOf { it.weight }
        val w = weight ?: return@run null
        component.weight * w / (others + w)
    }

    AppModal(
        title = stringResource(if (existing == null) R.string.teacher_new_assignment else R.string.teacher_edit_assignment),
        onDismiss = close
    ) {
        OutlinedTextField(
            value = title, onValueChange = { title = it.take(120) },
            label = { Text(stringResource(R.string.teacher_field_title)) },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = description, onValueChange = { description = it.take(2000) },
            label = { Text(stringResource(R.string.teacher_field_description)) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp)
        )

        Text(stringResource(R.string.teacher_field_component), fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            course.components.forEach { component ->
                ChoiceChip("${component.name} · ${component.weight}%", componentId == component.id) { componentId = component.id }
            }
            if (course.bonusEnabled) {
                ChoiceChip(stringResource(R.string.common_bonus_up_to, formatScore(course.bonusMax)), componentId == null) { componentId = null }
            }
        }

        OutlinedTextField(
            value = weightText,
            onValueChange = { text -> weightText = text.filter { it.isDigit() || it == '.' || it == ',' }.take(5) },
            label = { Text(stringResource(R.string.teacher_field_weight)) },
            supportingText = { Text(stringResource(R.string.teacher_field_weight_help)) },
            isError = weight == null || weight < 0.5 || weight > 100,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        if (contribution != null) {
            InlineInfo(stringResource(R.string.teacher_contribution_preview, formatScore(Math.round(contribution * 10) / 10.0)))
        } else if (componentId == null) {
            InlineInfo(stringResource(R.string.teacher_bonus_task_info, formatScore(course.bonusMax)))
        }

        DateTimeField(label = stringResource(R.string.teacher_field_due), epochMs = dueAt, onChange = { dueAt = it })
        if (!dueOk) Text(stringResource(R.string.error_due_in_past), color = AccentCoral, fontSize = 12.sp)
        if (existing != null && dueChanged) {
            Text(stringResource(R.string.teacher_due_change_notice), color = TextSecondary, fontSize = 12.sp)
        }

        Button(
            onClick = {
                val draft = AssignmentDraft(title, description, componentId, weight ?: 1.0, dueAt)
                viewModel.saveAssignment(course.id, editingId, draft)
            },
            enabled = valid && !viewModel.isSaving,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
        ) {
            Text(stringResource(if (existing == null) R.string.teacher_create else R.string.common_save), fontWeight = FontWeight.Bold)
        }
        if (existing != null) {
            if (existing.graded > 0) {
                Text(stringResource(R.string.error_assignment_graded), fontSize = 12.sp, color = TextSecondary)
            } else {
                ConfirmDeleteButton(
                    question = stringResource(R.string.teacher_delete_assignment_question, existing.title),
                    enabled = !viewModel.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) { viewModel.deleteAssignment(existing.id) }
            }
        }
    }
}

// ------------------------------------------------------------------ form đăng tin

/** Đăng tin cho lớp (lên Stream + thông báo), cho ngành của giáo viên, hoặc cho toàn trường. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PostComposerDialog(viewModel: TeacherViewModel) {
    val data = viewModel.data ?: return
    var audience by remember { mutableStateOf(viewModel.composerAudience ?: PostAudience.COURSE) }
    var courseId by remember { mutableStateOf(viewModel.composerCourseId ?: data.courses.firstOrNull()?.id) }
    var kind by remember { mutableStateOf(PostKind.NOTICE) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var pinned by remember { mutableStateOf(false) }
    var eventAt by remember { mutableLongStateOf(System.currentTimeMillis() + 3 * DAY_MS) }

    // Tin cho lớp là thông báo (không có loại "sự kiện").
    val isEvent = audience != PostAudience.COURSE && kind == PostKind.EVENT
    val valid = title.isNotBlank() && body.isNotBlank() &&
        (audience != PostAudience.COURSE || courseId != null) &&
        (!isEvent || eventAt > viewModel.nowMs)

    AppModal(title = stringResource(R.string.teacher_composer_title), onDismiss = { viewModel.composerAudience = null }) {
        Text(stringResource(R.string.teacher_audience), fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip(stringResource(R.string.teacher_audience_course), audience == PostAudience.COURSE) { audience = PostAudience.COURSE }
            ChoiceChip(
                stringResource(R.string.teacher_audience_program, data.profile.programCode),
                audience == PostAudience.PROGRAM
            ) { audience = PostAudience.PROGRAM }
            ChoiceChip(stringResource(R.string.teacher_audience_all), audience == PostAudience.ALL) { audience = PostAudience.ALL }
        }
        Text(
            text = when (audience) {
                PostAudience.COURSE -> stringResource(R.string.teacher_audience_course_help)
                PostAudience.PROGRAM -> stringResource(R.string.teacher_audience_program_help, data.profile.programName)
                PostAudience.ALL -> stringResource(R.string.teacher_audience_all_help)
            },
            fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp
        )

        if (audience == PostAudience.COURSE) {
            Text(stringResource(R.string.teacher_field_course), fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                data.courses.forEach { course -> ChoiceChip(course.shortTitle, courseId == course.id) { courseId = course.id } }
            }
        } else {
            Text(stringResource(R.string.teacher_field_kind), fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip(stringResource(R.string.teacher_kind_notice), kind == PostKind.NOTICE) { kind = PostKind.NOTICE }
                ChoiceChip(stringResource(R.string.teacher_kind_event), kind == PostKind.EVENT) { kind = PostKind.EVENT }
            }
        }

        OutlinedTextField(
            value = title, onValueChange = { title = it.take(120) },
            label = { Text(stringResource(R.string.teacher_field_title)) },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = body, onValueChange = { body = it.take(2000) },
            label = { Text(stringResource(R.string.teacher_field_body)) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 110.dp)
        )

        if (isEvent) {
            DateTimeField(label = stringResource(R.string.teacher_field_event_at), epochMs = eventAt, onChange = { eventAt = it })
            if (eventAt <= viewModel.nowMs) Text(stringResource(R.string.teacher_event_in_past), color = AccentCoral, fontSize = 12.sp)
            OutlinedTextField(
                value = location, onValueChange = { location = it.take(80) },
                label = { Text(stringResource(R.string.teacher_field_location)) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
        }
        if (audience == PostAudience.COURSE) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.teacher_pin), color = TextPrimary, modifier = Modifier.weight(1f))
                Switch(checked = pinned, onCheckedChange = { pinned = it }, colors = SwitchDefaults.colors(checkedTrackColor = PrimaryIndigo))
            }
        }

        Button(
            onClick = {
                viewModel.post(
                    PostDraft(
                        audience = audience,
                        courseId = courseId,
                        kind = if (audience == PostAudience.COURSE) PostKind.NOTICE else kind,
                        title = title,
                        body = body,
                        eventAtEpochMs = if (isEvent) eventAt else null,
                        location = if (isEvent) location else null,
                        pinned = audience == PostAudience.COURSE && pinned
                    )
                )
            },
            enabled = valid && !viewModel.isSaving,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
        ) {
            Text(stringResource(R.string.teacher_publish), fontWeight = FontWeight.Bold)
        }
    }
}

// ------------------------------------------------------------------ component nhỏ

@Composable
fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) },
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Check, contentDescription = null) }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = CardSurface,
            selectedContainerColor = PrimaryIndigoLight,
            selectedLabelColor = PrimaryIndigo,
            selectedLeadingIconColor = PrimaryIndigo,
            labelColor = TextSecondary
        )
    )
}

/** Nút xoá kèm hộp hỏi lại trước khi xoá. */
@Composable
fun ConfirmDeleteButton(question: String, enabled: Boolean, modifier: Modifier = Modifier, onConfirm: () -> Unit) {
    var asking by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { asking = true }, enabled = enabled, modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Text(stringResource(R.string.common_delete), color = AccentCoral)
    }
    if (asking) {
        AlertDialog(
            onDismissRequest = { asking = false },
            title = { Text(stringResource(R.string.teacher_delete_title)) },
            text = { Text(question) },
            confirmButton = {
                TextButton(onClick = {
                    asking = false
                    onConfirm()
                }) { Text(stringResource(R.string.common_delete), color = AccentCoral) }
            },
            dismissButton = { TextButton(onClick = { asking = false }) { Text(stringResource(R.string.common_cancel)) } }
        )
    }
}

/**
 * Ô chọn ngày giờ: chạm → chọn ngày (DatePicker) → chọn giờ (TimePicker, 24h).
 * DatePicker trả về nửa đêm theo UTC của ngày đã chọn, nên đọc ngày theo UTC rồi ghép giờ theo múi giờ máy.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeField(label: String, epochMs: Long, onChange: (Long) -> Unit) {
    var step by remember { mutableStateOf(0) } // 0 = đóng, 1 = chọn ngày, 2 = chọn giờ
    var pickedDateUtc by remember { mutableLongStateOf(0L) }
    val local = Calendar.getInstance().apply { timeInMillis = epochMs }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = label) { step = 1 },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Event, contentDescription = null, tint = PrimaryIndigo)
        Column(modifier = Modifier
            .weight(1f)
            .padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(label, fontSize = 12.sp, color = TextSecondary)
            Text(Formatters.dateTime(epochMs), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        Text(stringResource(R.string.common_edit), color = PrimaryIndigo, fontWeight = FontWeight.SemiBold)
    }

    if (step == 1) {
        // Ngày đang chọn quy về nửa đêm UTC cho DatePicker.
        val initialUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
        val state = rememberDatePickerState(initialSelectedDateMillis = initialUtc)
        DatePickerDialog(
            onDismissRequest = { step = 0 },
            confirmButton = {
                TextButton(onClick = {
                    pickedDateUtc = state.selectedDateMillis ?: initialUtc
                    step = 2
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = { TextButton(onClick = { step = 0 }) { Text(stringResource(R.string.common_cancel)) } }
        ) {
            DatePicker(state = state)
        }
    }
    if (step == 2) {
        val state = rememberTimePickerState(
            initialHour = local.get(Calendar.HOUR_OF_DAY),
            initialMinute = local.get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { step = 0 },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    val date = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = pickedDateUtc }
                    val result = Calendar.getInstance().apply {
                        clear()
                        set(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH), state.hour, state.minute)
                    }
                    onChange(result.timeInMillis)
                    step = 0
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = { TextButton(onClick = { step = 0 }) { Text(stringResource(R.string.common_cancel)) } }
        )
    }
}
