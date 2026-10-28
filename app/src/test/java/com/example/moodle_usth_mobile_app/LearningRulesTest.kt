package com.example.moodle_usth_mobile_app

import com.example.moodle_usth_mobile_app.data.model.Assignment
import com.example.moodle_usth_mobile_app.data.model.AssignmentStatus
import com.example.moodle_usth_mobile_app.data.model.Course
import com.example.moodle_usth_mobile_app.data.repository.LearningRules
import com.example.moodle_usth_mobile_app.data.repository.TimeLeft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LearningRulesTest {

    private val now = 1_800_000_000_000L
    private val hour = 3_600_000L

    private fun assignment(id: String, dueInHours: Double, completed: Boolean = false, courseId: String = "web") = Assignment(
        id = id,
        courseId = courseId,
        courseTitle = "Web Application Development",
        courseShortTitle = "Web Development",
        title = id,
        description = "",
        componentId = "web-labs",
        componentName = "Mid-term & labs",
        componentWeight = 30,
        contribution = 10.0,
        dueAtEpochMs = now + (dueInHours * hour).toLong(),
        completedAtEpochMs = if (completed) now - hour else null
    )

    @Test
    fun statusFollowsDeadline() {
        assertEquals(AssignmentStatus.DONE, LearningRules.status(assignment("a", -5.0, completed = true), now))
        assertEquals(AssignmentStatus.OVERDUE, LearningRules.status(assignment("b", -0.1), now))
        assertEquals(AssignmentStatus.URGENT, LearningRules.status(assignment("c", 23.9), now))
        assertEquals(AssignmentStatus.UPCOMING, LearningRules.status(assignment("d", 24.5), now))
    }

    @Test
    fun timeLeftIsSplitIntoDaysHoursAndMinutes() {
        val left = { hours: Double -> LearningRules.timeLeft(now + (hours * hour).toLong(), now) }
        assertEquals(TimeLeft(overdue = true), left(-1.0))
        assertEquals(TimeLeft(overdue = false, minutes = 30), left(0.5))
        assertEquals(TimeLeft(overdue = false, hours = 6, minutes = 42), left(6.7))
        assertEquals(TimeLeft(overdue = false, hours = 2), left(2.0))
        // Báo thức đến muộn 40 phút so với mốc 2 giờ → câu chữ vẫn đúng: "còn 1 giờ 20 phút".
        assertEquals(TimeLeft(overdue = false, hours = 1, minutes = 20), left(80 / 60.0))
        assertEquals(TimeLeft(overdue = false, days = 3), left(72.0))
        assertEquals(TimeLeft(overdue = false, days = 2), left(25.0)) // làm tròn lên
    }

    @Test
    fun prioritiesFollowTheNearestDeadlineOfUnfinishedWork() {
        val list = listOf(
            assignment("later", 72.0),
            assignment("done", 2.0, completed = true),
            assignment("soon", 5.0),
            assignment("late", -3.0),
            assignment("lateOlder", -30.0),
            assignment("tomorrow", 26.0)
        )
        assertEquals(listOf("soon", "tomorrow", "later"), LearningRules.priorities(list, now).map { it.id })
        assertEquals(listOf("late", "lateOlder"), LearningRules.overdue(list, now).map { it.id })
        assertEquals("soon", LearningRules.nextDeadline(list, now)?.id)
        // Không còn bài nào sắp tới → bài vừa quá hạn.
        assertEquals("late", LearningRules.nextDeadline(list.filter { it.dueAtEpochMs < now }, now)?.id)
        assertNull(LearningRules.nextDeadline(listOf(assignment("x", 1.0, completed = true)), now))
    }

    @Test
    fun courseProgressIsRecomputedFromAssignments() {
        val course = Course(
            id = "web", title = "", shortTitle = "", code = "", teacher = "", initials = "",
            imageResName = "development", credits = 4, tone = "blue", summary = "", grade = null
        )
        val list = listOf(
            assignment("a", -10.0, completed = true),
            assignment("b", 5.0),
            assignment("c", 50.0, completed = true),
            assignment("x", 5.0, courseId = "bio")
        )
        val updated = LearningRules.courseWithProgress(course, list)
        assertEquals(2, updated.done)
        assertEquals(3, updated.total)
        assertEquals(67, updated.progress)
    }

    @Test
    fun summaryCountsUrgentAndOverdue() {
        val summary = LearningRules.summary(
            listOf(assignment("a", -1.0), assignment("b", 3.0), assignment("c", 48.0), assignment("d", -2.0, completed = true)),
            now
        )
        assertEquals(3, summary.pendingCount)
        assertEquals(2, summary.urgentCount)
        assertEquals(1, summary.recentDoneCount)
        assertEquals(25, summary.overallProgressPercent)
    }
}
