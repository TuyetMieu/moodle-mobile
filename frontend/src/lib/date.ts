import type { ClassSession } from "./types"

export const weekdayLabels = ["CN", "T2", "T3", "T4", "T5", "T6", "T7"]

const dueDateFormatter = new Intl.DateTimeFormat("en-GB", {
  month: "short",
  day: "numeric",
  hour: "2-digit",
  minute: "2-digit",
})

export function formatDueDate(dueAt: number): string {
  return dueDateFormatter.format(dueAt)
}

export function shiftDay(date: Date, amount: number): Date {
  const next = new Date(date)
  next.setDate(next.getDate() + amount)
  return next
}

export function sameDay(a: Date, b: Date): boolean {
  return a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() && a.getDate() === b.getDate()
}

export function weekDates(date: Date): Date[] {
  const monday = shiftDay(date, -((date.getDay() + 6) % 7))
  monday.setHours(0, 0, 0, 0)
  return Array.from({ length: 7 }, (_, index) => shiftDay(monday, index))
}

export function monthDates(date: Date): Date[] {
  const first = new Date(date.getFullYear(), date.getMonth(), 1)
  const last = new Date(date.getFullYear(), date.getMonth() + 1, 0)
  const start = weekDates(first)[0]
  const leadingDays = (first.getDay() + 6) % 7
  const cells = Math.ceil((leadingDays + last.getDate()) / 7) * 7
  return Array.from({ length: cells }, (_, index) => shiftDay(start, index))
}

export function shiftMonth(date: Date, amount: number): Date {
  const next = new Date(date)
  next.setDate(1)
  next.setMonth(next.getMonth() + amount)
  const lastDay = new Date(next.getFullYear(), next.getMonth() + 1, 0).getDate()
  next.setDate(Math.min(date.getDate(), lastDay))
  return next
}

export function sessionsForDay(sessions: ClassSession[], date: Date): ClassSession[] {
  return sessions.filter((session) => session.weekday === date.getDay())
    .sort((a, b) => a.start.localeCompare(b.start))
}

export function sessionTimestamp(date: Date, time: string): number {
  const result = new Date(date)
  const [hours, minutes] = time.split(":").map(Number)
  result.setHours(hours, minutes, 0, 0)
  return result.getTime()
}

export function sessionDuration(session: ClassSession): number {
  const minutes = (time: string) => {
    const [hours, minutes] = time.split(":").map(Number)
    return hours * 60 + minutes
  }
  return (minutes(session.end) - minutes(session.start)) / 60
}
