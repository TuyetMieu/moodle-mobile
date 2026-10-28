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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle_usth_mobile_app.R
import com.example.moodle_usth_mobile_app.ui.AppDestination
import com.example.moodle_usth_mobile_app.ui.Formatters
import com.example.moodle_usth_mobile_app.ui.MainViewModel
import com.example.moodle_usth_mobile_app.ui.components.BadgeItem
import com.example.moodle_usth_mobile_app.ui.components.SettingsSection
import com.example.moodle_usth_mobile_app.ui.theme.AccentAmber
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

@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profile = viewModel.profile
    var confirmLogout by remember { mutableStateOf(false) }

    if (confirmLogout) {
        LogoutConfirmDialog(
            isDemo = viewModel.isDemoMode,
            onConfirm = {
                confirmLogout = false
                viewModel.logout()
            },
            onDismiss = { confirmLogout = false }
        )
    }
    val syncedAtMs = viewModel.lastSyncedAtMs
    val syncedAt = if (syncedAtMs != null) Formatters.syncedAt(context, syncedAtMs, viewModel.nowMs) else null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.prof_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.School, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(24.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Thẻ danh tính
            IdentityCard(
                initials = profile?.initials.orEmpty(),
                fullName = profile?.fullName.orEmpty(),
                cohort = profile?.cohort.orEmpty(),
                programName = profile?.programName.orEmpty()
            )

            // Thông tin sinh viên
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = stringResource(R.string.prof_student_info),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ProfileInfoRow(stringResource(R.string.prof_student_id), profile?.memberCode.orEmpty())
                    HorizontalDivider(color = CardBorderLight)
                    ProfileInfoRow(stringResource(R.string.prof_academic_year), profile?.academicYear.orEmpty())
                    HorizontalDivider(color = CardBorderLight)
                    ProfileInfoRow(stringResource(R.string.prof_current_semester), profile?.semester.orEmpty())
                }
            }

            LinkRow(
                icon = Icons.Default.Analytics,
                label = stringResource(R.string.prof_academic_progress),
                onClick = { viewModel.navigateTo(AppDestination.TRACKER) }
            )

            // Ngôn ngữ, giao diện, nhắc hạn nộp, thông báo
            Text(
                text = stringResource(R.string.settings_title).uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
            SettingsSection(viewModel = viewModel, showStudentOptions = true)

            // Trạng thái kết nối
            var icon = Icons.Default.CloudDone
            var tint = AccentGreen
            var title = stringResource(R.string.prof_synced_title)
            var body = if (syncedAt != null) stringResource(R.string.prof_synced_body_at, viewModel.serverUrl, syncedAt)
            else stringResource(R.string.prof_synced_body, viewModel.serverUrl)
            if (viewModel.isDemoMode) {
                icon = Icons.Default.Info
                tint = AccentAmber
                title = stringResource(R.string.prof_demo_title)
                body = stringResource(R.string.prof_demo_body)
            } else if (viewModel.isOffline) {
                icon = Icons.Default.CloudOff
                tint = AccentCoral
                title = stringResource(R.string.prof_offline_title)
                body = if (syncedAt != null) stringResource(R.string.prof_offline_body_at, viewModel.serverUrl, syncedAt)
                else stringResource(R.string.prof_offline_body, viewModel.serverUrl)
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = body, fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp)
                    }
                }
            }

            LogoutRow(onClick = { confirmLogout = true })

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/** Thẻ đầu trang hồ sơ: avatar chữ viết tắt, họ tên, khoá/khoa và ngành. Dùng chung cho giáo viên. */
@Composable
fun IdentityCard(initials: String, fullName: String, cohort: String, programName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(PrimaryIndigo),
                contentAlignment = Alignment.Center
            ) {
                Text(text = initials, color = OnAccent, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = fullName, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = cohort, fontSize = 13.sp, color = TextSecondary)
            if (programName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                BadgeItem(text = programName, tone = "sky")
            }
        }
    }
}

/** Hộp xác nhận đăng xuất. Dùng chung cho giáo viên (isDemo = false). */
@Composable
fun LogoutConfirmDialog(isDemo: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.prof_logout_confirm_title)) },
        text = {
            Text(stringResource(if (isDemo) R.string.prof_logout_confirm_demo else R.string.prof_logout_confirm_body))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.common_logout), color = AccentCoral, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
        containerColor = CardSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary
    )
}

/** Nút đăng xuất cuối trang hồ sơ. */
@Composable
fun LogoutRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AccentCoralLight)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = AccentCoral, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = stringResource(R.string.common_logout), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AccentCoral)
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = TextSecondary)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

@Composable
private fun LinkRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardSurface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(PrimaryIndigoLight, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
    }
}
