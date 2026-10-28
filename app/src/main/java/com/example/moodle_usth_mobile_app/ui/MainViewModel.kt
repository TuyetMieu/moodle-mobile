package com.example.moodle_usth_mobile_app.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moodle_usth_mobile_app.R
import com.example.moodle_usth_mobile_app.data.local.AppLanguage
import com.example.moodle_usth_mobile_app.data.local.AppSettings
import com.example.moodle_usth_mobile_app.data.local.DownloadSaver
import com.example.moodle_usth_mobile_app.data.local.LocaleHelper
import com.example.moodle_usth_mobile_app.data.local.ReminderLead
import com.example.moodle_usth_mobile_app.data.local.ThemeMode
import com.example.moodle_usth_mobile_app.data.model.AppData
import com.example.moodle_usth_mobile_app.data.model.AppNotification
import com.example.moodle_usth_mobile_app.data.model.Assignment
import com.example.moodle_usth_mobile_app.data.model.AssignmentStatus
import com.example.moodle_usth_mobile_app.data.model.ClassSession
import com.example.moodle_usth_mobile_app.data.model.Course
import com.example.moodle_usth_mobile_app.data.model.NotificationChannelKind
import com.example.moodle_usth_mobile_app.data.model.SemesterOverview
import com.example.moodle_usth_mobile_app.data.model.UserProfile
import com.example.moodle_usth_mobile_app.data.model.UserRole
import com.example.moodle_usth_mobile_app.data.remote.ApiException
import com.example.moodle_usth_mobile_app.data.repository.LearningRules
import com.example.moodle_usth_mobile_app.data.repository.MoodleRepository
import com.example.moodle_usth_mobile_app.data.repository.StudySummary
import com.example.moodle_usth_mobile_app.reminders.NotificationText
import com.example.moodle_usth_mobile_app.reminders.NotificationTexts
import com.example.moodle_usth_mobile_app.reminders.ReminderHelper
import com.example.moodle_usth_mobile_app.reminders.SyncWorker
import com.example.moodle_usth_mobile_app.reminders.UpdateNotifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 5 tab ở thanh dưới cùng của sinh viên. */
enum class AppDestination {
    HOME, COURSES, TIMETABLE, TRACKER, PROFILE
}

/**
 * ViewModel giữ dữ liệu đăng nhập, cài đặt và toàn bộ dữ liệu của sinh viên.
 * (Dữ liệu của giáo viên nằm ở [TeacherViewModel].)
 *
 * Mỗi biến khai báo bằng `mutableStateOf` là "state": khi giá trị đổi, các màn hình đang đọc
 * biến đó sẽ tự vẽ lại. Màn hình chỉ cần đọc `viewModel.tenBien` là được.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = MoodleRepository(application)
    private val context = application
    private val settings = AppSettings(application)

    /** Context theo ngôn ngữ app đang dùng — lấy lại mỗi lần vì người dùng có thể đổi ngôn ngữ. */
    private val res: Context get() = LocaleHelper.localized(context)

    // ---------------------------------------------------------- đăng nhập

    var isLoggedIn by mutableStateOf(repository.isSignedIn)
    var role by mutableStateOf(repository.role)
    var isLoggingIn by mutableStateOf(false)
    var loginError by mutableStateOf<String?>(null)

    // ---------------------------------------------------------- cài đặt

    var themeMode by mutableStateOf(settings.themeMode)
        private set
    var language by mutableStateOf(settings.language)
        private set
    var remindersEnabled by mutableStateOf(ReminderHelper.isEnabled(application))
    var reminderLeads by mutableStateOf(settings.reminderLeads)
        private set
    var notifyProgram by mutableStateOf(settings.notifyProgram)
        private set
    var notifyGeneral by mutableStateOf(settings.notifyGeneral)
        private set

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
     * [dialogArg] là tham số kèm theo (id bài tập, id môn học, bộ lọc "todo"/"urgent"/"done",
     * hoặc luồng thông báo "PROGRAM"/"GENERAL").
     */
    var dialogName by mutableStateOf("")
    var dialogArg by mutableStateOf("")

    // ---------------------------------------------------------- linh tinh

    /** Thông báo ngắn hiện ở dưới màn hình (snackbar). MainActivity hiện xong sẽ đặt lại null. */
    var message by mutableStateOf<String?>(null)

    /** Bài tập đang chờ server trả lời khi bấm Mark as done / Undo (để khoá nút). */
    var savingAssignmentId by mutableStateOf<String?>(null)

    var timetableSelectedDateMs by mutableLongStateOf(System.currentTimeMillis())
    var timetableCalendarView by mutableStateOf("week") // "week" hoặc "month"

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
    val profile: UserProfile? get() = data?.profile
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
        if (isLoggedIn && role == UserRole.STUDENT) {
            refresh(initial = true)
            if (!isDemoMode) SyncWorker.schedule(context)
        }
    }

    // ---------------------------------------------------------- đăng nhập / đăng xuất

    fun login(baseUrl: String, username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            loginError = res.getString(R.string.error_missing_credentials)
            return
        }
        viewModelScope.launch {
            isLoggingIn = true
            loginError = null
            try {
                val newRole = repository.login(baseUrl, username, password)
                onLoggedIn(newRole)
            } catch (error: Exception) {
                loginError = ErrorTexts.of(res, error, baseUrl)
            }
            isLoggingIn = false
        }
    }

    fun startDemo() {
        repository.startDemo()
        loginError = null
        onLoggedIn(UserRole.STUDENT)
    }

    private fun onLoggedIn(newRole: UserRole) {
        currentDestination = AppDestination.HOME
        role = newRole
        isLoggedIn = true
        // Giáo viên: TeacherViewModel tự tải dữ liệu khi màn giáo viên hiện ra.
        if (newRole == UserRole.STUDENT) {
            refresh(initial = true)
            if (!isDemoMode) SyncWorker.schedule(context)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            ReminderHelper.cancelAll(context)
            SyncWorker.cancel(context)
            data = null
            dialogName = ""
            selectedCourseId = null
            currentDestination = AppDestination.HOME
            isOffline = false
            loadError = null
            lastSyncedAtMs = null
            role = UserRole.STUDENT
            isLoggedIn = false
        }
    }

    /** Phiên đăng nhập hết hạn (401) — dùng chung cho màn sinh viên và giáo viên. */
    fun onSessionExpired() {
        repository.session.clear()
        ReminderHelper.cancelAll(context)
        SyncWorker.cancel(context)
        data = null
        isLoggedIn = false
        loginError = res.getString(R.string.error_session_expired)
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
                if (!result.fromCache && !isDemoMode) UpdateNotifier.markSeen(context, result.data)
                if (result.fromCache && !(quiet && wasOffline)) {
                    message = res.getString(R.string.error_offline_cache)
                }
            } catch (error: Exception) {
                if (error is ApiException && error.status == 401) {
                    onSessionExpired() // token hết hạn → quay về màn đăng nhập
                } else if (data == null) {
                    loadError = ErrorTexts.of(res, error, serverUrl) // chưa có gì để xem → hiện màn lỗi
                } else if (!quiet) {
                    message = ErrorTexts.of(res, error, serverUrl) // đã có dữ liệu → chỉ báo bằng snackbar
                }
            }
            isLoading = false
            isRefreshing = false
        }
    }

    /** Gọi từ MainActivity.onResume: cập nhật giờ và tự làm mới nếu dữ liệu đã cũ hơn 5 phút. */
    fun onAppResumed() {
        nowMs = System.currentTimeMillis()
        if (!isLoggedIn || role != UserRole.STUDENT || isDemoMode || data == null) return
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
                        res.getString(R.string.msg_course_progress, course.shortTitle, course.progress, course.total - course.done)
                    } else {
                        res.getString(R.string.msg_unmarked, updated.title)
                    }
                }
            } catch (error: Exception) {
                message = ErrorTexts.of(res, error, serverUrl)
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

    /** Đánh dấu đã đọc cả một luồng thông báo (null = tất cả). */
    fun markAllNotificationsRead(channel: NotificationChannelKind?) {
        val currentData = data ?: return
        val ids = currentData.notifications.filter { !it.read && (channel == null || it.channel == channel) }.map { it.id }
        if (ids.isEmpty()) return
        data = currentData.copy(notifications = currentData.notifications.map { if (it.id in ids) it.copy(read = true) else it })
        viewModelScope.launch {
            try {
                repository.markAllNotificationsRead(channel, ids)
            } catch (error: Exception) {
                // Bỏ qua như markNotificationRead.
            }
        }
    }

    fun downloadGradeReport() {
        val currentData = data ?: return
        viewModelScope.launch {
            try {
                val file = repository.downloadGradeReport(currentData)
                val location = DownloadSaver.save(context, file)
                message = res.getString(R.string.msg_report_downloaded, location)
            } catch (error: Exception) {
                message = ErrorTexts.of(res, error, serverUrl)
            }
        }
    }

    // ---------------------------------------------------------- cài đặt

    fun setTheme(mode: ThemeMode) {
        settings.themeMode = mode
        themeMode = mode
    }

    /** Lưu ngôn ngữ. Màn hình gọi xong phải tạo lại Activity (recreate) để áp dụng. */
    fun setAppLanguage(value: AppLanguage) {
        settings.language = value
        language = value
    }

    fun setRemindersOn(enabled: Boolean) {
        ReminderHelper.setEnabled(context, enabled)
        remindersEnabled = enabled
        val count = ReminderHelper.update(context, assignments)
        message = remindersMessage(enabled, count)
    }

    fun toggleReminderLead(lead: ReminderLead) {
        val next = if (lead in reminderLeads) reminderLeads - lead else reminderLeads + lead
        if (next.isEmpty()) {
            message = res.getString(R.string.reminders_leads_empty)
            return
        }
        settings.reminderLeads = next
        reminderLeads = next
        ReminderHelper.update(context, assignments)
    }

    fun changeNotifyProgram(enabled: Boolean) {
        settings.notifyProgram = enabled
        notifyProgram = enabled
    }

    fun changeNotifyGeneral(enabled: Boolean) {
        settings.notifyGeneral = enabled
        notifyGeneral = enabled
    }

    private fun remindersMessage(enabled: Boolean, count: Int): String = when {
        !enabled -> res.getString(R.string.reminders_off)
        count == 0 -> res.getString(R.string.reminders_on_none)
        else -> res.resources.getQuantityString(R.plurals.reminders_on_count, count, count)
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
        if (isLoggedIn && role == UserRole.STUDENT) openDialog("assignment", assignmentId)
    }

    /** Mở danh sách thông báo ở đúng luồng khi chạm vào thông báo hệ thống. */
    fun openNotifications(channel: String) {
        if (isLoggedIn && role == UserRole.STUDENT) openDialog("notifications", channel)
    }

    // ---------------------------------------------------------- hàm tiện ích cho màn hình

    fun findAssignment(id: String): Assignment? = assignments.find { it.id == id }

    fun findCourse(id: String): Course? = courses.find { it.id == id }

    fun getStudySummary(): StudySummary = LearningRules.summary(assignments, nowMs)

    fun getAssignmentStatus(assignment: Assignment): AssignmentStatus = LearningRules.status(assignment, nowMs)

    fun formatTimeRemaining(assignment: Assignment): String =
        Formatters.timeLeft(res, LearningRules.timeLeft(assignment.dueAtEpochMs, nowMs))

    fun getCourseWithCompletion(course: Course): Course = LearningRules.courseWithProgress(course, assignments)

    /** Bài chưa xong, chưa quá hạn, deadline gần nhất đứng đầu — ưu tiên của sinh viên này. */
    fun getPriorities(): List<Assignment> = LearningRules.priorities(assignments, nowMs)

    fun getOverdue(): List<Assignment> = LearningRules.overdue(assignments, nowMs)

    fun getNextDeadline(): Assignment? = LearningRules.nextDeadline(assignments, nowMs)

    /** Tiêu đề và nội dung thông báo theo ngôn ngữ đang chọn. */
    fun notificationText(notification: AppNotification): NotificationText = NotificationTexts.of(res, notification, data)
}
