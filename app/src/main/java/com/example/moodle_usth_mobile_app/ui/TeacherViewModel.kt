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
import com.example.moodle_usth_mobile_app.data.local.LocaleHelper
import com.example.moodle_usth_mobile_app.data.local.SessionStore
import com.example.moodle_usth_mobile_app.data.model.PostAudience
import com.example.moodle_usth_mobile_app.data.model.SchemeComponent
import com.example.moodle_usth_mobile_app.data.model.TeacherData
import com.example.moodle_usth_mobile_app.data.model.TeachingAssignment
import com.example.moodle_usth_mobile_app.data.model.TeachingCourse
import com.example.moodle_usth_mobile_app.data.remote.ApiException
import com.example.moodle_usth_mobile_app.data.remote.MoodleApi
import com.example.moodle_usth_mobile_app.data.repository.AssignmentDraft
import com.example.moodle_usth_mobile_app.data.repository.PostDraft
import com.example.moodle_usth_mobile_app.data.repository.TeacherRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 3 tab của giáo viên. */
enum class TeacherDestination { COURSES, POSTS, PROFILE }

/** 4 tab trong trang một môn của giáo viên. */
enum class TeacherCourseTab { ASSIGNMENTS, GRADING, STUDENTS, STREAM }

/**
 * Dữ liệu và thao tác của giáo viên: lớp đang dạy, thang điểm, bài tập, đăng tin.
 * Điểm sinh viên do máy chấm (server), giáo viên chỉ xem.
 */
class TeacherViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application
    private val session = SessionStore(application)
    private val repository = TeacherRepository(MoodleApi(session))

    private val res: Context get() = LocaleHelper.localized(context)
    private val serverUrl: String get() = session.baseUrl

    var data by mutableStateOf<TeacherData?>(null)
    var isLoading by mutableStateOf(false)
    var isRefreshing by mutableStateOf(false)
    var loadError by mutableStateOf<String?>(null)
    var message by mutableStateOf<String?>(null)

    /** true khi đang chờ server lưu (khoá nút Lưu/Đăng). */
    var isSaving by mutableStateOf(false)

    /** Server trả 401 → MainActivity đưa về màn đăng nhập. */
    var sessionExpired by mutableStateOf(false)

    var destination by mutableStateOf(TeacherDestination.COURSES)
    var selectedCourseId by mutableStateOf<String?>(null)
    var courseTab by mutableStateOf(TeacherCourseTab.ASSIGNMENTS)

    /** Form bài tập đang mở: null = đóng; "" = tạo bài mới; còn lại = id bài đang sửa. */
    var assignmentEditor by mutableStateOf<String?>(null)

    /** Form đăng tin đang mở (null = đóng) và đối tượng nhận mặc định. */
    var composerAudience by mutableStateOf<PostAudience?>(null)
    var composerCourseId by mutableStateOf<String?>(null)

    var nowMs by mutableLongStateOf(System.currentTimeMillis())

    val courses: List<TeachingCourse> get() = data?.courses ?: emptyList()
    val selectedCourse: TeachingCourse? get() = courses.find { it.id == selectedCourseId }

    init {
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                nowMs = System.currentTimeMillis()
            }
        }
    }

    // ---------------------------------------------------------- tải dữ liệu

    fun load(initial: Boolean = false) {
        if (isLoading || isRefreshing) return
        if (initial || data == null) isLoading = true else isRefreshing = true
        loadError = null
        viewModelScope.launch {
            try {
                data = repository.load()
                nowMs = System.currentTimeMillis()
            } catch (error: Exception) {
                if (!handleExpired(error)) {
                    if (data == null) loadError = ErrorTexts.of(res, error, serverUrl) else message = ErrorTexts.of(res, error, serverUrl)
                }
            }
            isLoading = false
            isRefreshing = false
        }
    }

    /** Xoá dữ liệu khi đăng xuất. */
    fun clear() {
        data = null
        loadError = null
        sessionExpired = false
        destination = TeacherDestination.COURSES
        selectedCourseId = null
        assignmentEditor = null
        composerAudience = null
    }

    // ---------------------------------------------------------- điều hướng

    fun navigateTo(target: TeacherDestination) {
        destination = target
        selectedCourseId = null
    }

    fun openCourse(courseId: String?) {
        selectedCourseId = courseId
        courseTab = TeacherCourseTab.ASSIGNMENTS
    }

    fun openComposer(audience: PostAudience, courseId: String? = null) {
        composerCourseId = courseId ?: selectedCourseId ?: courses.firstOrNull()?.id
        composerAudience = audience
    }

    fun findAssignment(courseId: String, assignmentId: String): TeachingAssignment? =
        courses.find { it.id == courseId }?.assignments?.find { it.id == assignmentId }

    // ---------------------------------------------------------- thao tác

    fun saveGrading(courseId: String, components: List<SchemeComponent>, bonusEnabled: Boolean, bonusMax: Double) {
        runSaving(onSuccess = { message = res.getString(R.string.teacher_msg_grading_saved) }) {
            replaceCourse(repository.saveGrading(courseId, components, bonusEnabled, bonusMax))
        }
    }

    /** assignmentId = null → tạo mới. */
    fun saveAssignment(courseId: String, assignmentId: String?, draft: AssignmentDraft) {
        runSaving(onSuccess = {
            assignmentEditor = null
            message = res.getString(if (assignmentId == null) R.string.teacher_msg_assignment_created else R.string.teacher_msg_assignment_saved)
        }) {
            val course = if (assignmentId == null) repository.createAssignment(courseId, draft)
            else repository.updateAssignment(assignmentId, draft)
            replaceCourse(course)
        }
    }

    fun deleteAssignment(assignmentId: String) {
        runSaving(onSuccess = {
            assignmentEditor = null
            message = res.getString(R.string.teacher_msg_assignment_deleted)
        }) {
            replaceCourse(repository.deleteAssignment(assignmentId))
        }
    }

    fun autoGrade(assignmentId: String) {
        var graded = 0
        runSaving(onSuccess = {
            message = if (graded == 0) res.getString(R.string.teacher_msg_nothing_to_grade)
            else res.resources.getQuantityString(R.plurals.teacher_msg_graded, graded, graded)
        }) {
            val result = repository.autoGrade(assignmentId)
            graded = result.first
            replaceCourse(result.second)
        }
    }

    fun post(draft: PostDraft) {
        runSaving(onSuccess = {
            composerAudience = null
            message = res.getString(R.string.teacher_msg_posted)
        }) {
            val result = repository.post(draft)
            val current = data ?: return@runSaving
            var updated = current.copy(posts = result.posts)
            if (result.course != null) updated = updated.copy(courses = updated.courses.map { if (it.id == result.course.id) result.course else it })
            data = updated
        }
    }

    fun deletePost(postId: String) {
        runSaving(onSuccess = { message = res.getString(R.string.teacher_msg_post_deleted) }) {
            data = repository.deletePost(postId)
        }
    }

    private fun replaceCourse(course: TeachingCourse) {
        val current = data ?: return
        data = current.copy(courses = current.courses.map { if (it.id == course.id) course else it })
    }

    /** Chạy một thao tác lưu: khoá nút, báo lỗi bằng snackbar, 401 → về màn đăng nhập. */
    private fun runSaving(onSuccess: () -> Unit, work: suspend () -> Unit) {
        if (isSaving) return
        viewModelScope.launch {
            isSaving = true
            try {
                work()
                onSuccess()
            } catch (error: Exception) {
                if (!handleExpired(error)) message = ErrorTexts.of(res, error, serverUrl)
            }
            isSaving = false
        }
    }

    private fun handleExpired(error: Exception): Boolean {
        if (error is ApiException && error.status == 401) {
            sessionExpired = true
            return true
        }
        return false
    }
}
