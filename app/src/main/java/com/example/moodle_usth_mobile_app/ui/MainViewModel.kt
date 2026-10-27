package com.example.moodle_usth_mobile_app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.example.moodle_usth_mobile_app.reminders.ReminderHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 5 tab ở thanh dưới cùng. */
enum class AppDestination {
    HOME, COURSES, TIMETABLE, TRACKER, PROFILE
}

/**
 * ViewModel giữ toàn bộ dữ liệu của app.
 *
 * Mỗi biến khai báo bằng `mutableStateOf` là "state": khi giá trị đổi, các màn hình đang đọc
 * biến đó sẽ tự vẽ lại. Màn hình chỉ cần đọc `viewModel.tenBien` là được.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = MoodleRepository(application)
    private val context = application

    // ---------------------------------------------------------- đăng nhập

    var isLoggedIn by mutableStateOf(repository.isSignedIn)
    var isLoggingIn by mutableStateOf(false)
    var loginError by mutableStateOf<String?>(null)

    // ---------------------------------------------------------- dữ liệu

    /** Toàn bộ dữ liệu lấy từ GET /api/v1/bootstrap. null = chưa tải xong. */
    var data by mutableStateOf<AppData?>(null)
    var isLoading by mutableStateOf(false)
    var isRefreshing by mutableStateOf(false)
    var isOffline by mutableStateOf(false)
    var loadError by mutableStateOf<String?>(null)
    var lastSyncedAtMs by mutableStateOf<Long?>(null)

    // ---------------------------------------------------------- điều hướng

    var currentDestination by mutableStateOf(AppDestination.HOME)
    var selectedCourseId by mutableStateOf<String?>(null)
    private var tabBeforeCourse = AppDestination.COURSES

    /**
     * Dialog đang mở: "" = không có. Các tên dùng trong app:
     * "assignment", "classwork", "notifications", "courseInfo", "planner", "gradeReport", "semester".
     * [dialogArg] là tham số kèm theo (id bài tập, id môn học hoặc bộ lọc "todo"/"urgent"/"done").
     */
    var dialogName by mutableStateOf("")
    var dialogArg by mutableStateOf("")

    // ---------------------------------------------------------- linh tinh

    /** Bài tập đang chờ server trả lời khi bấm Mark as done / Undo (để khoá nút). */
    var savingAssignmentId by mutableStateOf<String?>(null)

    var timetableSelectedDateMs by mutableLongStateOf(System.currentTimeMillis())
    var timetableCalendarView by mutableStateOf("week") // "week" hoặc "month"

    /** Thông báo ngắn hiện ở dưới màn hình (snackbar). MainActivity hiện xong sẽ đặt lại null. */
    var message by mutableStateOf<String?>(null)

    var remindersEnabled by mutableStateOf(ReminderHelper.isEnabled(application))

    /** Giờ hiện tại, cập nhật mỗi 30 giây để các nhãn "còn 2 giờ", "quá hạn" tự đổi. */
    var nowMs by mutableLongStateOf(System.currentTimeMillis())

    val isDemoMode: Boolean get() = repository.isDemoMode
    val serverUrl: String get() = repository.session.baseUrl

    // ---------------------------------------------------------- dữ liệu suy ra từ `data`

    val assignments: List<Assignment>
        get() = data?.assignments ?: emptyList()

    /** Môn học, với số bài đã xong / tổng số bài tính lại từ danh sách bài hiện tại. */
    val courses: List<Course>
        get() {
            val list = data?.courses ?: return emptyList()
            return list.map { LearningRules.courseWithProgress(it, assignments) }
        }

    val classSessions: List<ClassSession> get() = data?.sessions ?: emptyList()
    val notifications: List<AppNotification> get() = data?.notifications ?: emptyList()
    val profile: StudentProfile? get() = data?.profile
    val semester: SemesterOverview? get() = data?.semester
    val selectedCourse: Course? get() = courses.find { it.id == selectedCourseId }

    init {
        // Đồng hồ: cứ 30 giây cập nhật nowMs một lần.
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                nowMs = System.currentTimeMillis()
            }
        }
        if (isLoggedIn) refresh(initial = true)
    }

    // ---------------------------------------------------------- đăng nhập / đăng xuất

    fun login(baseUrl: String, username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            loginError = "Vui lòng nhập mã sinh viên và mật khẩu"
            return
        }
        viewModelScope.launch {
            isLoggingIn = true
            loginError = null
            try {
                repository.login(baseUrl, username, password)
                onLoggedIn()
            } catch (error: Exception) {
                loginError = errorText(error, baseUrl)
            }
            isLoggingIn = false
        }
    }

    fun startDemo() {
        repository.startDemo()
        loginError = null
        onLoggedIn()
    }

    private fun onLoggedIn() {
        currentDestination = AppDestination.HOME
        isLoggedIn = true
        refresh(initial = true)
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            ReminderHelper.cancelAll(context)
            data = null
            dialogName = ""
            selectedCourseId = null
            currentDestination = AppDestination.HOME
            isOffline = false
            loadError = null
            lastSyncedAtMs = null
            isLoggedIn = false
        }
    }

    // ---------------------------------------------------------- tải dữ liệu

    /**
     * Tải lại dữ liệu từ server (hoặc bản lưu khi mất mạng).
     * quiet = true khi app tự làm mới lúc quay lại: không làm phiền người dùng bằng thông báo lỗi.
     */
    fun refresh(initial: Boolean = false, quiet: Boolean = false) {
        if (isLoading || isRefreshing) return // đang tải rồi thì thôi
        val wasOffline = isOffline
        if (initial || data == null) isLoading = true else isRefreshing = true
        loadError = null

        viewModelScope.launch {
            try {
                val result = repository.load()
                setNewData(result.data)
                nowMs = System.currentTimeMillis()
                isOffline = result.fromCache
                lastSyncedAtMs = result.syncedAtMs
                if (result.fromCache && !(quiet && wasOffline)) {
                    message = "Không kết nối được máy chủ · đang hiển thị dữ liệu đã lưu."
                }
            } catch (error: Exception) {
                if (error is ApiException && error.status == 401) {
                    // Token hết hạn → quay về màn đăng nhập.
                    isLoggedIn = false
                    loginError = "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
                    ReminderHelper.cancelAll(context)
                } else if (data == null) {
                    loadError = errorText(error, serverUrl) // chưa có gì để xem → hiện màn lỗi
                } else if (!quiet) {
                    message = errorText(error, serverUrl) // đã có dữ liệu → chỉ báo bằng snackbar
                }
            }
            isLoading = false
            isRefreshing = false
        }
    }

    /** Gọi từ MainActivity.onResume: cập nhật giờ và tự làm mới nếu dữ liệu đã cũ hơn 5 phút. */
    fun onAppResumed() {
        nowMs = System.currentTimeMillis()
        if (!isLoggedIn || isDemoMode || data == null) return
        val syncedAt = lastSyncedAtMs ?: 0L
        val fiveMinutes = 5 * 60_000L
        if (isOffline || nowMs - syncedAt > fiveMinutes) refresh(quiet = true)
    }

    /** Đổi dữ liệu và đặt lại lịch nhắc hạn nộp cho khớp. */
    private fun setNewData(newData: AppData) {
        data = newData
        ReminderHelper.update(context, newData.assignments)
    }

    // ---------------------------------------------------------- thao tác của người dùng

    fun toggleAssignmentCompleted(assignmentId: String) {
        val assignment = findAssignment(assignmentId) ?: return
        val currentData = data ?: return
        if (savingAssignmentId != null) return // đang lưu bài khác
        val markDone = !assignment.isCompleted

        viewModelScope.launch {
            savingAssignmentId = assignmentId
            try {
                val updated = repository.setCompleted(assignment, markDone)
                // Thay bài cũ bằng bài server trả về.
                val newAssignments = currentData.assignments.map { if (it.id == updated.id) updated else it }
                setNewData(currentData.copy(assignments = newAssignments))

                val course = findCourse(updated.courseId)
                if (course != null) {
                    message = if (markDone) {
                        "${course.shortTitle}: ${course.progress}% complete · ${course.total - course.done} tasks left."
                    } else {
                        "Đã bỏ đánh dấu “${updated.title}”."
                    }
                }
            } catch (error: Exception) {
                message = errorText(error, serverUrl)
            }
            savingAssignmentId = null
        }
    }

    fun markNotificationRead(notificationId: String) {
        val currentData = data ?: return
        val notification = notifications.find { it.id == notificationId } ?: return
        if (notification.read) return
        // Đổi ngay trên màn hình, gửi lên server sau.
        val newList = currentData.notifications.map { if (it.id == notificationId) it.copy(read = true) else it }
        data = currentData.copy(notifications = newList)
        viewModelScope.launch {
            try {
                repository.markNotificationRead(notificationId)
            } catch (error: Exception) {
                // Bỏ qua: lần làm mới sau sẽ lấy lại trạng thái đúng từ server.
            }
        }
    }

    fun downloadGradeReport() {
        val currentData = data ?: return
        viewModelScope.launch {
            try {
                val file = repository.downloadGradeReport(currentData)
                val location = DownloadSaver.save(context, file)
                message = "Đã tải báo cáo điểm: $location"
            } catch (error: Exception) {
                message = errorText(error, serverUrl)
            }
        }
    }

    // ---------------------------------------------------------- nhắc hạn nộp

    fun setRemindersOn(enabled: Boolean) {
        ReminderHelper.setEnabled(context, enabled)
        remindersEnabled = enabled
        val count = ReminderHelper.update(context, assignments)
        message = if (!enabled) {
            "Đã tắt nhắc hạn nộp."
        } else if (count == 0) {
            "Đã bật nhắc hạn nộp · hiện không có bài nào cần nhắc."
        } else {
            "Đã bật nhắc hạn nộp · $count lời nhắc (24 giờ và 2 giờ trước hạn)."
        }
    }

    fun canPostNotifications(): Boolean = ReminderHelper.canShowNotifications(context)

    // ---------------------------------------------------------- điều hướng

    fun navigateTo(destination: AppDestination) {
        currentDestination = destination
        selectedCourseId = null
    }

    /** Mở trang chi tiết môn học. Truyền null để đóng (quay về tab trước đó). */
    fun selectCourse(course: Course?) {
        if (course == null) {
            selectedCourseId = null
            currentDestination = tabBeforeCourse
            return
        }
        if (selectedCourseId == null) tabBeforeCourse = currentDestination
        selectedCourseId = course.id
        dialogName = ""
    }

    fun openDialog(name: String, arg: String = "") {
        dialogName = name
        dialogArg = arg
    }

    fun closeDialog() {
        dialogName = ""
    }

    /** Mở chi tiết bài tập khi người dùng chạm vào thông báo nhắc hạn nộp. */
    fun openAssignment(assignmentId: String) {
        if (isLoggedIn) openDialog("assignment", assignmentId)
    }

    // ---------------------------------------------------------- hàm tiện ích cho màn hình

    fun findAssignment(id: String): Assignment? = assignments.find { it.id == id }

    fun findCourse(id: String): Course? = courses.find { it.id == id }

    fun getStudySummary(): StudySummary = LearningRules.summary(assignments, nowMs)

    fun getAssignmentStatus(assignment: Assignment): AssignmentStatus = LearningRules.status(assignment, nowMs)

    fun formatTimeRemaining(assignment: Assignment): String = LearningRules.timeRemaining(assignment, nowMs)

    fun getCourseWithCompletion(course: Course): Course = LearningRules.courseWithProgress(course, assignments)

    /** Đổi lỗi thành câu dễ hiểu cho người dùng. */
    private fun errorText(error: Exception, baseUrl: String): String {
        if (error is ApiException) return error.message
        if (error is java.net.SocketTimeoutException) {
            return "Máy chủ không phản hồi ($baseUrl). Kiểm tra backend đã chạy và quyền truy cập mạng cục bộ (Cài đặt › Ứng dụng › Quyền)."
        }
        if (error is java.net.ConnectException || error is java.net.NoRouteToHostException) {
            return "Không kết nối được tới $baseUrl. Kiểm tra backend đã chạy và app đã được cấp quyền truy cập mạng cục bộ."
        }
        return "Không kết nối được tới $baseUrl. Kiểm tra backend và địa chỉ máy chủ."
    }
}
