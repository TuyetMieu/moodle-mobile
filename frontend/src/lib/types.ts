export type Course = {
  id: string
  title: string
  shortTitle: string
  code: string
  teacher: string
  initials: string
  image: string
  progress: number
  done: number
  total: number
  grade: string
  credits: number
  tone: "blue" | "green" | "purple"
}

export type Assignment = {
  id: string
  courseId: string
  title: string
  /** Epoch milliseconds. */
  dueAt: number
  category: "Final project" | "Mid-term & labs"
  categoryWeight: number
  contribution: number
  description: string
  completed?: boolean
  /** Grade out of 10, present once the work has been marked. */
  score?: number
}

export type AssignmentFilter = "todo" | "urgent" | "done"
export type AssignmentStatus = "done" | "overdue" | "urgent" | "upcoming"

export type ClassSession = {
  id: string
  courseId: string
  /** 0 = Sunday, matching Date.getDay(). */
  weekday: number
  start: string
  end: string
  kind: "Lý thuyết" | "Thực hành"
  room: string
}

export type TimetableView = "week" | "month"
