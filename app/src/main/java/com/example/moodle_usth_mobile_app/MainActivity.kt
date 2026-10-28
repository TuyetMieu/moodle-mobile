package com.example.moodle_usth_mobile_app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle_usth_mobile_app.data.local.LocaleHelper
import com.example.moodle_usth_mobile_app.data.model.UserRole
import com.example.moodle_usth_mobile_app.reminders.ReminderReceiver
import com.example.moodle_usth_mobile_app.reminders.UpdateNotifier
import com.example.moodle_usth_mobile_app.ui.AppDestination
import com.example.moodle_usth_mobile_app.ui.Formatters
import com.example.moodle_usth_mobile_app.ui.MainViewModel
import com.example.moodle_usth_mobile_app.ui.TeacherViewModel
import com.example.moodle_usth_mobile_app.ui.dialogs.AppSnackbar
import com.example.moodle_usth_mobile_app.ui.dialogs.AssignmentDetailDialog
import com.example.moodle_usth_mobile_app.ui.dialogs.ClassworkListDialog
import com.example.moodle_usth_mobile_app.ui.dialogs.CourseInfoDialog
import com.example.moodle_usth_mobile_app.ui.dialogs.GradeReportDialog
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
import com.example.moodle_usth_mobile_app.ui.teacher.TeacherApp
import com.example.moodle_usth_mobile_app.ui.theme.AccentAmber
import com.example.moodle_usth_mobile_app.ui.theme.AccentAmberLight
import com.example.moodle_usth_mobile_app.ui.theme.AccentCoral
import com.example.moodle_usth_mobile_app.ui.theme.AccentCoralLight
import com.example.moodle_usth_mobile_app.ui.theme.CardBorderLight
import com.example.moodle_usth_mobile_app.ui.theme.CardSurface
import com.example.moodle_usth_mobile_app.ui.theme.MoodleusthmobileappTheme
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigo
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigoLight
import com.example.moodle_usth_mobile_app.ui.theme.SurfaceLight
import com.example.moodle_usth_mobile_app.ui.theme.TextPrimary
import com.example.moodle_usth_mobile_app.ui.theme.TextSecondary
import com.example.moodle_usth_mobile_app.ui.theme.isAppInDarkTheme

class MainActivity : ComponentActivity() {

    // ViewModel giữ dữ liệu của cả app (không bị mất khi xoay màn hình hay đổi ngôn ngữ).
    private val viewModel: MainViewModel by viewModels()
    private val teacherViewModel: TeacherViewModel by viewModels()

    /** Áp ngôn ngữ người dùng đã chọn (Tiếng Việt / English / Français) trước khi vẽ giao diện. */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.localized(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) openFromNotification(intent)
        setContent {
            val dark = isAppInDarkTheme(viewModel.themeMode)
            // Chữ/icon trên thanh trạng thái đổi theo chế độ sáng/tối.
            LaunchedEffect(dark) {
                val transparent = android.graphics.Color.TRANSPARENT
                enableEdgeToEdge(
                    statusBarStyle = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent),
                    navigationBarStyle = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
                )
            }
            MoodleusthmobileappTheme(darkTheme = dark) {
                MoodleAppRoot(viewModel, teacherViewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppResumed()
    }

    /** Chạm thông báo khi app đang mở (launchMode="singleTop"). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openFromNotification(intent)
    }

    /** App được mở từ thông báo: nhắc hạn nộp / điểm / bài mới → mở bài tập; tin khác → mở danh sách thông báo. */
    private fun openFromNotification(intent: Intent?) {
        if (intent == null) return
        val assignmentId = intent.getStringExtra(ReminderReceiver.EXTRA_ASSIGNMENT_ID)
        val channel = intent.getStringExtra(UpdateNotifier.EXTRA_OPEN_NOTIFICATIONS)
        // Xoá đi để khi xoay màn hình không mở lại lần nữa.
        intent.removeExtra(ReminderReceiver.EXTRA_ASSIGNMENT_ID)
        intent.removeExtra(UpdateNotifier.EXTRA_OPEN_NOTIFICATIONS)
        if (assignmentId != null) viewModel.openAssignment(assignmentId)
        else if (channel != null) viewModel.openNotifications(channel)
    }
}

@Composable
fun MoodleAppRoot(viewModel: MainViewModel, teacherViewModel: TeacherViewModel) {
    if (!viewModel.isLoggedIn) {
        LoginScreen(viewModel)
    } else if (viewModel.role == UserRole.TEACHER) {
        TeacherApp(teacherViewModel, viewModel)
    } else {
        SignedInApp(viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignedInApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentDestination = viewModel.currentDestination
    val selectedCourse = viewModel.selectedCourse
    val assignments = viewModel.assignments
    val courses = viewModel.courses
    val data = viewModel.data

    // Mỗi khi ViewModel có thông báo mới → hiện snackbar, hiện xong thì xoá.
    val message = viewModel.message
    LaunchedEffect(message) {
        if (message != null) {
            AppSnackbar.state.showSnackbar(message)
            viewModel.message = null
        }
    }

    // Back: đóng trang chi tiết môn học trước, rồi về Home.
    BackHandler(enabled = selectedCourse != null) { viewModel.selectCourse(null) }
    BackHandler(enabled = selectedCourse == null && currentDestination != AppDestination.HOME) {
        viewModel.navigateTo(AppDestination.HOME)
    }

    val destinations = listOf(
        AppDestinationInfo(AppDestination.HOME, stringResource(R.string.nav_home), Icons.Default.Home),
        AppDestinationInfo(AppDestination.COURSES, stringResource(R.string.nav_courses), Icons.Default.Book),
        AppDestinationInfo(AppDestination.TIMETABLE, stringResource(R.string.nav_timetable), Icons.Default.CalendarToday),
        AppDestinationInfo(AppDestination.TRACKER, stringResource(R.string.nav_tracker), Icons.Default.Analytics),
        AppDestinationInfo(AppDestination.PROFILE, stringResource(R.string.nav_profile), Icons.Default.Person)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = SurfaceLight,
        // Khi đang mở dialog, AppModal tự hiển thị snackbar bên trong nên ẩn bản ở màn nền.
        snackbarHost = { if (viewModel.dialogName == "") SnackbarHost(AppSnackbar.state) },
        bottomBar = {
            AppBottomBar(
                items = destinations.map { item ->
                    // Trang chi tiết môn học thuộc tab Courses (như thiết kế).
                    val selected = if (selectedCourse != null) item.destination == AppDestination.COURSES
                    else currentDestination == item.destination
                    BottomBarItem(item.label, item.icon, selected) { viewModel.navigateTo(item.destination) }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (data != null && (viewModel.isOffline || viewModel.isDemoMode)) {
                val syncedAtMs = viewModel.lastSyncedAtMs
                StatusBanner(
                    isDemo = viewModel.isDemoMode,
                    syncedAtText = if (syncedAtMs != null) Formatters.syncedAt(context, syncedAtMs, viewModel.nowMs) else null,
                    onRetry = { viewModel.refresh() }
                )
            }

            val loadError = viewModel.loadError
            if (data == null && loadError != null) {
                // Chưa có dữ liệu và tải bị lỗi
                ErrorState(
                    message = loadError,
                    onRetry = { viewModel.refresh(initial = true) },
                    onLogout = { viewModel.logout() }
                )
            } else if (data == null) {
                // Đang tải lần đầu
                LoadingState()
            } else {
                PullToRefreshBox(
                    isRefreshing = viewModel.isRefreshing,
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

        // Dialog đang mở (nếu có). dialogArg là id bài tập / id môn học / bộ lọc / luồng thông báo.
        val onDismiss = { viewModel.closeDialog() }
        val arg = viewModel.dialogArg
        when (viewModel.dialogName) {
            "assignment" -> AssignmentDetailDialog(arg, viewModel, onDismiss)
            "classwork" -> ClassworkListDialog(if (arg == "") "todo" else arg, viewModel, onDismiss)
            "notifications" -> NotificationsDialog(arg, viewModel, onDismiss)
            "courseInfo" -> CourseInfoDialog(arg, viewModel, onDismiss)
            "planner" -> PlannerDialog(viewModel, onDismiss)
            "gradeReport" -> GradeReportDialog(viewModel, onDismiss)
            "semester" -> SemesterOverviewDialog(viewModel, onDismiss)
        }
    }
}

/** Một mục của thanh điều hướng dưới cùng. */
class BottomBarItem(val label: String, val icon: ImageVector, val selected: Boolean, val onClick: () -> Unit)

/** Thanh điều hướng dưới cùng — dùng chung cho sinh viên và giáo viên. */
@Composable
fun AppBottomBar(items: List<BottomBarItem>) {
    Column {
        HorizontalDivider(color = CardBorderLight)
        NavigationBar(containerColor = CardSurface, tonalElevation = 0.dp) {
            items.forEach { item ->
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null, // nhãn bên dưới đã đọc tên tab
                            tint = if (item.selected) PrimaryIndigo else TextSecondary
                        )
                    },
                    label = {
                        Text(
                            text = item.label,
                            fontSize = 11.sp,
                            maxLines = 1,
                            fontWeight = if (item.selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (item.selected) PrimaryIndigo else TextSecondary
                        )
                    },
                    selected = item.selected,
                    onClick = item.onClick,
                    colors = NavigationBarItemDefaults.colors(indicatorColor = PrimaryIndigoLight)
                )
            }
        }
    }
}

@Composable
private fun StatusBanner(isDemo: Boolean, syncedAtText: String?, onRetry: () -> Unit) {
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
            text = if (isDemo) stringResource(R.string.banner_demo)
            else if (syncedAtText != null) stringResource(R.string.banner_offline_at, syncedAtText)
            else stringResource(R.string.banner_offline),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

@Composable
fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = PrimaryIndigo)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = stringResource(R.string.common_loading), fontSize = 13.sp, color = TextSecondary)
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, onLogout: () -> Unit) {
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
        Text(text = stringResource(R.string.common_load_failed), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = message, fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center, lineHeight = 18.sp)
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.common_retry), fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onLogout, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.common_logout))
        }
    }
}

private data class AppDestinationInfo(
    val destination: AppDestination,
    val label: String,
    val icon: ImageVector
)
