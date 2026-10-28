package com.example.moodle_usth_mobile_app.ui.teacher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.moodle_usth_mobile_app.AppBottomBar
import com.example.moodle_usth_mobile_app.BottomBarItem
import com.example.moodle_usth_mobile_app.ErrorState
import com.example.moodle_usth_mobile_app.LoadingState
import com.example.moodle_usth_mobile_app.R
import com.example.moodle_usth_mobile_app.ui.MainViewModel
import com.example.moodle_usth_mobile_app.ui.TeacherDestination
import com.example.moodle_usth_mobile_app.ui.TeacherViewModel
import com.example.moodle_usth_mobile_app.ui.dialogs.AppSnackbar
import com.example.moodle_usth_mobile_app.ui.theme.SurfaceLight

/**
 * Giao diện của giáo viên: 3 tab Lớp dạy / Đăng tin / Hồ sơ.
 * [main] giữ đăng nhập và cài đặt (ngôn ngữ, giao diện) dùng chung với sinh viên.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherApp(viewModel: TeacherViewModel, main: MainViewModel) {
    LaunchedEffect(Unit) {
        if (viewModel.data == null) viewModel.load(initial = true)
    }
    // Phiên hết hạn → xoá dữ liệu giáo viên và quay về màn đăng nhập.
    LaunchedEffect(viewModel.sessionExpired) {
        if (viewModel.sessionExpired) {
            viewModel.clear()
            main.onSessionExpired()
        }
    }
    // Snackbar: thông báo của cả hai ViewModel (VD đổi cài đặt ở Hồ sơ là của MainViewModel).
    val message = viewModel.message ?: main.message
    LaunchedEffect(message) {
        if (message != null) {
            AppSnackbar.state.showSnackbar(message)
            viewModel.message = null
            main.message = null
        }
    }

    BackHandler(enabled = viewModel.selectedCourseId != null) { viewModel.openCourse(null) }
    BackHandler(enabled = viewModel.selectedCourseId == null && viewModel.destination != TeacherDestination.COURSES) {
        viewModel.navigateTo(TeacherDestination.COURSES)
    }

    val logout = {
        viewModel.clear()
        main.logout()
    }

    val items = listOf(
        Triple(TeacherDestination.COURSES, stringResource(R.string.nav_teaching), Icons.Default.School),
        Triple(TeacherDestination.POSTS, stringResource(R.string.nav_posts), Icons.Default.Campaign),
        Triple(TeacherDestination.PROFILE, stringResource(R.string.nav_profile), Icons.Default.Person)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = SurfaceLight,
        snackbarHost = {
            val dialogOpen = viewModel.assignmentEditor != null || viewModel.composerAudience != null
            if (!dialogOpen) SnackbarHost(AppSnackbar.state)
        },
        bottomBar = {
            AppBottomBar(items.map { (destination, label, icon) ->
                BottomBarItem(label, icon, viewModel.destination == destination) { viewModel.navigateTo(destination) }
            })
        }
    ) { innerPadding ->
        val data = viewModel.data
        val loadError = viewModel.loadError
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        if (data == null && loadError != null) {
            ErrorState(message = loadError, onRetry = { viewModel.load(initial = true) }, onLogout = logout)
        } else if (data == null) {
            LoadingState()
        } else {
            PullToRefreshBox(
                isRefreshing = viewModel.isRefreshing,
                onRefresh = { viewModel.load() },
                modifier = contentModifier
            ) {
                when (viewModel.destination) {
                    TeacherDestination.COURSES -> {
                        val course = viewModel.selectedCourse
                        if (course != null) TeacherCourseScreen(course, viewModel)
                        else TeacherCoursesScreen(data, viewModel)
                    }
                    TeacherDestination.POSTS -> TeacherPostsScreen(data, viewModel)
                    TeacherDestination.PROFILE -> TeacherProfileScreen(data, viewModel, main, onLogout = logout)
                }
            }
        }

        if (viewModel.assignmentEditor != null) AssignmentEditorDialog(viewModel)
        if (viewModel.composerAudience != null) PostComposerDialog(viewModel)
    }
}
