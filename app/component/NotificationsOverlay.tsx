import { useState, useEffect, useCallback } from "react"
import { useFocusTrap } from "../lib/useFocusTrap"
import {
  AlertTriangle,
  ArrowLeft,
  BookOpen,
  ChevronRight,
  CircleCheck,
  Clock3,
  MessageSquare,
} from "lucide-react"
import { Button, Card, Text, Badge, ProgressBar } from "./ui"
import {
  CardSkeleton,
  AssignmentRowSkeleton,
  EmptyState,
  ErrorState,
} from "./states"
import {
  apiFetch,
  type NotificationsResponse,
  type CourseItem,
} from "../lib/api"
import type { ScreenProps } from "../App"
import { SectionHeading } from "../App"

type NotificationsOverlayProps = {
  close: () => void
  open: ScreenProps["open"]
  selectCourse: (course: CourseItem) => void
  completed: string[]
  now: number
}

export default function NotificationsOverlay({
  close,
  open,
  selectCourse,
  completed,
  now,
}: NotificationsOverlayProps) {
  const modalRef = useFocusTrap<HTMLDivElement>(true, close)
  const [data, setData] = useState<NotificationsResponse | null>(null)
  const [loading, setLoading] = useState<boolean>(true)
  const [error, setError] = useState<string | null>(null)

  const loadNotifications = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await apiFetch<NotificationsResponse>(
        "/api/notifications",
        completed,
        now,
      )
      setData(res)
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Unable to load notifications. Please try again.",
      )
    } finally {
      setLoading(false)
    }
  }, [completed, now])

  useEffect(() => {
    loadNotifications()
  }, [loadNotifications])

  const formatDueLabel = (dueAt: number) => {
    const diffMs = dueAt - now
    if (diffMs <= 0) {
      const overdueHours = Math.max(1, Math.floor(Math.abs(diffMs) / 3600000))
      return `Overdue · ${overdueHours}h ago`
    }
    const totalHours = Math.floor(diffMs / 3600000)
    const minutes = Math.floor((diffMs % 3600000) / 60000)
    if (totalHours < 24) {
      return `Due in ${totalHours}h ${minutes}m`
    }
    const days = Math.floor(totalHours / 24)
    return `Due in ${days}d`
  }

  const handleOpenCourse = (courseId: string) => {
    const target = data?.courses.find((c) => c.id === courseId)
    if (target) {
      close()
      selectCourse(target)
    }
  }

  return (
    <div className="overlay-screen" role="dialog" aria-label="Notifications">
      {/* HEADER */}
      <header className="overlay-header flex items-center justify-between px-4 py-3 border-b border-[var(--line)]">
        <div className="flex items-center gap-2.5">
          <Button
            variant="icon"
            aria-label="Close notifications"
            onClick={close}
          >
            <ArrowLeft size={20} />
          </Button>
          <div>
            <Text className="muted text-xs">UPDATES & ALERTS</Text>
            <Text as="h3">Notifications</Text>
          </div>
        </div>

        {data && data.urgentCount > 0 && (
          <Badge tone="coral">{data.urgentCount} urgent</Badge>
        )}
      </header>

      {/* CONTENT */}
      <div className="screen-scroll p-4 flex flex-col gap-5">
        {error ? (
          <ErrorState
            title="Failed to load notifications"
            message={error}
            retryLabel="Try again"
            onRetry={loadNotifications}
          />
        ) : loading || !data ? (
          <>
            <AssignmentRowSkeleton count={2} />
            <CardSkeleton height={140} />
            <CardSkeleton height={140} />
          </>
        ) : (
          <>
            {/* 1. DEADLINE REMINDERS (URGENT / OVERDUE) */}
            <section className="flex flex-col gap-2.5">
              <SectionHeading
                title="Deadline reminders"
                detail={String(data.reminders.length).padStart(2, "0")}
                action="All tasks"
                onAction={() => open({ type: "classwork" })}
              />

              {data.reminders.length === 0 ? (
                <EmptyState
                  icon={<CircleCheck size={26} />}
                  title="All caught up"
                  message="You have no pending deadline reminders right now."
                />
              ) : (
                <div className="flex flex-col gap-2.5">
                  {data.reminders.map((item) => {
                    const isOverdue = item.dueAt <= now
                    const isUrgent =
                      !isOverdue && item.dueAt - now <= 24 * 3600 * 1000

                    return (
                      <Card
                        key={item.id}
                        className="p-3.5 flex items-center justify-between gap-3 cursor-pointer"
                        onClick={() =>
                          open({ type: "assignment", assignment: item })
                        }
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
                                isOverdue || isUrgent
                                  ? "var(--coral-soft)"
                                  : "var(--blue-soft)",
                              color:
                                isOverdue || isUrgent
                                  ? "var(--color-coral)"
                                  : "var(--color-blue)",
                            }}
                          >
                            {isOverdue ? (
                              <AlertTriangle size={18} />
                            ) : (
                              <Clock3 size={18} />
                            )}
                          </span>

                          <div className="flex flex-col gap-1">
                            <div className="flex items-center gap-2 flex-wrap">
                              <Badge
                                tone={
                                  isOverdue
                                    ? "coral"
                                    : isUrgent
                                      ? "coral"
                                      : "blue"
                                }
                              >
                                {formatDueLabel(item.dueAt)}
                              </Badge>
                              <Text className="muted text-xs">
                                {item.course}
                              </Text>
                            </div>
                            <Text as="h4" className="font-semibold">
                              {item.title}
                            </Text>
                            <Text className="muted text-xs">
                              {item.category} · {item.contribution}% of grade
                            </Text>
                          </div>
                        </div>

                        <ChevronRight size={18} className="muted shrink-0" />
                      </Card>
                    )
                  })}
                </div>
              )}
            </section>

            {/* 2. COURSE PROGRESS */}
            <section className="flex flex-col gap-2.5">
              <SectionHeading
                title="Course progress"
                detail={String(data.courses.length).padStart(2, "0")}
              />

              <div className="flex flex-col gap-2.5">
                {data.courses.map((course) => (
                  <Card
                    key={course.id}
                    className="p-3.5 flex flex-col gap-2.5 cursor-pointer"
                    onClick={() => {
                      close()
                      selectCourse(course)
                    }}
                    role="button"
                    tabIndex={0}
                    onKeyDown={(e) => {
                      if (e.key === "Enter" || e.key === " ") {
                        e.preventDefault()
                        close()
                        selectCourse(course)
                      }
                    }}
                  >
                    <div className="flex items-center justify-between gap-2">
                      <div className="flex items-center gap-2.5">
                        <span className="subject-icon">
                          <BookOpen size={16} />
                        </span>
                        <div>
                          <Text as="h4" className="font-semibold">
                            {course.title}
                          </Text>
                          <Text className="muted text-xs">
                            {course.code} · {course.done}/{course.total} tasks
                            completed
                          </Text>
                        </div>
                      </div>
                      <Badge tone={course.tone}>{course.progress}%</Badge>
                    </div>
                    <ProgressBar value={course.progress} tone={course.tone} />
                  </Card>
                ))}
              </div>
            </section>

            {/* 3. FROM YOUR CLASSROOM */}
            <section className="flex flex-col gap-2.5">
              <SectionHeading
                title="From your classroom"
                detail={String(data.classroom.length).padStart(2, "0")}
              />

              {data.classroom.length === 0 ? (
                <EmptyState
                  title="No classroom updates"
                  message="Announcements from your instructors will appear here."
                />
              ) : (
                <div className="flex flex-col gap-2.5">
                  {data.classroom.map((post) => (
                    <Card
                      key={post.id}
                      className="p-3.5 flex items-start justify-between gap-3 cursor-pointer"
                      onClick={() => handleOpenCourse(post.courseId)}
                      role="button"
                      tabIndex={0}
                      onKeyDown={(e) => {
                        if (e.key === "Enter" || e.key === " ") {
                          e.preventDefault()
                          handleOpenCourse(post.courseId)
                        }
                      }}
                    >
                      <div className="flex items-start gap-3">
                        <span className="subject-icon shrink-0 mt-0.5">
                          <MessageSquare size={17} />
                        </span>
                        <div className="flex flex-col gap-1">
                          <div className="flex items-center gap-2 flex-wrap">
                            <Badge tone={post.tone}>{post.courseCode}</Badge>
                            <Text className="muted text-xs">
                              {post.author} · {post.time}
                            </Text>
                          </div>
                          <Text as="h4" className="font-semibold">
                            {post.title}
                          </Text>
                          <Text className="muted text-xs">{post.body}</Text>
                        </div>
                      </div>
                      <ChevronRight size={18} className="muted shrink-0 mt-1" />
                    </Card>
                  ))}
                </div>
              )}
            </section>
          </>
        )}
      </div>
    </div>
  )
}