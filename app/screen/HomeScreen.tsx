import { useState, useEffect } from "react"

import {
  ArrowRight,
  BookOpen,
  CalendarDays,
  ChevronRight,
  CircleCheck,
  Clock3,
  ListChecks,
  Sparkles,
} from "lucide-react"

import { Button, Card, Text, Badge, ProgressBar } from "../components/ui"

import type { ScreenProps } from "../App"

import { CourseCard, AssignmentRow, SectionHeading } from "../App" // Import lại từ App.tsx nếu chưa tách

import {
  courses,
  assignments,
  courseWithCompletion,
  pendingAssignments,
  learningSummary,
  assignmentStatus,
} from "../App"

// 1. Component Countdown

export function Countdown({ dueAt }: { dueAt: number }) {
  const remaining = () => Math.max(0, Math.ceil((dueAt - Date.now()) / 1000))

  const [seconds, setSeconds] = useState(remaining)

  useEffect(() => {
    setSeconds(remaining())

    const timer = window.setInterval(() => setSeconds(remaining()), 1000)

    return () => window.clearInterval(timer)
  }, [dueAt])

  const parts = [
    { value: Math.floor(seconds / 3600), label: "HRS" },

    { value: Math.floor((seconds % 3600) / 60), label: "MIN" },

    { value: seconds % 60, label: "SEC" },
  ]

  return (
    <div
      className="countdown"
      aria-label={`${parts[0].value} hours ${parts[1].value} minutes remaining`}
    >
      {parts.map((part, index) => (
        <div className="countdown-part" key={part.label}>
          {index > 0 && <span className="countdown-colon">:</span>}
          <div>
            <Text className="countdown-value">
              {String(part.value).padStart(2, "0")}
            </Text>
            <Text className="countdown-label">{part.label}</Text>
          </div>
        </div>
      ))}
    </div>
  )
}

// 2. Component StudyOverview

export function StudyOverview({
  open,
  completed,
  now,
}: Pick<ScreenProps, "open" | "completed" | "now">) {
  const summary = learningSummary(completed, now)

  return (
    <Card className="study-overview">
      <div className="study-overview-heading">
        <div className="avatar avatar--student">
          <span></span>A
        </div>
        <div>
          <Text className="eyebrow">STUDY OVERVIEW</Text>
          <Text as="h3">Coursework</Text>
        </div>
      </div>
      <div className="study-overview-stats">
        <Button
          variant="unstyled"
          className="study-stat"
          onClick={() => open({ type: "classwork" })}
        >
          <span>
            <span>{summary.pending}</span>
            <ListChecks size={13} />
          </span>
          <span>To do</span>
        </Button>
        <Button
          variant="unstyled"
          className={`study-stat ${
            summary.urgent > 0 ? "study-stat--urgent" : ""
          }`}
          onClick={() => open({ type: "classwork", filter: "urgent" })}
        >
          <span>
            <span>{summary.urgent}</span>
            <Clock3 size={13} />
          </span>
          <span>Due soon</span>
        </Button>
        <Button
          variant="unstyled"
          className="study-stat study-stat--done"
          onClick={() => open({ type: "classwork", filter: "done" })}
        >
          <span>
            <span>{summary.recentDone}</span>
            <CircleCheck size={13} />
          </span>
          <span>Done</span>
        </Button>
      </div>
    </Card>
  )
}

// 3. Component HomeScreen chính

export default function HomeScreen({
  open,
  selectCourse,
  completed,
  now,
}: ScreenProps) {
  const pending = pendingAssignments(completed, undefined, now)

  const next = pending[0]

  const nextStatus = next ? assignmentStatus(next, completed, now) : "done"

  return (
    <>
      <header className="app-header">
        <div className="greeting">
          <div>
            <Text className="muted greeting-small">SEMESTER 2 · 2024–2025</Text>
            <Text as="h2">
              Hi, An <span className="greeting-sun">👋</span>
            </Text>
          </div>
        </div>
        <Button
          variant="icon"
          className="notification-button"
          aria-label="Notifications"
          onClick={() => open({ type: "notifications" })}
        >
          <CalendarDays size={24} />
          {learningSummary(completed, now).urgent > 0 && (
            <span className="notification-dot" />
          )}
        </Button>

        <Button
          variant="icon"
          className="notification-button relative"
          aria-label={
            (summary?.urgent ?? 0) > 0
              ? `Notifications, ${summary?.urgent} urgent assignments`
              : "Notifications"
          }
          onClick={() => open({ type: "notifications" })}
        >
          <CalendarDays size={24} />
          {(summary?.urgent ?? 0) > 0 && (
            <span className="notification-dot flex items-center justify-center text-[10px] font-bold text-white">
              {summary?.urgent}
            </span>
          )}
        </Button>
      </header>

      <div className="screen-scroll home-content">
        <StudyOverview open={open} completed={completed} now={now} />

        <Card className={`deadline-card ${!next ? "deadline-card--done" : ""}`}>
          <div className="deadline-content">
            <div className="deadline-kicker">
              {nextStatus === "overdue" ? (
                <>
                  <span className="live-dot" />
                  ACTION REQUIRED
                </>
              ) : (
                <>
                  <Clock3 size={10} /> NEXT DEADLINE
                </>
              )}
            </div>
            <Text as="h3">{!next ? "All caught up, An." : next.title}</Text>
            {!next ? (
              <div className="deadline-done">
                <CircleCheck size={29} />
                <Text>Every assignment is marked as done.</Text>
              </div>
            ) : nextStatus === "overdue" ? (
              <Text className="overdue-countdown">Overdue · start here</Text>
            ) : (
              <Countdown key={next.id} dueAt={next.dueAt} />
            )}
            <Button
              variant="text"
              className="deadline-link"
              onClick={() =>
                next
                  ? open({ type: "assignment", assignment: next })
                  : open({ type: "classwork", filter: "done" })
              }
            >
              {next
                ? `${next.contribution}% of course grade · View task`
                : "View completed work"}{" "}
              <ArrowRight size={14} />
            </Button>
          </div>
          <img src="/images/deadline.webp" alt="" />
          <Badge
            tone={
              !next ? "green" : nextStatus === "upcoming" ? "blue" : "coral"
            }
            className="deadline-badge"
          >
            {!next
              ? "Done"
              : nextStatus === "overdue"
                ? "Overdue"
                : nextStatus === "urgent"
                  ? "Due within 24h"
                  : "Coming up"}
          </Badge>
        </Card>

        <section className="up-next-section">
          <SectionHeading
            title="Up next"
            detail={String(pending.length).padStart(2, "0")}
            action="All assignments"
            onAction={() => open({ type: "classwork" })}
          />
          <div className="upcoming-list">
            {pending.slice(0, 2).map((assignment) => (
              <AssignmentRow
                key={assignment.id}
                assignment={assignment}
                done={false}
                now={now}
                showCourse
                onClick={() => open({ type: "assignment", assignment })}
              />
            ))}
            {pending.length === 0 && (
              <div className="classwork-empty">
                <CircleCheck size={24} />
                <Text>No assignments left. Enjoy the breathing room.</Text>
              </div>
            )}
          </div>
        </section>

        <section className="courses-section">
          <SectionHeading
            title="Your courses"
            detail="03"
            action="See all"
            onAction={() => open({ type: "courses" })}
          />
          <div className="home-courses">
            {courses.map((course) => (
              <CourseCard
                key={course.id}
                course={courseWithCompletion(course, completed)}
                onClick={() => selectCourse(course)}
              />
            ))}
          </div>
        </section>

        <div className="little-note">
          <Sparkles size={13} />
          <Text>A little progress, every single day.</Text>
        </div>
      </div>
    </>
  )
}
