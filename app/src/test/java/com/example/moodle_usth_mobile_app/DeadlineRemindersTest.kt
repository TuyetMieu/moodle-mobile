package com.example.moodle_usth_mobile_app

import com.example.moodle_usth_mobile_app.data.model.Assignment
import com.example.moodle_usth_mobile_app.data.model.AssignmentCategory
import com.example.moodle_usth_mobile_app.reminders.ReminderHelper
import com.example.moodle_usth_mobile_app.ui.dialogs.matchesSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeadlineRemindersTest {

    private val now = 1_800_000_000_000L
    private val hour = 3_600_000L

    private fun assignment(id: String, dueInHours: Double, completed: Boolean = false) = Assignment(
        id = id,
        courseId = "web",
        courseTitle = "Web Application Development",
        courseShortTitle = "Web Development",
        title = "Task $id",
        category = AssignmentCategory.FINAL_PROJECT,
        contribution = 15,
        description = "",
        dueAtEpochMs = now + (dueInHours * hour).toLong(),
        completedAtEpochMs = if (completed) now - hour else null
    )

    @Test
    fun remindsTwentyFourAndTwoHoursBeforeTheDeadline() {
        val plan = ReminderHelper.makePlan(listOf(assignment("a", 72.0)), now)
        assertEquals(listOf(48 * hour, 70 * hour), plan.map { it.triggerAtEpochMs - now })
    }

    @Test
    fun notificationTextIsComputedWhenItIsShown() {
        val due = now + 72 * hour
        assertEquals("Còn 1 ngày", ReminderHelper.remainingText(due, due - 24 * hour))
        assertEquals("Còn 2 giờ", ReminderHelper.remainingText(due, due - 2 * hour))
        // Báo thức đến muộn 40 phút so với mốc 2 giờ → câu chữ vẫn đúng.
        assertEquals("Còn 1 giờ 20 phút", ReminderHelper.remainingText(due, due - 80 * 60_000L))
        assertEquals("Còn 5 phút", ReminderHelper.remainingText(due, due - 5 * 60_000L))
        assertNull(ReminderHelper.remainingText(due, due + 1))
    }

    @Test
    fun skipsLeadTimesThatAlreadyPassed() {
        // Còn 20 giờ: mốc 24 giờ đã qua, chỉ còn mốc 2 giờ. Còn 1 giờ: không còn mốc nào.
        val plan = ReminderHelper.makePlan(listOf(assignment("soon", 20.0), assignment("now", 1.0)), now)
        assertEquals(listOf("soon"), plan.map { it.assignmentId })
        assertEquals(18 * hour, plan[0].triggerAtEpochMs - now)
    }

    @Test
    fun ignoresCompletedAndOverdueWork() {
        val plan = ReminderHelper.makePlan(listOf(assignment("done", 48.0, completed = true), assignment("late", -3.0)), now)
        assertTrue(plan.isEmpty())
    }

    @Test
    fun keepsOnlyTheNearestRemindersSortedByTime() {
        val many = (1..40).map { assignment("t$it", 30.0 + it) }
        val plan = ReminderHelper.makePlan(many, now)
        assertEquals(24, plan.size)
        assertEquals(plan.sortedBy { it.triggerAtEpochMs }, plan)
        assertEquals("t1", plan[0].assignmentId)
    }

    @Test
    fun requestCodesAreStablePerAssignmentAndLead() {
        val first = ReminderHelper.makePlan(listOf(assignment("a", 72.0)), now).map { ReminderHelper.requestCodeOf(it) }
        val again = ReminderHelper.makePlan(listOf(assignment("a", 72.0)), now + hour).map { ReminderHelper.requestCodeOf(it) }
        assertEquals(first, again)
        assertEquals(2, first.toSet().size)
    }

    @Test
    fun searchMatchesTitleAndCourseOnly() {
        val task = assignment("x", 10.0)
        assertTrue(matchesSearch(task, ""))
        assertTrue(matchesSearch(task, "  task X "))
        assertTrue(matchesSearch(task, "web dev"))
        assertTrue(matchesSearch(task, "application"))
        assertFalse(matchesSearch(task, "biology"))
        // Thành phần điểm (FINAL_PROJECT → "Final project") không được tính là khớp.
        assertFalse(matchesSearch(task, "final project"))
    }
}
