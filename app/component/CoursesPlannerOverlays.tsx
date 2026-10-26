import { useState, useEffect, useCallback } from "react"
import { useFocusTrap } from "../lib/useFocusTrap"
import {
  ArrowLeft,
  BookOpen,
  CalendarRange,
  ChevronRight,
  CircleCheck,
  Clock3,
} from "lucide-react"
import { Button, Card, Text, Badge, ProgressBar } from "./ui"
import {
  CourseCardSkeleton,
  AssignmentRowSkeleton,
  EmptyState,
  ErrorState,
} from "./states"
import {
  apiFetch,
  type CourseItem,
  type AssignmentItem,
} from "../lib/api"
import type { ScreenProps } from "../App"
import { SectionHeading } from "../App"
import { CourseCard } from "../screens/CoursesScreen"

/* ==========================================================================
   1. YOUR COURSES OVERLAY (Opened from "See all" on HomeScreen)
   ========================================================================== */

type YourCoursesOverlayProps = {
  close: () => void
  open: ScreenProps["open"]
  selectCourse: (course: CourseItem) => void
  completed: string[]
  now: number
}

export function YourCoursesOverlay({
  close,
  open,
  selectCourse,
  completed,
  now,
}: YourCoursesOverlayProps) {
  const modalRef = useFocusTrap<HTMLDivElement>(true, close)
  const [courses, setCourses] = useState<CourseItem[]>([])
  const [loading, setLoading] = useState<boolean>(true)
  const [error, setError] = useState<string | null>(null)

  const loadCourses = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await apiFetch<{ courses: CourseItem[] }>(
        "/api/courses",
        completed,
        now,
      )
      setCourses(res.courses)
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Unable to load courses. Please try again.",
      )
    } finally {
      setLoading(false)
    }
  }, [completed, now])

  useEffect(() => {
    loadCourses()
  }, [loadCourses])

  const totalCredits = courses.reduce((sum, c) => sum + c.credits, 0)
  const totalDone = courses.reduce((sum, c) => sum + c.done, 0)
  const totalTasks = courses.reduce((sum, c) => sum + c.total, 0)
  const overallProgress =
    totalTasks > 0 ? Math.round((totalDone / totalTasks) * 100) : 0

  return (
    <div className="overlay-screen" role="dialog" aria-label="Your courses">
      <div
        ref={modalRef}
        className="overlay-screen"
        role="dialog"
        aria-modal="true"
        aria-label="Your courses"
        tabIndex={-1}
      ></div>
      {/* HEADER */}
      <header className="overlay-header flex items-center justify-between px-4 py-3 border-b border-[var(--line)]">
        <div className="flex items-center gap-2.5">
          <Button
            variant="icon"
            aria-label="Close your courses"
            onClick={close}
          >
            <ArrowLeft size={20} />
          </Button>
          <div>
            <Text className="muted text-xs">SEMESTER 2 · 2024–2025</Text>
            <Text as="h3">Your courses</Text>
          </div>
        </div>

        <Button
          variant="default"
          onClick={() => open({ type: "planner" })}
          aria-label="Open weekly planner"
        >
          <CalendarRange size={15} />
          Planner
        </Button>
      </header>

      {/* BODY */}
      <div className="screen-scroll p-4 flex flex-col gap-4">
        {error ? (
          <ErrorState
            title="Failed to load courses"
            message={error}
            retryLabel="Try again"
            onRetry={loadCourses}
          />
        ) : loading ? (
          <CourseCardSkeleton count={3} />
        ) : courses.length === 0 ? (
          <EmptyState
            icon={<BookOpen size={28} />}
            title="No courses found"
            message="You have no enrolled courses for this semester."
          />
        ) : (
          <>
            {/* SEMESTER SUMMARY BANNER */}
            <Card className="p-4 flex flex-col gap-2.5">
              <div className="flex items-center justify-between">
                <div>
                  <Text className="eyebrow">OVERALL PROGRESS</Text>
                  <Text as="h4" className="font-semibold">
                    {courses.length} Enrolled Subjects · {totalCredits} ECTS
                  </Text>
                </div>
                <Badge tone="blue">{overallProgress}%</Badge>
              </div>
              <ProgressBar value={overallProgress} tone="blue" />
              <Text className="muted text-xs">
                {totalDone} of {totalTasks} coursework tasks completed across
                all subjects
              </Text>
            </Card>

            {/* COURSES LIST */}
            <div className="home-courses flex flex-col gap-3.5">
              {courses.map((course) => (
                <CourseCard
                  key={course.id}
                  course={course}
                  onClick={() => {
                    close()
                    selectCourse(course)
                  }}
                />
              ))}
            </div>
          </>
        )}
      </div>
    </div>
  )
}

/* ==========================================================================
   2. WEEKLY PLANNER OVERLAY (Upcoming deadlines grouped by timeframe)
   ========================================================================== */

type WeeklyPlannerOverlayProps = {
  close: () => void
  open: ScreenProps["open"]
  completed: string[]
  now: number
}

export function WeeklyPlannerOverlay({
  close,
  open,
  completed,
  now,
}: WeeklyPlannerOverlayProps) {
  const [assignments, setAssignments] = useState<AssignmentItem[]>([])
  const [loading, setLoading] = useState<boolean>(true)
  const [error, setError] = useState<string | null>(null)

  const loadPlanner = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await apiFetch<{ assignments: AssignmentItem[] }>(
        "/api/assignments",
        completed,
        now,
      )
      setAssignments(res.assignments)
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Unable to load weekly planner. Please try again.",
      )
    } finally {
      setLoading(false)
    }
  }, [completed, now])

  useEffect(() => {
    loadPlanner()
  }, [loadPlanner])

  const pending = assignments
    .filter((a) => !a.completed && !completed.includes(a.id))
    .sort((a, b) => a.dueAt - b.dueAt)

  const dayMs = 24 * 3600 * 1000
  const urgentGroup = pending.filter((a) => a.dueAt - now <= dayMs)
  const thisWeekGroup = pending.filter(
    (a) => a.dueAt - now > dayMs && a.dueAt - now <= 7 * dayMs,
  )
  const laterGroup = pending.filter((a) => a.dueAt - now > 7 * dayMs)

  const formatDeadlineDate = (dueAt: number) => {
    const date = new Date(dueAt)
    const weekday = date.toLocaleDateString("en-US", { weekday: "short" })
    const monthDay = date.toLocaleDateString("en-US", {
      month: "short",
      day: "numeric",
    })
    const time = date.toLocaleTimeString("en-US", {
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
    })
    return `${weekday}, ${monthDay} · ${time}`
  }

  const formatCountdownTag = (dueAt: number) => {
    const diff = dueAt - now
    if (diff <= 0) return "Overdue"
    const hours = Math.floor(diff / 3600000)
    if (hours < 24) return `${hours}h left`
    const days = Math.ceil(hours / 24)
    return `${days}d left`
  }

  const renderGroup = (
    title: string,
    items: AssignmentItem[],
    badgeTone: "coral" | "blue" | "green",
  ) => {
    if (items.length === 0) return null
    return (
      <section className="flex flex-col gap-2.5">
        <SectionHeading
          title={title}
          detail={String(items.length).padStart(2, "0")}
        />
        <div className="flex flex-col gap-2.5">
          {items.map((item) => (
            <Card
              key={item.id}
              className="p-3.5 flex items-center justify-between gap-3 cursor-pointer"
              onClick={() => open({ type: "assignment", assignment: item })}
              role="button"
              tabIndex={0}
              onKeyDown={(e) => {
                if (e.key === "Enter" || e.key === " ") {
                  e.preventDefault()
                  open({ type: "assignment", assignment: item })
                }
              }}
            >
              <div className="flex items-start gap-3">
                <span
                  className="subject-icon shrink-0 mt-0.5"
                  style={{
                    background:
                      badgeTone === "coral"
                        ? "var(--coral-soft)"
                        : "var(--blue-soft)",
                    color:
                      badgeTone === "coral"
                        ? "var(--color-coral)"
                        : "var(--color-blue)",
                  }}
                >
                  <Clock3 size={18} />
                </span>

                <div className="flex flex-col gap-1">
                  <div className="flex items-center gap-2 flex-wrap">
                    <Badge tone={badgeTone}>
                      {formatCountdownTag(item.dueAt)}
                    </Badge>
                    <Text className="muted text-xs">{item.course}</Text>
                  </div>
                  <Text as="h4" className="font-semibold">
                    {item.title}
                  </Text>
                  <Text className="muted text-xs">
                    {formatDeadlineDate(item.dueAt)} · {item.contribution}% of
                    grade
                  </Text>
                </div>
              </div>

              <ChevronRight size={18} className="muted shrink-0" />
            </Card>
          ))}
        </div>
      </section>
    )
  }

  return (
    <div className="overlay-screen" role="dialog" aria-label="Weekly planner">
      {/* HEADER */}
      <header className="overlay-header flex items-center justify-between px-4 py-3 border-b border-[var(--line)]">
        <div className="flex items-center gap-2.5">
          <Button
            variant="icon"
            aria-label="Close weekly planner"
            onClick={close}
          >
            <ArrowLeft size={20} />
          </Button>
          <div>
            <Text className="muted text-xs">DEADLINE SCHEDULE</Text>
            <Text as="h3">Weekly planner</Text>
          </div>
        </div>

        <Badge tone={urgentGroup.length > 0 ? "coral" : "blue"}>
          {pending.length} pending
        </Badge>
      </header>

      {/* BODY */}
      <div className="screen-scroll p-4 flex flex-col gap-5">
        {error ? (
          <ErrorState
            title="Failed to load weekly planner"
            message={error}
            retryLabel="Try again"
            onRetry={loadPlanner}
          />
        ) : loading ? (
          <AssignmentRowSkeleton count={4} />
        ) : pending.length === 0 ? (
          <EmptyState
            icon={<CircleCheck size={28} />}
            title="Clear schedule!"
            message="All assignments for this week are completed. Enjoy your free time!"
            actionLabel="View completed work"
            onAction={() => open({ type: "classwork", filter: "done" })}
          />
        ) : (
          <>
            {renderGroup("Urgent · Next 24 hours", urgentGroup, "coral")}
            {renderGroup("This week · Next 7 days", thisWeekGroup, "blue")}
            {renderGroup("Later upcoming", laterGroup, "green")}
          </>
        )}
      </div>
    </div>
  )
}