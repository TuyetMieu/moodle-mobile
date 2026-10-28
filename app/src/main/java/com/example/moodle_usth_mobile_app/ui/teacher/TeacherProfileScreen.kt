package com.example.moodle_usth_mobile_app.ui.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle_usth_mobile_app.R
import com.example.moodle_usth_mobile_app.data.model.TeacherData
import com.example.moodle_usth_mobile_app.ui.MainViewModel
import com.example.moodle_usth_mobile_app.ui.TeacherViewModel
import com.example.moodle_usth_mobile_app.ui.components.SettingsSection
import com.example.moodle_usth_mobile_app.ui.theme.AccentCoral
import com.example.moodle_usth_mobile_app.ui.theme.CardSurface
import com.example.moodle_usth_mobile_app.ui.theme.TextPrimary
import com.example.moodle_usth_mobile_app.ui.theme.TextSecondary

/** Hồ sơ giáo viên + cài đặt (ngôn ngữ, giao diện) + đăng xuất. */
@Composable
fun TeacherProfileScreen(data: TeacherData, viewModel: TeacherViewModel, main: MainViewModel, onLogout: () -> Unit) {
    var confirmLogout by remember { mutableStateOf(false) }
    val profile = data.profile

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TeacherHeader(overline = stringResource(R.string.teacher_profile_overline), title = stringResource(R.string.teacher_profile_title))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardSurface, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(profile.initials, 56)
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(profile.fullName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(profile.cohort, fontSize = 13.sp, color = TextSecondary)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardSurface, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            InfoRow(stringResource(R.string.teacher_staff_code), profile.memberCode)
            InfoRow(stringResource(R.string.teacher_program), profile.programName)
            InfoRow(stringResource(R.string.teacher_semester), "${data.semesterLabel} · ${data.academicYear}")
            InfoRow(stringResource(R.string.teacher_stat_courses), data.courses.joinToString { it.code })
        }

        SettingsSection(viewModel = main, showStudentOptions = false)

        Text(stringResource(R.string.teacher_server, main.serverUrl), fontSize = 12.sp, color = TextSecondary)

        OutlinedButton(onClick = { confirmLogout = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Text(stringResource(R.string.common_logout), color = AccentCoral, fontWeight = FontWeight.Bold)
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text(stringResource(R.string.teacher_logout_title)) },
            text = { Text(stringResource(R.string.teacher_logout_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    onLogout()
                }) { Text(stringResource(R.string.common_logout), color = AccentCoral) }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text(stringResource(R.string.common_cancel)) } }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row {
        Text(label, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}
