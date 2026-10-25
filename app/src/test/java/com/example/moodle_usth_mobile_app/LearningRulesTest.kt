package com.example.moodle_usth_mobile_app

import com.example.moodle_usth_mobile_app.data.model.Assignment
import com.example.moodle_usth_mobile_app.data.model.AssignmentCategory
import com.example.moodle_usth_mobile_app.data.model.AssignmentStatus
import com.example.moodle_usth_mobile_app.data.model.Course
import com.example.moodle_usth_mobile_app.data.repository.LearningRules
import org.junit.Assert.assertEquals
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
        category = AssignmentCategory.MIDTERM_LABS,
        contribution = 10,
        description = "",
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
    fun timeRemainingMatchesDesignCopy() {
        assertEquals("Overdue", LearningRules.timeRemaining(assignment("a", -1.0), now))
        assertEquals("30 min left", LearningRules.timeRemaining(assignment("b", 0.5), now))
        assertEquals("6h 42m left", LearningRules.timeRemaining(assignment("c", 6.7), now))
        assertEquals("3 days left", LearningRules.timeRemaining(assignment("d", 72.0), now))
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
