import { useEffect, useRef, useState, type ReactNode } from "react"
import { createPortal } from "react-dom"
import {
  ArrowDownToLine,
  ArrowLeft,
  ArrowRight,
  ArrowUpRight,
  Award,
  Bell,
  BookOpen,
  CalendarDays,
  ChartNoAxesCombined,
  Check,
  ChevronDown,
  ChevronRight,
  CircleCheck,
  Clock3,
  Code2,
  Dna,
  FileText,
  Globe2,
  GraduationCap,
  House,
  Info,
  ListChecks,
  MessageCircle,
  MoreHorizontal,
  Pin,
  Sparkles,
  Target,
  TrendingUp,
  RotateCw,
  TriangleAlert,
  UserRound,
  WifiOff,
} from "lucide-react"
import {
  Badge,
  Button,
  Card,
  Modal,
  ProgressBar,
  ProgressRing,
  Text,
} from "./components/ui"
import TimetableScreen from "./components/TimetableScreen"
import { formatDueDate, weekdayLabels } from "./lib/date"
import { api, ApiError } from "./lib/api"
import { LearningDataContext, loadLearningData, useLearningData } from "./lib/learning-data"
import { assignmentStatus, courseWithCompletion, learningSummary, pendingAssignments, timeRemaining } from "./lib/status"
import type { Assignment, AssignmentFilter, Course, TimetableView } from "./lib/types"
import { useApi } from "./lib/useApi"
import { useOnlineStatus } from "./lib/useOnlineStatus"

type Toast = {
  message: string
  tone?: "error"
  action?: { label: string; onClick: () => void }
}

/**
 * Modals use <dialog>.showModal(), which sits in the browser's top layer and makes everything
 * outside the dialog inert. Toasts and banners are therefore rendered inside the open dialog
 * (or <body>) as manual popovers, so they stay visible and clickable over a modal.
 * Changing `raise` re-attaches them after a dialog opens or closes.
 */
function TopLayer({ className, role, raise, children }: {
  className: string
  role: "alert" | "status"
  raise?: string
  children: ReactNode
}) {
  const ref = useRef<HTMLDivElement>(null)
  const [host, setHost] = useState<Element | null>(null)
  useEffect(() => {
    // After this commit's effects, so a dialog opened in the same commit is already open.
    queueMicrotask(() => setHost(document.querySelector("dialog[open]") ?? document.body))
  }, [raise])
  useEffect(() => {
    const element = ref.current
    if (element?.isConnected && !element.matches(":popover-open")) element.showPopover()
  }, [host])
  if (!host) return null
  return createPortal(<div ref={ref} popover="manual" className={className} role={role}>{children}</div>, host)
}

function OfflineBanner({ raise }: { raise?: string }) {
  const online = useOnlineStatus()
  if (online) return null
  return (
    <TopLayer className="offline-banner" role="alert" raise={raise}>
      <WifiOff size={15} />
      <Text>Mất kết nối. Dữ liệu sẽ tự cập nhật khi có mạng trở lại.</Text>
    </TopLayer>
  )
}

type View = "home" | "courses" | "course" | "timetable" | "tracker" | "profile"
type Overlay = {
  type: "assignment"
  assignment: Assignment
} | {
  type: "classwork"
  filter?: AssignmentFilter
} | {
  type: "notifications" | "courses" | "calendar" | "semester" | "grades" | "course-info"
} | null

function CourseIcon({ id, size = 19 }: {
  id: string
  size?: number
}) {
  return id === "bio" ? (
    <Dna size={size} />
  ) : id === "earth" ? (
    <Globe2 size={size} />
  ) : (
    <Code2 size={size} />
  )
}

function BottomNav({
  view,
  onNavigate,
}: {
  view: View
  onNavigate: (view: View) => void
}) {
  const items = [
    { id: "home" as const, label: "Home", icon: House },
    { id: "courses" as const, label: "Courses", icon: BookOpen },
    { id: "timetable" as const, label: "Lịch học", icon: CalendarDays },
    { id: "tracker" as const, label: "Tracker", icon: ChartNoAxesCombined },
    { id: "profile" as const, label: "Profile", icon: UserRound },
  ]
  return (
    <nav className="bottom-nav" aria-label="App navigation">
      <div className="nav-items">
        {items.map(({ id, label, icon: Icon }) => {
          const active = view === id || (id === "courses" && view === "course")
          return (
          <Button
            key={id}
            variant="unstyled"
            className={`nav-item ${active ? "is-active" : ""}`}
            aria-label={id === "timetable" ? "Thời khóa biểu" : label}
            onClick={() => onNavigate(id)}
            aria-current={active ? "page" : undefined}
          >
            <span className="nav-icon">
              <Icon size={24} strokeWidth={active ? 2.2 : 1.8} />
            </span>
            <Text>{label}</Text>
          </Button>
          )
        })}
      </div>
    </nav>
  )
}

function SectionHeading({
  title,
  action,
  onAction,
  detail,
}: {
  title: string
  action?: string
  onAction?: () => void
  detail?: string
}) {
  return (
    <div className="section-heading">
      <Text as="h3">
        {title}
        {detail && <Text className="heading-count">{detail}</Text>}
      </Text>
      {action && (
        <Button variant="text" onClick={onAction}>
          {action}
          <ChevronRight size={13} />
        </Button>
      )}
    </div>
  )
}

function CourseCard({
  course,
  onClick,
}: {
  course: Course
  onClick: () => void
}) {
  return (
    <Button
      variant="unstyled"
      className={`course-card course-card--${course.tone}`}
      onClick={onClick}
    >
      <img src={`/images/${course.image}.webp`} alt="" />
      <div className="course-card-shade" />
      <div className="course-card-content">
        <div className="course-card-top">
          <Text>
            {course.code} · {course.credits} CREDITS
          </Text>
          <ArrowUpRight size={16} />
        </div>
        <Text as="h4">{course.title}</Text>
        <Text className="course-card-teacher">{course.teacher}</Text>
        <div className="course-card-progress">
          <ProgressBar value={course.progress} tone="white" />
          <Text>{course.progress}%</Text>
        </div>
      </div>
      <div className="classroom-card-footer">
        <Text><CircleCheck size={13} />{course.done}/{course.total} completed</Text>
        <Text className={course.total > course.done ? "classroom-todo" : "classroom-done"}>
          {course.total > course.done ? `${course.total - course.done} to do` : "All caught up"}
          <ChevronRight size={14} />
        </Text>
      </div>
    </Button>
  )
}

function Countdown({ dueAt }: { dueAt: number }) {
  const remaining = () => Math.max(0, Math.ceil((dueAt - Date.now()) / 1000))
  const [seconds, setSeconds] = useState(remaining)
  useEffect(() => {
    setSeconds(remaining())
    const timer = window.setInterval(
      () => setSeconds(remaining()),
      1000,
    )
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

type ScreenProps = {
  open: (overlay: Overlay) => void
  selectCourse: (course: Course) => void
  completed: Set<string>
  now: number
}

function StudyOverview({ open, completed, now }: Pick<ScreenProps, "open" | "completed" | "now">) {
  const { courses, assignments } = useLearningData()
  const summary = learningSummary(courses, assignments, completed, now)
  const items = [
    { filter: "todo" as const, label: "To do", value: summary.pending, icon: ListChecks },
    { filter: "urgent" as const, label: "Due soon", value: summary.urgent, icon: Clock3 },
    { filter: "done" as const, label: "Recent done", value: summary.recentDone, icon: CircleCheck },
  ]
  return (
    <Card className="study-overview">
      <div className="study-overview-heading">
        <span className="subject-icon subject-icon--green"><BookOpen size={21} /></span>
        <div><Text className="eyebrow">YOUR STUDY SPACE</Text><Text as="h3">A little focus. A lot of progress.</Text></div>
      </div>
      <div className="study-overview-stats">
        {items.map(({ filter, label, value, icon: Icon }) => (
          <Button key={filter} variant="unstyled" className={`study-stat study-stat--${filter}`} onClick={() => open({ type: "classwork", filter })} aria-label={`${label}: ${value} assignments`}>
            <span><Icon size={15} /><Text>{value}</Text></span><Text>{label}<ChevronRight size={12} /></Text>
          </Button>
        ))}
      </div>
    </Card>
  )
}

function HomeScreen({ open, selectCourse, completed, now }: ScreenProps) {
  const { courses, assignments } = useLearningData()
  const summary = learningSummary(courses, assignments, completed, now)
  const pending = pendingAssignments(assignments, completed)
  const next = pending[0]
  const nextStatus = next ? assignmentStatus(next, completed, now) : "done"
  return (
    <>
      <header className="app-header home-header">
        <div className="greeting">
          <div className="avatar avatar--student">
            HA
            <span />
          </div>
          <div>
            <Text className="app-wordmark">moodle<Text>USTH</Text></Text>
            <Text as="h2">
              Hi, An{" "}
              <span className="greeting-sun">
                <Sparkles size={18} />
              </span>
            </Text>
          </div>
        </div>
        <Button
          variant="icon"
          className="notification-button"
          aria-label="View notifications"
          onClick={() => open({ type: "notifications" })}
        >
          <Bell size={21} />
          {summary.urgent > 0 && <span className="notification-count">{summary.urgent}</span>}
        </Button>
      </header>
      <div className="screen-scroll home-content">
        <StudyOverview open={open} completed={completed} now={now} />
        <Card
          className={`deadline-card ${
            !next ? "deadline-card--done" : ""
          }`}
        >
          <div className="deadline-content">
            <div className="deadline-kicker">
              <span className="live-dot" />
              <Text>
                {!next
                  ? "ONE LESS THING TO DO"
                  : nextStatus === "overdue" ? "NEEDS YOUR ATTENTION" : "YOUR NEXT DEADLINE"}
              </Text>
            </div>
            <Text as="h3">
              {!next ? "All caught up, An." : next.title}
            </Text>
            {!next ? (
              <div className="deadline-done">
                <CircleCheck size={29} />
                <Text>Every assignment is marked as done.</Text>
              </div>
            ) : (
              nextStatus === "overdue" ? <Text className="overdue-countdown">Overdue · start here</Text> : <Countdown key={next.id} dueAt={next.dueAt} />
            )}
            <Button
              variant="text"
              className="deadline-link"
              onClick={() =>
                next ? open({ type: "assignment", assignment: next }) : open({ type: "classwork", filter: "done" })
              }
            >
              {next ? `${next.contribution}% of course grade · View task` : "View completed work"} <ArrowRight size={14} />
            </Button>
          </div>
          <img src="/images/deadline.webp" alt="" />
          <Badge
            tone={!next ? "green" : nextStatus === "upcoming" ? "blue" : "coral"}
            className="deadline-badge"
          >
            {!next ? "Done" : nextStatus === "overdue" ? "Overdue" : nextStatus === "urgent" ? "Due within 24h" : "Coming up"}
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
            {pending.slice(0, 2).map((assignment) => <AssignmentRow key={assignment.id} assignment={assignment} done={false} now={now} showCourse onClick={() => open({ type: "assignment", assignment })} />)}
            {pending.length === 0 && <div className="classwork-empty"><CircleCheck size={24} /><Text>No assignments left. Enjoy the breathing room.</Text></div>}
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
                course={courseWithCompletion(course, assignments, completed)}
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

function CoursesScreen({ selectCourse, completed, open, now }: ScreenProps) {
  const { courses, assignments } = useLearningData()
  const summary = learningSummary(courses, assignments, completed, now)
  return (
    <>
      <header className="app-header">
        <div>
          <Text className="muted greeting-small">SEMESTER 2 · 2024–2025</Text>
          <Text as="h2">My courses</Text>
        </div>
        <Button variant="icon" aria-label="View weekly planner" onClick={() => open({ type: "calendar" })}>
          <CalendarDays size={24} />
        </Button>
      </header>
      <div className="screen-scroll courses-content">
        <div className="classroom-shortcuts">
          <Button onClick={() => open({ type: "classwork" })}><ListChecks size={17} />To do<Badge tone="blue">{summary.pending}</Badge></Button>
          <Button onClick={() => open({ type: "classwork", filter: "urgent" })}><Clock3 size={17} />Due soon<Badge tone="coral">{summary.urgent}</Badge></Button>
        </div>
        <div className="section-heading">
          <Text as="h3">In progress<Text className="heading-count">03</Text></Text>
          <Badge tone="blue">10 credits</Badge>
        </div>
        <div className="courses-grid">
          {courses.map((item) => (
            <CourseCard
              key={item.id}
              course={courseWithCompletion(item, assignments, completed)}
              onClick={() => selectCourse(item)}
            />
          ))}
        </div>
        <div className="little-note"><BookOpen size={16} /><Text>Select a course to view your classwork and resources.</Text></div>
      </div>
    </>
  )
}

function GradeWeights({ detailed = false }: { detailed?: boolean }) {
  return (
    <Card
      className={`weights-card ${detailed ? "weights-card--detailed" : ""}`}
    >
      <div className="weights-title">
        <Text as="h3">Know what counts</Text>
        <Info size={14} className="muted" />
      </div>
      {detailed && (
        <Text as="p" className="muted small weight-intro">
          Your final grade is a weighted average of these three components.
        </Text>
      )}
      <div
        className="weight-bar"
        aria-label="Attendance 10%, mid-term and labs 30%, final project 60%"
      >
        <span />
        <span />
        <span />
      </div>
      <div className="weight-legend">
        <div>
          <Text className="weight-value weight-blue">10%</Text>
          <Text>
            <i className="dot dot--blue" />
            Attendance
          </Text>
        </div>
        <div>
          <Text className="weight-value weight-amber">30%</Text>
          <Text>
            <i className="dot dot--amber" />
            Mid-term & labs
          </Text>
        </div>
        <div>
          <Text className="weight-value weight-purple">60%</Text>
          <Text>
            <i className="dot dot--purple" />
            Final project
          </Text>
        </div>
      </div>
      {detailed && (
        <div className="weight-details">
          <div>
            <Text>Attendance</Text>
            <Badge tone="green">9.5 / 10</Badge>
          </div>
          <div>
            <Text>Mid-term & labs</Text>
            <Badge tone="amber">In progress</Badge>
          </div>
          <div>
            <Text>Final project</Text>
            <Badge tone="purple">Not graded yet</Badge>
          </div>
          <Text as="p" className="muted tiny">
            Project proposals are a milestone of the final project, not an
            additional 60% assessment.
          </Text>
        </div>
      )}
    </Card>
  )
}

function AssignmentRow({
  assignment,
  done,
  onClick,
  compact = false,
  showCourse = false,
  now = Date.now(),
}: {
  assignment: Assignment
  done: boolean
  onClick: () => void
  compact?: boolean
  showCourse?: boolean
  now?: number
}) {
  const { courses } = useLearningData()
  const status = done ? "done" : assignmentStatus(assignment, new Set(), now)
  const urgent = status === "urgent" || status === "overdue"
  return (
    <Button
      variant="unstyled"
      className={`assignment-row ${done ? "assignment-row--done" : ""} ${
        compact ? "assignment-row--compact" : ""
      } ${urgent ? "assignment-row--urgent" : ""}`}
      onClick={onClick}
    >
      <span
        className={`assignment-icon ${done ? "assignment-icon--done" : ""}`}
      >
        {done ? <CircleCheck size={18} /> : <FileText size={18} />}
      </span>
      <div className="assignment-info">
        <Text as="h4">{assignment.title}</Text>
        {showCourse && <Text className="assignment-course">{courses.find((course) => course.id === assignment.courseId)?.shortTitle}</Text>}
        <Text
          className={`assignment-date ${
            urgent ? "urgent-text" : ""
          }`}
        >
          {done ? (
            "Completed · Nice work!"
          ) : (
            <>
              <Clock3 size={10} />
              <strong>{timeRemaining(assignment, now)}</strong><Text>· {formatDueDate(assignment.dueAt)}</Text>
            </>
          )}
        </Text>
        {!compact && (
          <div className="assignment-badges">
            <Badge tone={done ? "green" : assignment.category === "Final project" ? "purple" : "amber"}>
              {done ? "Completed" : `${assignment.category} · ${assignment.categoryWeight}%`}
            </Badge>
            {!done && <Badge tone="blue"><Target size={10} />{assignment.contribution}% this task</Badge>}
          </div>
        )}
      </div>
      {done && assignment.score !== undefined ? (
        <Text className="assignment-score">
          {assignment.score.toFixed(1)}<Text>/10</Text>
        </Text>
      ) : (
        <ChevronRight size={16} className="muted" />
      )}
    </Button>
  )
}

function ClassworkList({
  open,
  completed,
  now,
  initialFilter = "todo",
}: Pick<ScreenProps, "open" | "completed" | "now"> & {
  initialFilter?: AssignmentFilter
}) {
  const { courses, assignments } = useLearningData()
  const [filter, setFilter] = useState<AssignmentFilter>(initialFilter)
  const [courseId, setCourseId] = useState("all")
  const course = courseId === "all" ? undefined : courseId
  const { data, loading, error, refetch } = useApi(
    (signal) => api.getAssignments({ status: filter, course }, signal),
    [filter, course],
  )
  const inCourse = (assignment: Assignment) => !course || assignment.courseId === course
  const matches = (assignment: Assignment, current: AssignmentFilter) => {
    const status = assignmentStatus(assignment, completed, now)
    return current === "done" ? status === "done" : current === "urgent" ? status === "urgent" || status === "overdue" : status !== "done"
  }
  // Re-check on the client: the server doesn't know about tasks ticked off in this
  // session, and static mock files ignore the query string.
  // If the request fails, fall back to the full list loaded at startup (like Timetable) instead of a dead end.
  const source = error ? assignments : data
  const fetched = (source ?? []).filter((assignment) => inCourse(assignment) && matches(assignment, filter))
  const markedHere = filter === "done" && source
    ? assignments.filter((assignment) => completed.has(assignment.id) && inCourse(assignment) && !fetched.some((item) => item.id === assignment.id))
    : []
  const visible = [...fetched, ...markedHere].sort((a, b) => a.dueAt - b.dueAt)
  // Tab badges come from the list loaded at startup, so they don't need three extra requests.
  const scoped = assignments.filter(inCourse)
  const filters: { id: AssignmentFilter; label: string }[] = [
    { id: "todo", label: "To do" }, { id: "urgent", label: "Due soon" }, { id: "done", label: "Done" },
  ]
  return (
    <div className="classwork-list">
      <Text as="p" className="muted small">Every class. Every deadline. One clear list.</Text>
      <div className="classwork-filters" role="tablist" aria-label="Assignment status">
        {filters.map((item) => <Button key={item.id} variant="unstyled" role="tab" id={`classwork-${item.id}`} aria-controls="classwork-results" aria-selected={filter === item.id} className={filter === item.id ? "is-active" : ""} onClick={() => setFilter(item.id)}>{item.label}<Text>{scoped.filter((assignment) => matches(assignment, item.id)).length}</Text></Button>)}
      </div>
      <div className="classwork-course-filters" role="group" aria-label="Filter assignments by course">
        <Button variant="unstyled" aria-pressed={courseId === "all"} onClick={() => setCourseId("all")}>All classes</Button>
        {courses.map((course) => <Button key={course.id} variant="unstyled" aria-pressed={courseId === course.id} onClick={() => setCourseId(course.id)}>{course.shortTitle}</Button>)}
      </div>
      <div className={`classwork-results ${loading && data ? "is-refreshing" : ""}`} role="tabpanel" id="classwork-results" aria-labelledby={`classwork-${filter}`} aria-busy={loading}>
        {error && <div className="timetable-note" role="alert"><Info size={16} /><Text>Couldn't refresh assignments: {error.message} Showing the list loaded earlier.</Text><Button variant="text" onClick={refetch} disabled={loading}>Try again</Button></div>}
        {!source ? (
          <div className="classwork-empty" role="status"><Text>Loading assignments…</Text></div>
        ) : (
        <>
        {visible.map((assignment) => <AssignmentRow key={assignment.id} assignment={assignment} done={assignmentStatus(assignment, completed, now) === "done"} now={now} showCourse onClick={() => open({ type: "assignment", assignment })} />)}
        {visible.length === 0 && <div className="classwork-empty"><CircleCheck size={32} /><Text as="h3">{filter === "urgent" ? "Nothing urgent. You're on track." : filter === "done" ? "Your next small win starts here." : "All caught up!"}</Text><Text>Try another class or status to see more assignments.</Text></div>}
        </>
        )}
      </div>
      <Text className="muted tiny classwork-list-note">Due soon means within 24 hours, or overdue. Times use your device's local time. “Done” shows recent tasks in this preview.</Text>
    </div>
  )
}

function CourseScreen({
  course,
  open,
  completed,
  onBack,
  now,
}: ScreenProps & {
  course: Course
  onBack: () => void
}) {
  const { assignments } = useLearningData()
  const [tab, setTab] = useState("Stream")
  const [collapsed, setCollapsed] = useState<Set<string>>(new Set())
  const toggleGroup = (group: string) =>
    setCollapsed((previous) => {
      const next = new Set(previous)
      if (next.has(group)) next.delete(group)
      else next.add(group)
      return next
    })
  const courseAssignments = assignments.filter((assignment) => assignment.courseId === course.id)
  const pending = pendingAssignments(assignments, completed, course.id)
  const progress = courseWithCompletion(course, assignments, completed)
  const groups = [
    { title: "Final project", weight: "60%", tone: "purple" as const, items: courseAssignments.filter((assignment) => assignment.category === "Final project") },
    { title: "Mid-term & labs", weight: "30%", tone: "amber" as const, items: courseAssignments.filter((assignment) => assignment.category === "Mid-term & labs") },
  ].filter((group) => group.items.length > 0)
  return (
    <>
      <header className="app-header compact-header">
        <Button variant="icon" aria-label="Go back" onClick={onBack}>
          <ArrowLeft size={20} />
        </Button>
        <Text className="header-title">Course details</Text>
        <Button
          variant="icon"
          aria-label="Course information"
          onClick={() => open({ type: "course-info" })}
        >
          <MoreHorizontal size={22} />
        </Button>
      </header>
      <div className="screen-scroll course-content">
        <div className={`course-hero course-hero--${course.tone}`}>
          <img src={`/images/${course.image}.webp`} alt="" />
          <div className="course-hero-shade" />
          <div className="course-hero-content">
            <Text className="course-code">
              {course.code} <span>·</span> {course.credits} CREDITS
            </Text>
            <Text as="h2">{course.title}</Text>
            <div className="course-teacher">
              <span className="teacher-avatar">{course.initials}</span>
              <Text>{course.teacher}</Text>
            </div>
          </div>
          <ProgressRing value={progress.progress} label="complete" />
        </div>
        <div className="course-study-metrics">
          <span><ListChecks size={15} /><Text><strong>{pending.length}</strong> to do</Text></span>
          <span><TrendingUp size={15} /><Text><strong>{progress.done}/{progress.total}</strong> completed</Text></span>
          <span><GraduationCap size={17} /><Text><strong>{course.credits}</strong> credits</Text></span>
        </div>
        <div className="course-tabs" role="tablist" aria-label="Course content">
          {["Stream", "Classwork", "Grade Weights"].map((item) => (
            <Button
              key={item}
              variant="unstyled"
              role="tab"
              id={`tab-${course.id}-${item.replace(/ /g, "-")}`}
              aria-selected={tab === item}
              aria-controls={`panel-${course.id}`}
              className={tab === item ? "is-active" : ""}
              onClick={() => setTab(item)}
            >
              {item}
            </Button>
          ))}
        </div>
        <div
          className="course-tab-content"
          role="tabpanel"
          id={`panel-${course.id}`}
          aria-labelledby={`tab-${course.id}-${tab.replace(/ /g, "-")}`}
        >
          {tab === "Classwork" && (
            <>
              {course.id === "web" && <GradeWeights />}
              <SectionHeading
                title="Your classwork"
                detail={String(courseAssignments.length).padStart(2, "0")}
              />
              {groups.map((group) => (
                <section className="assignment-group" key={group.title}>
                  <Button
                    variant="unstyled"
                    className="assignment-group-heading"
                    aria-expanded={!collapsed.has(group.title)}
                    onClick={() => toggleGroup(group.title)}
                  >
                    <Text>{group.title}</Text>
                    <Badge tone={group.tone}>{group.weight}</Badge>
                    <ChevronDown
                      size={15}
                      className={
                        collapsed.has(group.title) ? "chevron-closed" : ""
                      }
                    />
                  </Button>
                  {!collapsed.has(group.title) && (
                    <div className="assignment-group-items">
                      {group.items.map((assignment) => (
                        <AssignmentRow
                          key={assignment.id}
                          assignment={assignment}
                          done={Boolean(
                            assignment.completed ||
                              completed.has(assignment.id),
                          )}
                          onClick={() =>
                            open({ type: "assignment", assignment })
                          }
                          compact={assignment.completed}
                          now={now}
                        />
                      ))}
                    </div>
                  )}
                </section>
              ))}
              <div className="course-tip">
                <Target size={17} />
                <Text>
                  Focus on your final project.
                  <br />
                  <strong>It makes the biggest difference.</strong>
                </Text>
              </div>
            </>
          )}
          {tab === "Grade Weights" && (
            <>
              <GradeWeights detailed />
              <Card className="grade-explainer">
                <TrendingUp size={23} />
                <Text as="h3">Every point has a purpose.</Text>
                <Text as="p" className="muted small">
                  Your current accumulated grade is{" "}
                  <strong>{course.grade}/10</strong>. Keep completing
                  assessments to build your final result.
                </Text>
                <Button variant="text" onClick={() => open({ type: "grades" })}>
                  See academic overview
                  <ArrowRight size={14} />
                </Button>
              </Card>
            </>
          )}
          {tab === "Stream" && (
            <div className="stream-feed">
              <section className="stream-upcoming">
                <SectionHeading title="Your next steps" detail={String(pending.length)} action="View classwork" onAction={() => setTab("Classwork")} />
                <div className="upcoming-list">
                  {pending.slice(0, 2).map((assignment) => <AssignmentRow key={assignment.id} assignment={assignment} done={false} now={now} onClick={() => open({ type: "assignment", assignment })} />)}
                  {pending.length === 0 && <div className="classwork-empty"><CircleCheck size={25} /><Text>All caught up in this class.</Text></div>}
                </div>
              </section>
              <Card className="stream-card">
                <div className="stream-author">
                  <span className="teacher-avatar">{course.initials}</span>
                  <div>
                    <Text className="bold">{course.teacher}</Text>
                    <Text className="muted tiny">Today, 08:30</Text>
                  </div>
                  <Pin size={15} />
                </div>
                <Badge tone="blue">COURSE ANNOUNCEMENT</Badge>
                <Text as="h3">A little reminder for this week</Text>
                <Text as="p" className="muted small">
                  Please check your upcoming classwork and make sure your
                  project milestones are on track. Bring your questions to our
                  next session!
                </Text>
                <div className="stream-footer">
                  <MessageCircle size={14} />
                  <Text>Questions? Ask in your next class.</Text>
                </div>
              </Card>
              <Card className="stream-card">
                <div className="stream-author">
                  <span className="teacher-avatar">{course.initials}</span>
                  <div>
                    <Text className="bold">{course.teacher}</Text>
                    <Text className="muted tiny">May 16, 14:15</Text>
                  </div>
                </div>
                <Text as="h3">Week 09 resources are ready</Text>
                <Text as="p" className="muted small">
                  Lecture notes and practice materials are now available. Review
                  the key concepts before starting your next assignment.
                </Text>
                <Button
                  className="resource-button"
                  onClick={() => open({ type: "course-info" })}
                >
                  <FileText size={17} />
                  View course resources
                  <ChevronRight size={15} />
                </Button>
              </Card>
            </div>
          )}
        </div>
      </div>
    </>
  )
}

function TrackerScreen({ open, selectCourse, completed, now }: ScreenProps) {
  const { courses, assignments } = useLearningData()
  const summary = learningSummary(courses, assignments, completed, now)
  return (
    <>
      <header className="app-header tracker-header">
        <div>
          <Text className="muted greeting-small">THE BIGGER PICTURE</Text>
          <Text as="h2">
            Your progress
            <TrendingUp size={22} />
          </Text>
        </div>
        <Button
          variant="icon"
          aria-label="View grade report"
          onClick={() => open({ type: "grades" })}
        >
          <ArrowDownToLine size={20} />
        </Button>
      </header>
      <div className="screen-scroll tracker-content">
        <Button
          variant="unstyled"
          className="semester-selector"
          onClick={() => open({ type: "semester" })}
          aria-label="View semester overview"
        >
          <CalendarDays size={16} />
          <Text>Semester 2, 2024–2025</Text>
          <ChevronDown size={16} />
        </Button>
        <Card className="semester-card">
          <div className="semester-card-top">
            <Badge tone="blue">
              <span className="live-dot" />
              IN PROGRESS
            </Badge>
            <GraduationCap size={27} />
          </div>
          <Text className="semester-caption">A semester of possibilities.</Text>
          <div className="credits-value">
            <Text>18</Text>
            <Text>
              / 24
              <span>credits earned</span>
            </Text>
          </div>
          <ProgressBar
            value={75}
            tone="white"
            label="Semester credits earned"
          />
          <div className="semester-card-bottom">
            <Text>
              6 credits to your next milestone
            </Text>
            <Sparkles size={14} />
          </div>
        </Card>
        <div className="tracker-stats">
          <Card className="completion-stat">
            <div className="stat-label">
              <span className="stat-icon stat-icon--green">
                <CircleCheck size={17} />
              </span>
              <Text>Overall completion</Text>
            </div>
            <div className="stat-number">
              <Text>
                {summary.progress}
                <span>%</span>
              </Text>
              <Badge tone="green">On track</Badge>
            </div>
            <ProgressBar
              value={summary.progress}
              tone="green"
              label="Overall completion"
            />
            <Text className="stat-footnote">
              {summary.done} of {summary.total} tasks · 3 courses
            </Text>
          </Card>
          <Card className="grade-stat">
            <div className="stat-label">
              <span className="stat-icon stat-icon--blue">
                <Award size={17} />
              </span>
              <Text>Average grade</Text>
            </div>
            <div className="stat-number">
              <Text>
                8.6
                <span>/10</span>
              </Text>
            </div>
            <Badge tone="blue">Keep it up!</Badge>
            <Text className="stat-footnote">You're doing great.</Text>
          </Card>
        </div>
        <section>
          <SectionHeading title="Subject breakdown" detail="03" />
          <div className="subject-list">
            {courses.map((item) => courseWithCompletion(item, assignments, completed)).map((course) => (
              <Button
                key={course.id}
                variant="unstyled"
                className="subject-card"
                onClick={() => selectCourse(course)}
              >
                <div className="subject-card-top">
                  <span className={`subject-icon subject-icon--${course.tone}`}>
                    <CourseIcon id={course.id} />
                  </span>
                  <div>
                    <Text as="h4">{course.shortTitle}</Text>
                    <Text className="muted tiny">
                      {course.credits} credits · {course.code}
                    </Text>
                  </div>
                  <ChevronRight size={15} className="muted" />
                </div>
                <div className="subject-progress-label">
                  <Text>
                    <strong>
                      {course.done}
                    </strong>{" "}
                    / {course.total} tasks completed
                  </Text>
                  <Text
                    className={`subject-grade subject-grade--${course.tone}`}
                  >
                    {course.grade}
                    <span>/10</span>
                  </Text>
                </div>
                <ProgressBar
                  value={course.progress}
                  tone={course.tone}
                />
              </Button>
            ))}
          </div>
        </section>
        <div className="tracker-encouragement">
          <span className="encouragement-icon">
            <Sparkles size={18} />
          </span>
          <div>
            <Text as="h4">Look how far you've come.</Text>
            <Text className="muted tiny">
              Every completed task is a step forward.
            </Text>
          </div>
        </div>
      </div>
    </>
  )
}

function ProfileScreen({ onNavigate }: { onNavigate: (view: View) => void }) {
  return (
    <>
      <header className="app-header">
        <Text as="h2">Your profile</Text>
        <GraduationCap size={24} className="blue-text" />
      </header>
      <div className="screen-scroll profile-content">
        <Card className="profile-card">
          <div className="avatar avatar--large">HA</div>
          <Text as="h2">Hoang An</Text>
          <Text className="muted small">USTH · Class of 2026</Text>
          <Badge tone="blue">Information & Communication Technology</Badge>
        </Card>
        <Card className="profile-details">
          <Text className="eyebrow">STUDENT INFORMATION</Text>
          <div>
            <Text className="muted">Student ID</Text>
            <Text>BA22-041</Text>
          </div>
          <div>
            <Text className="muted">Academic year</Text>
            <Text>2024–2025</Text>
          </div>
          <div>
            <Text className="muted">Current semester</Text>
            <Text>Semester 2</Text>
          </div>
        </Card>
        <Button className="profile-link" onClick={() => onNavigate("tracker")}>
          <ChartNoAxesCombined size={20} />
          <Text>My academic progress</Text>
          <ChevronRight size={16} />
        </Button>
        <Card className="preview-note">
          <Info size={20} />
          <div>
            <Text as="h3">Your campus, reimagined.</Text>
            <Text as="p" className="muted small">
              This is an interactive design preview with sample academic data,
              not a live connection to USTH Moodle.
            </Text>
          </div>
        </Card>
      </div>
    </>
  )
}

export default function App() {
  const { data, loading, error, refetch } = useApi(loadLearningData, [])
  if (!data) {
    return (
      <div className="app">
        <OfflineBanner />
        <main className="app-main">
          <div className="classwork-empty app-fallback" role={error ? "alert" : "status"}>
            {error ? (
              <>
                {error.status === 0 ? <WifiOff size={32} /> : <TriangleAlert size={32} />}
                <Text as="h3">Không tải được dữ liệu</Text>
                <Text>{error.message}</Text>
                <Button variant="primary" onClick={refetch} disabled={loading}><RotateCw size={16} />{loading ? "Đang thử lại…" : "Thử lại"}</Button>
              </>
            ) : (
              <Text>Đang tải dữ liệu…</Text>
            )}
          </div>
        </main>
      </div>
    )
  }
  return (
    <LearningDataContext.Provider value={data}>
      <AppContent />
    </LearningDataContext.Provider>
  )
}

function AppContent() {
  const { courses, assignments, sessions } = useLearningData()
  const [view, setView] = useState<View>("home")
  const [timetableDate, setTimetableDate] = useState(() => new Date())
  const [timetableView, setTimetableView] = useState<TimetableView>("week")
  const [courseBack, setCourseBack] = useState<View>("courses")
  const [course, setCourse] = useState<Course | undefined>(courses[0])
  const [overlay, setOverlay] = useState<Overlay>(null)
  const [completed, setCompleted] = useState<Set<string>>(new Set())
  const [now, setNow] = useState(Date.now)
  const [toast, setToast] = useState<Toast | null>(null)
  // Read at call time, so a retry from a toast doesn't act on a stale set.
  const completedRef = useRef(completed)
  completedRef.current = completed
  // Tick on each minute boundary so class statuses flip exactly at HH:MM, and catch up after the tab was hidden.
  useEffect(() => {
    let timeout: number
    const tick = () => {
      setNow(Date.now())
      timeout = window.setTimeout(tick, 60000 - (Date.now() % 60000))
    }
    timeout = window.setTimeout(tick, 60000 - (Date.now() % 60000))
    const onVisible = () => {
      if (document.visibilityState === "visible") setNow(Date.now())
    }
    document.addEventListener("visibilitychange", onVisible)
    return () => {
      window.clearTimeout(timeout)
      document.removeEventListener("visibilitychange", onVisible)
    }
  }, [])
  useEffect(() => {
    if (!toast) return
    const timeout = window.setTimeout(() => setToast(null), toast.action ? 8000 : 4000)
    return () => window.clearTimeout(timeout)
  }, [toast])

  function selectCourse(selected: Course) {
    setCourse(courses.find((item) => item.id === selected.id) ?? selected)
    setCourseBack(view === "course" ? "courses" : view)
    setView("course")
    setOverlay(null)
  }
  // Optimistic: progress on every screen updates at once, and is rolled back if the server says no.
  async function markDone(assignment: Assignment) {
    const id = assignment.id
    const nextCompleted = new Set([...completedRef.current, id])
    setCompleted((previous) => new Set([...previous, id]))
    const taskCourse = courses.find((course) => course.id === assignment.courseId)
    if (taskCourse) {
      const progress = courseWithCompletion(taskCourse, assignments, nextCompleted)
      setToast({ message: `${taskCourse.shortTitle}: ${progress.progress}% complete · ${progress.total - progress.done} tasks left.` })
    }
    try {
      await api.completeAssignment(id)
    } catch (error) {
      setCompleted((previous) => {
        const next = new Set(previous)
        next.delete(id)
        return next
      })
      const reason = error instanceof ApiError ? error.message : "Something went wrong."
      setToast({
        tone: "error",
        message: `Couldn't mark "${assignment.title}" as done. ${reason}`,
        action: { label: "Thử lại", onClick: () => markDone(assignment) },
      })
    }
  }
  function downloadReport() {
    const rows = [
      "Subject,Course code,Credits,Completed tasks,Total tasks,Accumulated grade",
      ...courses.map((item) => courseWithCompletion(item, assignments, completed)).map(
        (item) =>
          `${item.title},${item.code},${item.credits},${item.done},${item.total},${item.grade}`,
      ),
    ]
    const url = URL.createObjectURL(
      new Blob([rows.join("\n")], { type: "text/csv;charset=utf-8;" }),
    )
    const link = document.createElement("a")
    link.href = url
    link.download = "USTH-semester-2-sample-report.csv"
    link.click()
    URL.revokeObjectURL(url)
    setToast({ message: "Sample grade report downloaded." })
  }
  const screenProps = { open: setOverlay, selectCourse, completed, now }
  const summary = learningSummary(courses, assignments, completed, now)
  const activeStatus = overlay?.type === "assignment" ? assignmentStatus(overlay.assignment, completed, now) : null
  return (
    <div className="app">
      <OfflineBanner raise={overlay?.type} />
      <main className="app-main">
        <div className="app-screen" key={`${view}-${view === "course" ? course?.id : ""}`}>
          {view === "home" ? (
            <HomeScreen {...screenProps} />
          ) : view === "courses" ? (
            <CoursesScreen {...screenProps} />
          ) : view === "course" && course ? (
            <CourseScreen {...screenProps} course={course} onBack={() => setView(courseBack)} />
          ) : view === "timetable" ? (
            <TimetableScreen now={now} selectCourse={selectCourse} selectedDate={timetableDate} setSelectedDate={setTimetableDate} calendarView={timetableView} setCalendarView={setTimetableView} />
          ) : view === "tracker" ? (
            <TrackerScreen {...screenProps} />
          ) : (
            <ProfileScreen onNavigate={setView} />
          )}
        </div>
      </main>
      <BottomNav view={view} onNavigate={setView} />
      {overlay && (
        <Modal
          title={
            overlay.type === "assignment"
              ? "Assignment details"
              : overlay.type === "classwork"
                ? "My classwork"
              : overlay.type === "notifications"
                ? "Your notifications"
                : overlay.type === "courses"
                  ? "Your courses"
                  : overlay.type === "calendar"
                    ? "Your weekly planner"
                    : overlay.type === "grades"
                      ? "Your grade report"
                      : overlay.type === "course-info"
                        ? "Course information"
                        : "Semester overview"
          }
          onClose={() => setOverlay(null)}
        >
          {overlay.type === "assignment" && (
            <div className="assignment-detail">
              <Badge
                tone={
                  activeStatus === "done"
                    ? "green"
                    : activeStatus === "urgent" || activeStatus === "overdue"
                      ? "coral"
                      : "amber"
                }
              >
                {activeStatus === "done"
                  ? "Completed"
                  : activeStatus === "overdue" ? "Overdue"
                  : activeStatus === "urgent"
                    ? "Due within 24 hours"
                    : "Upcoming"}
              </Badge>
              <Text as="h3">{overlay.assignment.title}</Text>
              <Text className="muted small">{courses.find((course) => course.id === overlay.assignment.courseId)?.title}</Text>
              <div className="assignment-detail-meta">
                <span>
                  <Clock3 size={16} />
                  {activeStatus === "done" ? "Completed" : timeRemaining(overlay.assignment, now)} · {formatDueDate(overlay.assignment.dueAt)}
                </span>
                <span>
                  <Target size={16} />
                  {overlay.assignment.contribution}% of course grade
                </span>
              </div>
              <Text as="p" className="assignment-description">
                {overlay.assignment.description}
              </Text>
              <Card className="assignment-impact">
                <div className="impact-heading"><Target size={19} /><Text as="h4">How this task counts</Text></div>
                <div><Text>Assessment component</Text><Text>{overlay.assignment.category}<Badge tone={overlay.assignment.category === "Final project" ? "purple" : "amber"}>{overlay.assignment.categoryWeight}% of course</Badge></Text></div>
                <div><Text>This assignment</Text><Text className="impact-contribution">{overlay.assignment.contribution}%<Text>of the final course grade</Text></Text></div>
                <div><Text>Course credits</Text><Text><GraduationCap size={16} />{courses.find((course) => course.id === overlay.assignment.courseId)?.credits} credits</Text></div>
                <Text as="p" className="tiny muted">A 10/10 score on this task contributes up to {(overlay.assignment.contribution / 10).toFixed(2)} points to your course grade out of 10.</Text>
              </Card>
              <div className="inline-info"><Info size={17} /><Text><strong>Course weight is not GPA weight.</strong> This task contributes {overlay.assignment.contribution}% to this course. GPA uses final course grades weighted by credits, with the university's official grade conversion.</Text></div>
              <Button
                variant="primary"
                disabled={Boolean(
                  overlay.assignment.completed ||
                    completed.has(overlay.assignment.id),
                )}
                onClick={() => markDone(overlay.assignment)}
              >
                <Check size={17} />
                {overlay.assignment.completed ||
                completed.has(overlay.assignment.id)
                  ? "Marked as complete"
                  : "Mark as done"}
              </Button>
              <Text className="muted tiny preview-disclaimer">
                Interactive preview only. Submit your actual work through USTH
                Moodle.
              </Text>
              <Button variant="text" onClick={() => setOverlay({ type: "classwork" })}><ListChecks size={16} />Back to all classwork</Button>
            </div>
          )}
          {overlay.type === "classwork" && <ClassworkList key={overlay.filter ?? "todo"} open={setOverlay} completed={completed} now={now} initialFilter={overlay.filter} />}
          {overlay.type === "courses" && (
            <div className="modal-course-list">
              <Text as="p" className="muted small">
                Your learning, all in one place. Select a course to explore.
              </Text>
              {courses.map((item) => (
                <CourseCard
                  key={item.id}
                  course={courseWithCompletion(item, assignments, completed)}
                  onClick={() => selectCourse(item)}
                />
              ))}
            </div>
          )}
          {overlay.type === "notifications" && (
            <div className="notification-list">
              <div className="notification-summary"><Badge tone={summary.urgent > 0 ? "coral" : "green"}><Clock3 size={12} />{summary.urgent} due soon</Badge><Text className="muted tiny">In-app reminders · sample course data</Text></div>
              <SectionHeading title="Deadline reminders" action="All classwork" onAction={() => setOverlay({ type: "classwork" })} />
              {pendingAssignments(assignments, completed).filter((assignment) => ["urgent", "overdue"].includes(assignmentStatus(assignment, completed, now))).map((assignment) => <AssignmentRow key={assignment.id} assignment={assignment} done={false} showCourse now={now} onClick={() => setOverlay({ type: "assignment", assignment })} />)}
              {summary.urgent === 0 && <div className="classwork-empty"><CircleCheck size={24} /><Text>No urgent work. You're on track.</Text></div>}
              <SectionHeading title="Course progress" />
              {courses.map((item) => courseWithCompletion(item, assignments, completed)).map((item) => <Button key={item.id} variant="unstyled" className="progress-notification" onClick={() => selectCourse(item)}><span className={`subject-icon subject-icon--${item.tone}`}><CourseIcon id={item.id} /></span><div><Text as="h3">{item.shortTitle} · {item.progress}% complete</Text><Text className="muted tiny">{item.done}/{item.total} tasks completed · {item.total - item.done} remaining · {item.credits} credits</Text><ProgressBar value={item.progress} tone={item.tone} /></div><ChevronRight size={16} /></Button>)}
              <SectionHeading title="From your classroom" />
              {[
                {
                  icon: Award,
                  title: "A new grade to feel good about",
                  body: "Lab 03 · React components · 9.0 / 10",
                  time: "2 HOURS AGO",
                },
                {
                  icon: MessageCircle,
                  title: "A note from Dr. Nguyen Minh",
                  body: "Week 09 resources are ready to explore.",
                  time: "3 HOURS AGO",
                },
              ].map((item, index) => (
                <Button
                  key={item.title}
                  variant="unstyled"
                  className="notification-row"
                  onClick={() => {
                    if (index === 0) {
                      const graded = assignments.find((assignment) => assignment.completed)
                      if (graded) setOverlay({ type: "assignment", assignment: graded })
                    } else {
                      if (courses[0]) selectCourse(courses[0])
                      setOverlay({ type: "course-info" })
                    }
                  }}
                >
                  <span className="subject-icon subject-icon--blue">
                    <item.icon size={19} />
                  </span>
                  <div>
                    <Text className="eyebrow">{item.time}</Text>
                    <Text as="h3">{item.title}</Text>
                    <Text className="muted tiny">{item.body}</Text>
                  </div>
                  <ChevronRight size={16} />
                </Button>
              ))}
            </div>
          )}
          {overlay.type === "calendar" && (
            <div className="planner">
              <Text className="muted small">
                Upcoming deadlines · shown in your local time
              </Text>
              {pendingAssignments(assignments, completed)
                .map((assignment) => (
                  <AssignmentRow
                    key={assignment.id}
                    assignment={assignment}
                    done={completed.has(assignment.id)}
                    now={now}
                    showCourse
                    onClick={() =>
                      setOverlay({ type: "assignment", assignment })
                    }
                  />
                ))}
              <div className="inline-info">
                <CalendarDays size={18} />
                <Text>
                  Plan around the things that count. Start with the
                  highest-impact assignment.
                </Text>
              </div>
            </div>
          )}
          {overlay.type === "grades" && (
            <div className="report">
              <Badge tone="blue">SEMESTER 2 · SAMPLE DATA</Badge>
              <Text as="p" className="muted small">
                Your accumulated grades so far. Final results may change as
                remaining assessments are graded.
              </Text>
              {courses.map((item) => (
                <div className="report-row" key={item.id}>
                  <span className={`subject-icon subject-icon--${item.tone}`}>
                    <CourseIcon id={item.id} />
                  </span>
                  <div>
                    <Text className="bold">{item.shortTitle}</Text>
                    <Text className="muted tiny">
                      {item.credits} credits · {item.code}
                    </Text>
                  </div>
                  <Text className="report-grade">
                    {item.grade}
                    <span>/10</span>
                  </Text>
                </div>
              ))}
              <Button variant="primary" onClick={downloadReport}>
                <ArrowDownToLine size={17} />
                Download sample report
              </Button>
            </div>
          )}
          {overlay.type === "semester" && (
            <div className="report">
              <Badge tone="blue">SEMESTER 2 · 2024–2025</Badge>
              <Text as="p" className="muted small">Your current semester at a glance. This preview includes sample records for three featured subjects.</Text>
              <div className="report-row"><GraduationCap size={23} className="blue-text" /><div><Text className="bold">Credits earned</Text><Text className="muted tiny">6 credits to your next milestone</Text></div><Text className="report-grade">18<span>/24</span></Text></div>
              <div className="report-row"><CircleCheck size={23} className="blue-text" /><div><Text className="bold">Tasks completed</Text><Text className="muted tiny">Across your 3 featured courses</Text></div><Text className="report-grade">{summary.done}<span>/{summary.total}</span></Text></div>
              <div className="inline-info"><Info size={17} /><Text>Completion marks are kept only in this preview session. Earned credits and grades change only after official assessment.</Text></div>
              <Button variant="primary" onClick={() => setOverlay({ type: "grades" })}>View grade report<ArrowRight size={16} /></Button>
            </div>
          )}
          {overlay.type === "course-info" && course && (
            <div className="course-info-modal">
              <img src={`/images/${course.image}.webp`} alt="" />
              <Badge tone={course.tone}>
                {course.code} · {course.credits} CREDITS
              </Badge>
              <Text as="h3">{course.title}</Text>
              <Text className="muted small">
                Course coordinator: {course.teacher}
              </Text>
              <div className="course-session-summary">
                {sessions.filter((session) => session.courseId === course.id).map((session) => <div key={session.id}><CalendarDays size={17} /><div><Text className="bold">{weekdayLabels[session.weekday]} · {session.start}–{session.end}</Text><Text className="muted tiny">{session.kind} · {session.room}</Text></div></div>)}
                <Button variant="text" onClick={() => { setOverlay(null); setView("timetable") }}>Xem thời khóa biểu<ArrowRight size={15} /></Button>
              </div>
              <Text as="p" className="muted small">
                Lecture notes, assignment submissions, and official course
                materials are available in your university's Moodle portal. This
                preview uses sample course information.
              </Text>
            </div>
          )}
        </Modal>
      )}
      {toast && (
        <TopLayer className={`toast ${toast.tone === "error" ? "toast--error" : ""}`} role={toast.tone === "error" ? "alert" : "status"} raise={overlay?.type}>
          {toast.tone === "error" ? <TriangleAlert size={18} /> : <CircleCheck size={18} />}
          <Text>{toast.message}</Text>
          {toast.action && (
            <Button variant="unstyled" className="toast-action" onClick={() => { const { onClick } = toast.action!; setToast(null); onClick() }}>
              <RotateCw size={14} />{toast.action.label}
            </Button>
          )}
        </TopLayer>
      )}
    </div>
  )
}
