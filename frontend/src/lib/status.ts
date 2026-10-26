import type { Assignment, AssignmentStatus, Course } from "./types"

const hour = 60 * 60 * 1000

export function assignmentStatus(
  assignment: Assignment,
  completed: Set<string>,
  now = Date.now(),
): AssignmentStatus {
  if (assignment.completed || completed.has(assignment.id)) return "done"
  if (assignment.dueAt <= now) return "overdue"
  return assignment.dueAt - now <= 24 * hour ? "urgent" : "upcoming"
}

export function timeRemaining(assignment: Assignment, now = Date.now()): string {
  const remaining = assignment.dueAt - now
  if (remaining <= 0) return "Overdue"
  const minutes = Math.ceil(remaining / 60000)
  if (minutes < 60) return `${minutes} min left`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}h ${minutes % 60}m left`
  return `${Math.ceil(remaining / hour / 24)} days left`
}

export function courseWithCompletion(course: Course, assignments: Assignment[], completed: Set<string>): Course {
  const extra = assignments.filter((assignment) =>
    assignment.courseId === course.id && !assignment.completed && completed.has(assignment.id)
  ).length
  const done = Math.min(course.total, course.done + extra)
  return { ...course, done, progress: course.total ? Math.round(done / course.total * 100) : 0 }
}

export function pendingAssignments(assignments: Assignment[], completed: Set<string>, courseId?: string) {
  return assignments
    .filter((assignment) => !assignment.completed && !completed.has(assignment.id) && (!courseId || assignment.courseId === courseId))
    .sort((a, b) => a.dueAt - b.dueAt)
}

export function learningSummary(courses: Course[], assignments: Assignment[], completed: Set<string>, now = Date.now()) {
  const pending = pendingAssignments(assignments, completed)
  const updatedCourses = courses.map((course) => courseWithCompletion(course, assignments, completed))
  const done = updatedCourses.reduce((sum, course) => sum + course.done, 0)
  const total = updatedCourses.reduce((sum, course) => sum + course.total, 0)
  return {
    pending: pending.length,
    urgent: pending.filter((assignment) => ["urgent", "overdue"].includes(assignmentStatus(assignment, completed, now))).length,
    recentDone: assignments.filter((assignment) => assignmentStatus(assignment, completed, now) === "done").length,
    done,
    total,
    progress: total ? Math.round(done / total * 100) : 0,
  }
}
