package com.example.moodle_usth_mobile_app.data.repository

import com.example.moodle_usth_mobile_app.data.model.PostAudience
import com.example.moodle_usth_mobile_app.data.model.PostKind
import com.example.moodle_usth_mobile_app.data.model.SchemeComponent
import com.example.moodle_usth_mobile_app.data.model.TeacherData
import com.example.moodle_usth_mobile_app.data.model.TeacherPost
import com.example.moodle_usth_mobile_app.data.model.TeachingCourse
import com.example.moodle_usth_mobile_app.data.remote.JsonMapper
import com.example.moodle_usth_mobile_app.data.remote.MoodleApi
import org.json.JSONArray
import org.json.JSONObject

/** Nội dung bài tập giáo viên nhập trong form. componentId = null nghĩa là bài bonus. */
data class AssignmentDraft(
    val title: String,
    val description: String,
    val componentId: String?,
    val weight: Double,
    val dueAtEpochMs: Long
)

/** Nội dung tin giáo viên soạn. courseId chỉ dùng khi audience = COURSE. */
data class PostDraft(
    val audience: PostAudience,
    val courseId: String?,
    val kind: PostKind,
    val title: String,
    val body: String,
    val eventAtEpochMs: Long?,
    val location: String?,
    val pinned: Boolean
)

/** Kết quả đăng tin: môn học (nếu đăng cho lớp) và danh sách tin đã đăng. */
class PostResult(val course: TeachingCourse?, val posts: List<TeacherPost>)

/**
 * Dữ liệu của giáo viên. Mỗi thao tác sửa trả về môn học mới nhất từ server
 * để màn hình cập nhật ngay (điểm, số bài, đóng góp % đều do server tính).
 */
class TeacherRepository(private val api: MoodleApi) {

    private val mapper = JsonMapper()

    suspend fun load(): TeacherData = mapper.teacherData(api.teacherBootstrap())

    suspend fun saveGrading(courseId: String, components: List<SchemeComponent>, bonusEnabled: Boolean, bonusMax: Double): TeachingCourse {
        val list = JSONArray()
        for (component in components) {
            val item = JSONObject()
            if (component.id != null) item.put("id", component.id)
            item.put("name", component.name.trim())
            item.put("weight", component.weight)
            list.put(item)
        }
        val body = JSONObject()
        body.put("components", list)
        body.put("bonusEnabled", bonusEnabled)
        if (bonusEnabled) body.put("bonusMax", bonusMax)
        return mapper.teachingCourse(api.updateGrading(courseId, body))
    }

    suspend fun createAssignment(courseId: String, draft: AssignmentDraft): TeachingCourse =
        mapper.teachingCourse(api.createAssignment(courseId, assignmentJson(draft)))

    suspend fun updateAssignment(assignmentId: String, draft: AssignmentDraft): TeachingCourse =
        mapper.teachingCourse(api.updateAssignment(assignmentId, assignmentJson(draft)))

    suspend fun deleteAssignment(assignmentId: String): TeachingCourse =
        mapper.teachingCourse(api.deleteAssignment(assignmentId))

    /** Trả về số bài vừa được chấm và môn học sau khi chấm. */
    suspend fun autoGrade(assignmentId: String): Pair<Int, TeachingCourse> {
        val json = api.autoGrade(assignmentId)
        return Pair(json.getInt("graded"), mapper.teachingCourse(json.getJSONObject("course")))
    }

    suspend fun post(draft: PostDraft): PostResult {
        val body = JSONObject()
        body.put("audience", draft.audience.name)
        if (draft.audience == PostAudience.COURSE) body.put("courseId", draft.courseId)
        body.put("kind", draft.kind.name)
        body.put("title", draft.title.trim())
        body.put("body", draft.body.trim())
        body.put("pinned", draft.pinned)
        if (draft.kind == PostKind.EVENT) {
            body.put("eventAt", draft.eventAtEpochMs)
            if (!draft.location.isNullOrBlank()) body.put("location", draft.location.trim())
        }
        val json = api.postAnnouncement(body)
        val course = json.optJSONObject("course")?.let { mapper.teachingCourse(it) }
        val posts = mutableListOf<TeacherPost>()
        val array = json.getJSONArray("posts")
        for (i in 0 until array.length()) posts.add(mapper.teacherPost(array.getJSONObject(i)))
        return PostResult(course, posts)
    }

    suspend fun deletePost(postId: String): TeacherData = mapper.teacherData(api.deletePost(postId))

    private fun assignmentJson(draft: AssignmentDraft): JSONObject {
        val body = JSONObject()
        body.put("title", draft.title.trim())
        body.put("description", draft.description.trim())
        if (draft.componentId == null) body.put("isBonus", true) else body.put("componentId", draft.componentId)
        body.put("weight", draft.weight)
        body.put("dueAt", draft.dueAtEpochMs)
        return body
    }
}
