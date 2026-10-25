package com.example.moodle_usth_mobile_app.ui

import android.app.Application
import androidx.compose.runtime.mutableLongStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moodle_usth_mobile_app.data.local.DownloadSaver
import com.example.moodle_usth_mobile_app.data.model.AppData
import com.example.moodle_usth_mobile_app.data.model.AppNotification
import com.example.moodle_usth_mobile_app.data.model.Assignment
import com.example.moodle_usth_mobile_app.data.model.AssignmentStatus
import com.example.moodle_usth_mobile_app.data.model.ClassSession
import com.example.moodle_usth_mobile_app.data.model.Course
import com.example.moodle_usth_mobile_app.data.model.SemesterOverview
import com.example.moodle_usth_mobile_app.data.model.StudentProfile
import com.example.moodle_usth_mobile_app.data.remote.ApiException
import com.example.moodle_usth_mobile_app.data.repository.LearningRules
import com.example.moodle_usth_mobile_app.data.repository.MoodleRepository
import com.example.moodle_usth_mobile_app.data.repository.StudySummary
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import java.io.IOException

enum class AppDestination(val label: String) {
    HOME("Home"),
    COURSES("Courses"),
    TIMETABLE("Lịch học"),
    TRACKER("Tracker"),
    PROFILE("Profile")
}

sealed class DialogState {
    data class AssignmentDetail(val assignmentId: String) : DialogState()
    data class ClassworkList(val filter: String = "todo") : DialogState()
    object Notifications : DialogState()
    data class CourseInfo(val courseId: String) : DialogState()
    object CalendarPlanner : DialogState()
    object GradeReport : DialogState()
    object SemesterOverview : DialogState()
}

enum class AuthState { SIGNED_OUT, SIGNED_IN }

data class LoginUiState(val isSubmitting: Boolean = false, val error: String? = null)

data class LoadUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val error: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = MoodleRepository(application)

    private val _authState = MutableStateFlow(if (repository.isSignedIn) AuthState.SIGNED_IN else AuthState.SIGNED_OUT)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    private val _loadState = MutableStateFlow(LoadUiState())
    val loadState: StateFlow<LoadUiState> = _loadState.asStateFlow()

    private val _data = MutableStateFlow<AppData?>(null)
    val data: StateFlow<AppData?> = _data.asStateFlow()

    val assignments: StateFlow<List<Assignment>> = _data
        .map { it?.assignments.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Môn học kèm tiến độ tính lại từ danh sách bài tập hiện tại. */
    val courses: StateFlow<List<Course>> = _data
        .map { data -> data?.courses.orEmpty().map { LearningRules.courseWithProgress(it, data!!.assignments) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val classSessions: StateFlow<List<ClassSession>> = _data
        .map { it?.sessions.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val notifications: StateFlow<List<AppNotification>> = _data
        .map { it?.notifications.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val profile: StateFlow<StudentProfile?> = _data
        .map { it?.profile }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val semester: StateFlow<SemesterOverview?> = _data
        .map { it?.semester }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _currentDestination = MutableStateFlow(AppDestination.HOME)
    val currentDestination: StateFlow<AppDestination> = _currentDestination.asStateFlow()

    private val _selectedCourseId = MutableStateFlow<String?>(null)
    val selectedCourse: StateFlow<Course?> = combine(_selectedCourseId, courses) { id, list ->
        list.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Tab để quay lại khi bấm Back ở trang chi tiết môn học. */
    private var courseBackDestination = AppDestination.COURSES

    private val _activeDialog = MutableStateFlow<DialogState?>(null)
    val activeDialog: StateFlow<DialogState?> = _activeDialog.asStateFlow()

    private val _pendingAssignmentIds = MutableStateFlow<Set<String>>(emptySet())
    val pendingAssignmentIds: StateFlow<Set<String>> = _pendingAssignmentIds.asStateFlow()

    private val _timetableSelectedDateMs = MutableStateFlow(System.currentTimeMillis())
    val timetableSelectedDateMs: StateFlow<Long> = _timetableSelectedDateMs.asStateFlow()

    private val _timetableCalendarView = MutableStateFlow("week") // "week" | "month"
    val timetableCalendarView: StateFlow<String> = _timetableCalendarView.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    /**
     * Đồng hồ dùng chung cho mọi màn hình. Là Compose state nên những composable đọc nó
     * (qua [getAssignmentStatus], [formatTimeRemaining]...) sẽ tự cập nhật mỗi 30 giây.
     */
    private val clock = mutableLongStateOf(System.currentTimeMillis())
    val nowMs: Long get() = clock.longValue

    val isDemoMode: Boolean get() = repository.isDemoMode
    val serverUrl: String get() = repository.session.baseUrl

    init {
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                clock.longValue = System.currentTimeMillis()
            }
        }
        if (_authState.value == AuthState.SIGNED_IN) refresh(initial = true)
    }

    // ------------------------------------------------------------------ auth

    fun login(baseUrl: String, username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _loginState.value = LoginUiState(error = "Vui lòng nhập mã sinh viên và mật khẩu")
            return
        }
        viewModelScope.launch {
            _loginState.value = LoginUiState(isSubmitting = true)
            try {
                repository.login(baseUrl, username, password)
                _loginState.value = LoginUiState()
                onSignedIn()
            } catch (error: IOException) {
                _loginState.value = LoginUiState(error = error.toUserMessage(baseUrl))
            }
        }
    }

    fun startDemo() {
        repository.startDemo()
        _loginState.value = LoginUiState()
        onSignedIn()
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _data.value = null
            _activeDialog.value = null
            _selectedCourseId.value = null
            _currentDestination.value = AppDestination.HOME
            _loadState.value = LoadUiState()
            _authState.value = AuthState.SIGNED_OUT
        }
    }

    private fun onSignedIn() {
        _currentDestination.value = AppDestination.HOME
        _authState.value = AuthState.SIGNED_IN
        refresh(initial = true)
    }

    // ------------------------------------------------------------------ data

    fun refresh(initial: Boolean = false) {
        if (_loadState.value.isLoading || _loadState.value.isRefreshing) return
        viewModelScope.launch {
            _loadState.update { it.copy(isLoading = initial || _data.value == null, isRefreshing = !initial && _data.value != null, error = null) }
            try {
                val result = repository.load()
                _data.value = result.data
                clock.longValue = System.currentTimeMillis()
                _loadState.value = LoadUiState(isOffline = result.fromCache)
                if (result.fromCache) _messages.send("Không kết nối được máy chủ · đang hiển thị dữ liệu đã lưu.")
            } catch (error: ApiException) {
                if (error.status == 401) {
                    _authState.value = AuthState.SIGNED_OUT
                    _loginState.value = LoginUiState(error = "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.")
                    _loadState.value = LoadUiState()
                } else {
                    _loadState.value = LoadUiState(error = error.message)
                }
            } catch (error: IOException) {
                _loadState.value = LoadUiState(error = error.toUserMessage(serverUrl))
            }
        }
    }

    fun toggleAssignmentCompleted(assignmentId: String) {
        val assignment = findAssignment(assignmentId) ?: return
        if (assignmentId in _pendingAssignmentIds.value) return
        val markDone = !assignment.isCompleted
        viewModelScope.launch {
            _pendingAssignmentIds.update { it + assignmentId }
            try {
                val updated = repository.setCompleted(assignment, markDone)
                val data = _data.updateAndGet { data ->
                    data?.copy(assignments = data.assignments.map { if (it.id == updated.id) updated else it })
                }
                val course = data?.courses?.firstOrNull { it.id == updated.courseId }
                    ?.let { LearningRules.courseWithProgress(it, data.assignments) }
                if (course != null) {
                    val left = course.total - course.done
                    _messages.send(
                        if (markDone) "${course.shortTitle}: ${course.progress}% complete · $left tasks left."
                        else "Đã bỏ đánh dấu “${updated.title}”."
                    )
                }
            } catch (error: IOException) {
                _messages.send(error.toUserMessage(serverUrl))
            } finally {
                _pendingAssignmentIds.update { it - assignmentId }
            }
        }
    }

    fun markNotificationRead(notificationId: String) {
        val notification = notifications.value.firstOrNull { it.id == notificationId } ?: return
        if (notification.read) return
        _data.update { data ->
            data?.copy(notifications = data.notifications.map { if (it.id == notificationId) it.copy(read = true) else it })
        }
        viewModelScope.launch { runCatching { repository.markNotificationRead(notificationId) } }
    }

    fun downloadGradeReport() {
        val data = _data.value ?: return
        viewModelScope.launch {
            try {
                val file = repository.downloadGradeReport(data)
                val location = DownloadSaver.save(getApplication(), file)
                _messages.send("Đã tải báo cáo điểm: $location")
            } catch (error: IOException) {
                _messages.send(error.toUserMessage(serverUrl))
            }
        }
    }

    // ------------------------------------------------------------ navigation

    fun navigateTo(destination: AppDestination) {
        _currentDestination.value = destination
        _selectedCourseId.value = null // reset course detail when navigating tabs
    }

    fun selectCourse(course: Course?) {
        if (course == null) {
            _selectedCourseId.value = null
            _currentDestination.value = courseBackDestination
            return
        }
        if (_selectedCourseId.value == null) courseBackDestination = _currentDestination.value
        _selectedCourseId.value = course.id
        _activeDialog.value = null
    }

    fun openDialog(dialog: DialogState) {
        _activeDialog.value = dialog
    }

    fun closeDialog() {
        _activeDialog.value = null
    }

    fun setTimetableDate(dateMs: Long) {
        _timetableSelectedDateMs.value = dateMs
    }

    fun setTimetableCalendarView(view: String) {
        _timetableCalendarView.value = view
    }

    // --------------------------------------------------------------- helpers

    fun findAssignment(id: String): Assignment? = assignments.value.firstOrNull { it.id == id }

    fun findCourse(id: String): Course? = courses.value.firstOrNull { it.id == id }

    fun getStudySummary(currentTimeMs: Long = nowMs): StudySummary =
        LearningRules.summary(assignments.value, currentTimeMs)

    fun getAssignmentStatus(assignment: Assignment, currentTimeMs: Long = nowMs): AssignmentStatus =
        LearningRules.status(assignment, currentTimeMs)

    fun formatTimeRemaining(assignment: Assignment, currentTimeMs: Long = nowMs): String =
        LearningRules.timeRemaining(assignment, currentTimeMs)

    fun getCourseWithCompletion(course: Course): Course =
        LearningRules.courseWithProgress(course, assignments.value)

    private fun IOException.toUserMessage(baseUrl: String): String = when (this) {
        is ApiException -> message
        is java.net.SocketTimeoutException ->
            "Máy chủ không phản hồi ($baseUrl). Kiểm tra backend đã chạy và quyền truy cập mạng cục bộ (Cài đặt › Ứng dụng › Quyền)."
        is java.net.ConnectException, is java.net.NoRouteToHostException ->
            "Không kết nối được tới $baseUrl. Kiểm tra backend đã chạy và app đã được cấp quyền truy cập mạng cục bộ."
        else -> "Không kết nối được tới $baseUrl. Kiểm tra backend và địa chỉ máy chủ."
    }
}
