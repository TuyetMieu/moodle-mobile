package com.example.moodle_usth_mobile_app.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle_usth_mobile_app.R
import com.example.moodle_usth_mobile_app.data.model.AppNotification
import com.example.moodle_usth_mobile_app.data.model.NotificationChannelKind
import com.example.moodle_usth_mobile_app.data.model.NotificationKind
import com.example.moodle_usth_mobile_app.data.repository.LearningRules
import com.example.moodle_usth_mobile_app.ui.Formatters
import com.example.moodle_usth_mobile_app.ui.MainViewModel
import com.example.moodle_usth_mobile_app.ui.components.AssignmentRow
import com.example.moodle_usth_mobile_app.ui.components.BadgeItem
import com.example.moodle_usth_mobile_app.ui.components.SubjectIcon
import com.example.moodle_usth_mobile_app.ui.screens.SectionHeadingRow
import com.example.moodle_usth_mobile_app.ui.theme.AccentCoral
import com.example.moodle_usth_mobile_app.ui.theme.CardSurface
import com.example.moodle_usth_mobile_app.ui.theme.OnAccent
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigo
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigoLight
import com.example.moodle_usth_mobile_app.ui.theme.SurfaceLight
import com.example.moodle_usth_mobile_app.ui.theme.TextPrimary
import com.example.moodle_usth_mobile_app.ui.theme.TextSecondary

/**
 * Thông báo của sinh viên, chia hai luồng:
 * - PROGRAM: tin/sự kiện dành cho ngành của sinh viên.
 * - GENERAL: việc thầy cô làm (điểm, bài mới, đổi hạn nộp, tin của lớp) và tin toàn trường.
 * [initialChannel] = "PROGRAM" | "GENERAL" | "" (mặc định GENERAL).
 */
@Composable
fun NotificationsDialog(
    initialChannel: String,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var channel by remember {
        mutableStateOf(if (initialChannel == "PROGRAM") NotificationChannelKind.PROGRAM else NotificationChannelKind.GENERAL)
    }
    val notifications = viewModel.notifications
    val unreadProgram = notifications.count { !it.read && it.channel == NotificationChannelKind.PROGRAM }
    val unreadGeneral = notifications.count { !it.read && it.channel == NotificationChannelKind.GENERAL }
    val items = notifications.filter { it.channel == channel }.sortedByDescending { it.createdAtEpochMs }
    val unreadHere = if (channel == NotificationChannelKind.PROGRAM) unreadProgram else unreadGeneral

    AppModal(title = stringResource(R.string.notif_ui_title), onDismiss = onDismiss) {
        // Hai tab
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceLight, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ChannelTab(
                label = stringResource(R.string.notif_ui_tab_program),
                unread = unreadProgram,
                selected = channel == NotificationChannelKind.PROGRAM,
                onClick = { channel = NotificationChannelKind.PROGRAM },
                modifier = Modifier.weight(1f)
            )
            ChannelTab(
                label = stringResource(R.string.notif_ui_tab_general),
                unread = unreadGeneral,
                selected = channel == NotificationChannelKind.GENERAL,
                onClick = { channel = NotificationChannelKind.GENERAL },
                modifier = Modifier.weight(1f)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (channel == NotificationChannelKind.PROGRAM) {
                    val program = viewModel.profile?.programName.orEmpty()
                    if (program.isEmpty()) stringResource(R.string.notif_ui_program_hint_generic)
                    else stringResource(R.string.notif_ui_program_hint, program)
                } else stringResource(R.string.notif_ui_general_hint),
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp,
                modifier = Modifier.weight(1f)
            )
            if (unreadHere > 0) {
                TextButton(onClick = { viewModel.markAllNotificationsRead(channel) }) {
                    Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = stringResource(R.string.notif_ui_mark_all_read), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Bài gấp chỉ hiện ở luồng thầy cô (là nhắc hạn nộp của chính sinh viên).
        if (channel == NotificationChannelKind.GENERAL) {
            val urgent = LearningRules.pending(viewModel.assignments).filter { LearningRules.isUrgent(viewModel.getAssignmentStatus(it)) }
            if (urgent.isNotEmpty()) {
                SectionHeadingRow(
                    title = stringResource(R.string.notif_ui_deadlines),
                    actionText = stringResource(R.string.notif_ui_all_classwork),
                    onActionClick = { viewModel.openDialog("classwork") }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BadgeItem(
                        text = pluralStringResource(R.plurals.notif_ui_due_soon, urgent.size, urgent.size),
                        tone = "coral"
                    )
                }
                urgent.forEach { assignment ->
                    AssignmentRow(
                        assignment = assignment,
                        status = viewModel.getAssignmentStatus(assignment),
                        timeRemainingText = viewModel.formatTimeRemaining(assignment),
                        showCourse = true,
                        onClick = { viewModel.openDialog("assignment", assignment.id) }
                    )
                }
                SectionHeadingRow(title = stringResource(R.string.notif_ui_latest))
            }
        }

        if (items.isEmpty()) {
            EmptyState(
                title = stringResource(
                    if (channel == NotificationChannelKind.PROGRAM) R.string.notif_ui_empty_program else R.string.notif_ui_empty_general
                )
            )
        }
        items.forEach { notification ->
            NotificationRow(notification, viewModel)
        }
    }
}

@Composable
private fun ChannelTab(label: String, unread: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) CardSurface else SurfaceLight)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 8.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (selected) PrimaryIndigo else TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 15.sp,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (unread > 0) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .background(AccentCoral, CircleShape)
                    .padding(horizontal = 6.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = if (unread > 99) "99+" else unread.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OnAccent)
            }
        }
    }
}

private fun kindIcon(kind: NotificationKind): ImageVector = when (kind) {
    NotificationKind.GRADE -> Icons.Default.EmojiEvents
    NotificationKind.ANNOUNCEMENT -> Icons.AutoMirrored.Outlined.Chat
    NotificationKind.ASSIGNMENT_NEW -> Icons.AutoMirrored.Filled.NoteAdd
    NotificationKind.DEADLINE_CHANGED -> Icons.Default.EventRepeat
    NotificationKind.EVENT -> Icons.Default.Event
    NotificationKind.NOTICE -> Icons.Default.Campaign
}

private fun kindTone(kind: NotificationKind): String = when (kind) {
    NotificationKind.GRADE -> "green"
    NotificationKind.ANNOUNCEMENT -> "blue"
    NotificationKind.ASSIGNMENT_NEW -> "purple"
    NotificationKind.DEADLINE_CHANGED -> "amber"
    NotificationKind.EVENT -> "sky"
    NotificationKind.NOTICE -> "coral"
}

@Composable
private fun NotificationRow(notification: AppNotification, viewModel: MainViewModel) {
    val context = LocalContext.current
    val text = viewModel.notificationText(notification)
    val isEvent = notification.kind == NotificationKind.EVENT
    // Sự kiện: phần ngày giờ/địa điểm vẽ riêng nên chỉ lấy nội dung gốc.
    val body = if (isEvent) notification.body else text.body
    val opensSomething = notification.assignmentId != null || notification.courseId != null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (notification.read) SurfaceLight else PrimaryIndigoLight)
            .clickable {
                viewModel.markNotificationRead(notification.id)
                val assignmentId = notification.assignmentId
                val course = notification.courseId?.let { viewModel.findCourse(it) }
                when {
                    assignmentId != null && viewModel.findAssignment(assignmentId) != null ->
                        viewModel.openDialog("assignment", assignmentId)
                    course != null -> {
                        viewModel.selectCourse(course)
                        viewModel.openDialog("courseInfo", course.id)
                    }
                }
            }
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        SubjectIcon(icon = kindIcon(notification.kind), tone = kindTone(notification.kind))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val author = notification.author
                Text(
                    text = if (author.isNullOrBlank()) Formatters.relative(context, notification.createdAtEpochMs, viewModel.nowMs)
                    else "$author · ${Formatters.relative(context, notification.createdAtEpochMs, viewModel.nowMs)}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (!notification.read) {
                    val unreadLabel = stringResource(R.string.notif_ui_unread)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(AccentCoral, CircleShape)
                            .semantics { contentDescription = unreadLabel } // trình đọc màn hình đọc "chưa đọc"
                    )
                }
            }
            Text(
                text = text.title,
                fontSize = 13.sp,
                fontWeight = if (notification.read) FontWeight.SemiBold else FontWeight.Bold,
                color = TextPrimary
            )
            val eventAt = notification.eventAtEpochMs
            if (isEvent && eventAt != null) {
                Column(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .background(CardSurface, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Event, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(text = Formatters.dateTime(eventAt), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    val location = notification.location
                    if (!location.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(text = location, fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }
            if (body.isNotBlank()) {
                Text(text = body, fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp)
            }
        }
        if (opensSomething) {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}
