package com.example.moodle_usth_mobile_app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle_usth_mobile_app.ui.AppDestination
import com.example.moodle_usth_mobile_app.ui.Formatters
import com.example.moodle_usth_mobile_app.ui.MainViewModel
import com.example.moodle_usth_mobile_app.ui.components.BadgeItem
import com.example.moodle_usth_mobile_app.ui.theme.AccentCoral
import com.example.moodle_usth_mobile_app.ui.theme.AccentCoralLight
import com.example.moodle_usth_mobile_app.ui.theme.AccentGreen
import com.example.moodle_usth_mobile_app.ui.theme.AccentAmber
import com.example.moodle_usth_mobile_app.ui.theme.CardBorderLight
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
    val profile = viewModel.profile
    var confirmLogout by remember { mutableStateOf(false) }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Đăng xuất?") },
            text = {
                Text(
                    if (viewModel.isDemoMode) "Thoát chế độ xem thử. Các đánh dấu hoàn thành tạm thời sẽ bị xoá."
                    else "Bạn sẽ cần đăng nhập lại để xem bài tập và lịch học. Dữ liệu đã lưu trên máy sẽ bị xoá."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    viewModel.logout()
                }) { Text("Đăng xuất", color = AccentCoral, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Huỷ") } },
            containerColor = Color.White
        )
    }
    val remindersEnabled = viewModel.remindersEnabled
    val syncedAtMs = viewModel.lastSyncedAtMs
    val syncedAt = if (syncedAtMs != null) Formatters.syncedAt(syncedAtMs, viewModel.nowMs) else null

    // Android 13+ phải xin POST_NOTIFICATIONS lúc chạy; máy cũ hơn có sẵn quyền.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.setRemindersOn(true)
        else viewModel.message = "Chưa được cấp quyền thông báo · có thể bật lại trong Cài đặt › Ứng dụng › Thông báo."
    }
    // Đã bật nhắc nhưng thông báo của app đang bị tắt trong Cài đặt → hiện cảnh báo.
    val notificationsBlocked = remindersEnabled && !viewModel.canPostNotifications()

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
            Text(text = "Your profile", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
            Icon(Icons.Default.School, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(24.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Identity card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
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
                        Text(text = profile?.initials.orEmpty(), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = profile?.fullName.orEmpty(), fontSize = 21.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = profile?.cohort.orEmpty(), fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    BadgeItem(text = profile?.program.orEmpty(), tone = "sky")
                }
            }

            // Student information
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(text = "STUDENT INFORMATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    InfoRow("Student ID", profile?.studentId.orEmpty())
                    HorizontalDivider(color = CardBorderLight)
                    InfoRow("Academic year", profile?.academicYear.orEmpty())
                    HorizontalDivider(color = CardBorderLight)
                    InfoRow("Current semester", profile?.semester.orEmpty())
                }
            }

            LinkRow(
                icon = Icons.Default.Analytics,
                label = "My academic progress",
                onClick = { viewModel.navigateTo(AppDestination.TRACKER) }
            )

            // Deadline reminders
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(PrimaryIndigoLight, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Nhắc hạn nộp", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Thông báo 24 giờ và 2 giờ trước hạn của bài chưa hoàn thành.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 17.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = remindersEnabled,
                            onCheckedChange = { checked ->
                                if (!checked) {
                                    viewModel.setRemindersOn(false)
                                } else if (Build.VERSION.SDK_INT >= 33 && !viewModel.canPostNotifications()) {
                                    // Android 13+ phải xin quyền thông báo trước.
                                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.setRemindersOn(true)
                                }
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = PrimaryIndigo)
                        )
                    }
                    if (notificationsBlocked) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Thông báo của app đang bị tắt trong Cài đặt hệ thống nên lời nhắc sẽ không hiện.",
                            fontSize = 12.sp,
                            color = AccentCoral,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // Connection status
            var icon = Icons.Default.CloudDone
            var tint = AccentGreen
            var title = "Đã đồng bộ"
            var body = "Dữ liệu lấy từ ${viewModel.serverUrl}" + (if (syncedAt != null) " lúc $syncedAt" else "") +
                ". App tự làm mới khi bạn quay lại sau 5 phút; kéo xuống để làm mới ngay."
            if (viewModel.isDemoMode) {
                icon = Icons.Default.Info
                tint = AccentAmber
                title = "Chế độ xem thử"
                body = "Đang dùng dữ liệu mẫu đóng gói trong app. Đăng xuất để kết nối tới backend."
            } else if (viewModel.isOffline) {
                icon = Icons.Default.CloudOff
                tint = AccentCoral
                title = "Đang ngoại tuyến"
                body = "Không kết nối được ${viewModel.serverUrl}. Dữ liệu hiển thị là bản đồng bộ " +
                    (if (syncedAt != null) "lúc $syncedAt." else "gần nhất.")
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
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

            // Logout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AccentCoralLight)
                    .clickable { confirmLogout = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = AccentCoral, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Đăng xuất", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AccentCoral)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = TextSecondary)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

@Composable
private fun LinkRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
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
