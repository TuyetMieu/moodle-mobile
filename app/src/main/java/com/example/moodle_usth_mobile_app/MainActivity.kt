package com.example.moodle_usth_mobile_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moodle_usth_mobile_app.ui.AppDestination
import com.example.moodle_usth_mobile_app.ui.AuthState
import com.example.moodle_usth_mobile_app.ui.DialogState
import com.example.moodle_usth_mobile_app.ui.MainViewModel
import com.example.moodle_usth_mobile_app.ui.dialogs.AssignmentDetailDialog
import com.example.moodle_usth_mobile_app.ui.dialogs.ClassworkListDialog
import com.example.moodle_usth_mobile_app.ui.dialogs.CourseInfoDialog
import com.example.moodle_usth_mobile_app.ui.dialogs.GradeReportDialog
import com.example.moodle_usth_mobile_app.ui.dialogs.LocalSnackbarHostState
import com.example.moodle_usth_mobile_app.ui.dialogs.NotificationsDialog
import com.example.moodle_usth_mobile_app.ui.dialogs.PlannerDialog
import com.example.moodle_usth_mobile_app.ui.dialogs.SemesterOverviewDialog
import com.example.moodle_usth_mobile_app.ui.screens.CourseDetailScreen
import com.example.moodle_usth_mobile_app.ui.screens.CoursesScreen
import com.example.moodle_usth_mobile_app.ui.screens.HomeScreen
import com.example.moodle_usth_mobile_app.ui.screens.LoginScreen
import com.example.moodle_usth_mobile_app.ui.screens.ProfileScreen
import com.example.moodle_usth_mobile_app.ui.screens.TimetableScreen
import com.example.moodle_usth_mobile_app.ui.screens.TrackerScreen
import com.example.moodle_usth_mobile_app.ui.theme.AccentAmber
import com.example.moodle_usth_mobile_app.ui.theme.AccentAmberLight
import com.example.moodle_usth_mobile_app.ui.theme.AccentCoral
import com.example.moodle_usth_mobile_app.ui.theme.AccentCoralLight
import com.example.moodle_usth_mobile_app.ui.theme.CardBorderLight
import com.example.moodle_usth_mobile_app.ui.theme.MoodleusthmobileappTheme
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigo
import com.example.moodle_usth_mobile_app.ui.theme.SurfaceLight
import com.example.moodle_usth_mobile_app.ui.theme.TextPrimary
import com.example.moodle_usth_mobile_app.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Thiết kế chỉ có giao diện sáng: cố định icon thanh trạng thái màu tối kể cả khi máy bật dark mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.WHITE, android.graphics.Color.WHITE)
        )
        setContent {
            MoodleusthmobileappTheme(darkTheme = false) {
                MoodleAppRoot()
            }
        }
    }
}

@Composable
fun MoodleAppRoot(
    viewModel: MainViewModel = viewModel()
) {
    val authState by viewModel.authState.collectAsState()
    when (authState) {
        AuthState.SIGNED_OUT -> LoginScreen(viewModel)
        AuthState.SIGNED_IN -> SignedInApp(viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignedInApp(viewModel: MainViewModel) {
    val currentDestination by viewModel.currentDestination.collectAsState()
    val selectedCourse by viewModel.selectedCourse.collectAsState()
    val assignments by viewModel.assignments.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val activeDialog by viewModel.activeDialog.collectAsState()
    val loadState by viewModel.loadState.collectAsState()
    val data by viewModel.data.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    // Back: đóng trang chi tiết môn học trước, rồi về Home.
    BackHandler(enabled = selectedCourse != null) { viewModel.selectCourse(null) }
    BackHandler(enabled = selectedCourse == null && currentDestination != AppDestination.HOME) {
        viewModel.navigateTo(AppDestination.HOME)
    }

    val destinations = listOf(
        AppDestinationInfo(AppDestination.HOME, "Home", Icons.Default.Home),
        AppDestinationInfo(AppDestination.COURSES, "Courses", Icons.Default.Book),
        AppDestinationInfo(AppDestination.TIMETABLE, "Lịch học", Icons.Default.CalendarToday),
        AppDestinationInfo(AppDestination.TRACKER, "Tracker", Icons.Default.Analytics),
        AppDestinationInfo(AppDestination.PROFILE, "Profile", Icons.Default.Person)
    )

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = SurfaceLight,
        // Khi đang mở dialog, AppModal tự hiển thị snackbar bên trong nên ẩn bản ở màn nền.
        snackbarHost = { if (activeDialog == null) SnackbarHost(snackbarHostState) },
        bottomBar = {
          Column {
            HorizontalDivider(color = CardBorderLight)
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 0.dp
            ) {
                destinations.forEach { item ->
                    // Trang chi tiết môn học thuộc tab Courses (như thiết kế).
                    val selected = if (selectedCourse != null) item.destination == AppDestination.COURSES
                    else currentDestination == item.destination
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (selected) PrimaryIndigo else TextSecondary
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) PrimaryIndigo else TextSecondary
                            )
                        },
                        selected = selected,
                        onClick = { viewModel.navigateTo(item.destination) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color(0xFFEEF2FF)
                        )
                    )
                }
            }
        }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (data != null && (loadState.isOffline || viewModel.isDemoMode)) {
                StatusBanner(
                    isDemo = viewModel.isDemoMode,
                    onRetry = { viewModel.refresh() }
                )
            }

            val currentData = data
            when {
                currentData == null && loadState.error != null -> ErrorState(
                    message = loadState.error.orEmpty(),
                    onRetry = { viewModel.refresh(initial = true) },
                    onLogout = { viewModel.logout() }
                )
                currentData == null -> LoadingState()
                else -> PullToRefreshBox(
                    isRefreshing = loadState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    val course = selectedCourse
                    if (course != null) {
                        CourseDetailScreen(
                            course = course,
                            viewModel = viewModel,
                            onBackClick = { viewModel.selectCourse(null) }
                        )
                    } else {
                        when (currentDestination) {
                            AppDestination.HOME -> HomeScreen(
                                viewModel = viewModel,
                                assignments = assignments,
                                courses = courses,
                                onCourseClick = { viewModel.selectCourse(it) }
                            )
                            AppDestination.COURSES -> CoursesScreen(
                                viewModel = viewModel,
                                courses = courses,
                                onCourseClick = { viewModel.selectCourse(it) }
                            )
                            AppDestination.TIMETABLE -> TimetableScreen(
                                viewModel = viewModel,
                                onCourseClick = { viewModel.selectCourse(it) }
                            )
                            AppDestination.TRACKER -> TrackerScreen(
                                viewModel = viewModel,
                                courses = courses,
                                onCourseClick = { viewModel.selectCourse(it) }
                            )
                            AppDestination.PROFILE -> ProfileScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }

        val onDismiss = { viewModel.closeDialog() }
        when (val dialog = activeDialog) {
            is DialogState.AssignmentDetail -> AssignmentDetailDialog(dialog.assignmentId, viewModel, onDismiss)
            is DialogState.ClassworkList -> ClassworkListDialog(dialog.filter, viewModel, onDismiss)
            is DialogState.Notifications -> NotificationsDialog(viewModel, onDismiss)
            is DialogState.CourseInfo -> CourseInfoDialog(dialog.courseId, viewModel, onDismiss)
            is DialogState.CalendarPlanner -> PlannerDialog(viewModel, onDismiss)
            is DialogState.GradeReport -> GradeReportDialog(viewModel, onDismiss)
            is DialogState.SemesterOverview -> SemesterOverviewDialog(viewModel, onDismiss)
            null -> {}
        }
    }
    }
}

@Composable
private fun StatusBanner(isDemo: Boolean, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isDemo) AccentAmberLight else AccentCoralLight)
            .clickable(enabled = !isDemo, onClick = onRetry)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isDemo) Icons.Default.Info else Icons.Default.CloudOff,
            contentDescription = null,
            tint = if (isDemo) AccentAmber else AccentCoral,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isDemo) "Chế độ xem thử · dữ liệu mẫu, không kết nối máy chủ"
            else "Ngoại tuyến · đang xem dữ liệu đã lưu. Chạm để thử lại.",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = PrimaryIndigo)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Đang đồng bộ với Moodle…", fontSize = 13.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(AccentCoralLight, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.WifiOff, contentDescription = null, tint = AccentCoral, modifier = Modifier.size(30.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Không tải được dữ liệu", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = message, fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center, lineHeight = 18.sp)
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Thử lại", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onLogout, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Text(text = "Đăng xuất")
        }
    }
}

private data class AppDestinationInfo(
    val destination: AppDestination,
    val label: String,
    val icon: ImageVector
)
