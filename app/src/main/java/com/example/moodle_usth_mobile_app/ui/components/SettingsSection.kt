package com.example.moodle_usth_mobile_app.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle_usth_mobile_app.R
import com.example.moodle_usth_mobile_app.data.local.AppLanguage
import com.example.moodle_usth_mobile_app.data.local.ReminderLead
import com.example.moodle_usth_mobile_app.data.local.ThemeMode
import com.example.moodle_usth_mobile_app.ui.MainViewModel
import com.example.moodle_usth_mobile_app.ui.theme.AccentCoral
import com.example.moodle_usth_mobile_app.ui.theme.CardBorderLight
import com.example.moodle_usth_mobile_app.ui.theme.CardSurface
import com.example.moodle_usth_mobile_app.ui.theme.OnAccent
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigo
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigoLight
import com.example.moodle_usth_mobile_app.ui.theme.SurfaceLight
import com.example.moodle_usth_mobile_app.ui.theme.TextPrimary
import com.example.moodle_usth_mobile_app.ui.theme.TextSecondary

/**
 * Cài đặt dùng chung cho hồ sơ sinh viên và giáo viên: ngôn ngữ, giao diện sáng/tối.
 * [showStudentOptions] = true thêm nhắc hạn nộp (mốc nhắc tuỳ chọn) và loại thông báo muốn nhận.
 */
@Composable
fun SettingsSection(viewModel: MainViewModel, showStudentOptions: Boolean) {
    val context = LocalContext.current

    SettingsCard {
        SettingsHeader(icon = Icons.Default.Language, title = stringResource(R.string.settings_language))
        Spacer(modifier = Modifier.height(10.dp))
        val languages = listOf(
            AppLanguage.SYSTEM to stringResource(R.string.lang_system),
            AppLanguage.VI to stringResource(R.string.lang_vi),
            AppLanguage.EN to stringResource(R.string.lang_en),
            AppLanguage.FR to stringResource(R.string.lang_fr)
        )
        OptionGrid(
            options = languages,
            selected = viewModel.language,
            onSelect = { language ->
                if (language != viewModel.language) {
                    viewModel.setAppLanguage(language)
                    // Tạo lại Activity để mọi chữ đổi sang ngôn ngữ mới (ViewModel vẫn giữ dữ liệu).
                    context.findActivity()?.recreate()
                }
            }
        )

        HorizontalDivider(color = CardBorderLight, modifier = Modifier.padding(vertical = 14.dp))

        SettingsHeader(icon = Icons.Default.DarkMode, title = stringResource(R.string.settings_theme))
        Spacer(modifier = Modifier.height(10.dp))
        OptionGrid(
            options = listOf(
                ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                ThemeMode.LIGHT to stringResource(R.string.theme_light),
                ThemeMode.DARK to stringResource(R.string.theme_dark)
            ),
            selected = viewModel.themeMode,
            onSelect = { viewModel.setTheme(it) }
        )
    }

    if (showStudentOptions) {
        ReminderSettings(viewModel)
        NotificationSettings(viewModel)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderSettings(viewModel: MainViewModel) {
    val remindersEnabled = viewModel.remindersEnabled
    val deniedText = stringResource(R.string.settings_reminders_denied)

    // Android 13+ phải xin POST_NOTIFICATIONS lúc chạy; máy cũ hơn có sẵn quyền.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.setRemindersOn(true) else viewModel.message = deniedText
    }
    // Đã bật nhắc nhưng thông báo của app đang bị tắt trong Cài đặt → hiện cảnh báo.
    val notificationsBlocked = remindersEnabled && !viewModel.canPostNotifications()

    SettingsCard {
        SwitchRow(
            icon = Icons.Default.NotificationsActive,
            title = stringResource(R.string.settings_reminders),
            description = stringResource(R.string.settings_reminders_desc),
            checked = remindersEnabled,
            onCheckedChange = { checked ->
                if (!checked) {
                    viewModel.setRemindersOn(false)
                } else if (Build.VERSION.SDK_INT >= 33 && !viewModel.canPostNotifications()) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    viewModel.setRemindersOn(true)
                }
            }
        )
        if (remindersEnabled) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.settings_reminder_leads),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ReminderLead.entries.forEach { lead ->
                    val selected = lead in viewModel.reminderLeads
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.toggleReminderLead(lead) },
                        label = { Text(text = leadLabel(lead), fontSize = 12.sp) },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = CardSurface,
                            labelColor = TextPrimary,
                            selectedContainerColor = PrimaryIndigoLight,
                            selectedLabelColor = PrimaryIndigo,
                            selectedLeadingIconColor = PrimaryIndigo
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = CardBorderLight,
                            selectedBorderColor = PrimaryIndigo
                        )
                    )
                }
            }
        }
        if (notificationsBlocked) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.settings_reminders_blocked),
                fontSize = 12.sp,
                color = AccentCoral,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun NotificationSettings(viewModel: MainViewModel) {
    SettingsCard {
        SettingsHeader(icon = Icons.Default.NotificationsNone, title = stringResource(R.string.settings_notify_title))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = stringResource(R.string.settings_notify_desc), fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp)
        Spacer(modifier = Modifier.height(8.dp))
        SwitchRow(
            icon = null,
            title = stringResource(R.string.settings_notify_program),
            description = stringResource(R.string.channel_program_desc),
            checked = viewModel.notifyProgram,
            onCheckedChange = { viewModel.changeNotifyProgram(it) }
        )
        HorizontalDivider(color = CardBorderLight, modifier = Modifier.padding(vertical = 10.dp))
        SwitchRow(
            icon = null,
            title = stringResource(R.string.settings_notify_general),
            description = stringResource(R.string.channel_general_desc),
            checked = viewModel.notifyGeneral,
            onCheckedChange = { viewModel.changeNotifyGeneral(it) }
        )
    }
}

@Composable
private fun leadLabel(lead: ReminderLead): String = stringResource(
    when (lead) {
        ReminderLead.THREE_DAYS -> R.string.lead_THREE_DAYS
        ReminderLead.ONE_DAY -> R.string.lead_ONE_DAY
        ReminderLead.TWELVE_HOURS -> R.string.lead_TWELVE_HOURS
        ReminderLead.SIX_HOURS -> R.string.lead_SIX_HOURS
        ReminderLead.TWO_HOURS -> R.string.lead_TWO_HOURS
        ReminderLead.ONE_HOUR -> R.string.lead_ONE_HOUR
        ReminderLead.THIRTY_MINUTES -> R.string.lead_THIRTY_MINUTES
    }
)

// ------------------------------------------------------------------ khối giao diện nhỏ

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun SettingsHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBox(icon)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

@Composable
private fun IconBox(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(PrimaryIndigoLight, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(20.dp))
    }
}

/** Một dòng có công tắc; cả dòng bấm được (vùng chạm lớn, trình đọc màn hình đọc là công tắc). */
@Composable
private fun SwitchRow(
    icon: ImageVector?,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            IconBox(icon)
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = description, fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = null, // dòng cha đã xử lý bấm
            colors = SwitchDefaults.colors(checkedTrackColor = PrimaryIndigo, checkedThumbColor = OnAccent)
        )
    }
}

/** Nhóm lựa chọn một trong nhiều (ngôn ngữ, giao diện), chia đều chiều ngang. */
@Composable
private fun <T> OptionGrid(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    // Nhiều lựa chọn thì chia 2 hàng cho đủ chỗ chữ dài (VD "Langue du système").
    val rows = if (options.size > 3) options.chunked(2) else listOf(options)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceLight, RoundedCornerShape(14.dp))
            .padding(4.dp)
            .selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { (value, label) ->
                    val isSelected = value == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (isSelected) CardSurface else SurfaceLight)
                            .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(value) })
                            .padding(horizontal = 6.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) PrimaryIndigo else TextSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/** Tìm Activity chứa Context (Context trong Compose có thể bị bọc bởi ContextWrapper). */
private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
