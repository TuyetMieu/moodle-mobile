import type { Dispatch, SetStateAction } from "react"
import {
  ArrowRight,
  BookOpen,
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  Clock3,
  GraduationCap,
  Info,
  MapPin,
  UserRound,
} from "lucide-react"
import { Badge, Button, Card, Text } from "./ui"
import { courses, type Course } from "../data/learning"
import {
  monthDates,
  sameDay,
  sessionDuration,
  sessionsForDay,
  sessionTimestamp,
  shiftDay,
  shiftMonth,
  weekDates,
  weekdayLabels,
  type TimetableView,
} from "../data/timetable"

const fullDate = new Intl.DateTimeFormat("vi-VN", {
  weekday: "long",
  day: "numeric",
  month: "long",
})
const shortDate = new Intl.DateTimeFormat("vi-VN", {
  day: "2-digit",
  month: "2-digit",
})
const monthLabel = new Intl.DateTimeFormat("vi-VN", {
  month: "long",
  year: "numeric",
})

export default function TimetableScreen({
  now,
  selectCourse,
  selectedDate,
  setSelectedDate,
  calendarView,
  setCalendarView,
}: {
  now: number
  selectCourse: (course: Course) => void
  selectedDate: Date
  setSelectedDate: Dispatch<SetStateAction<Date>>
  calendarView: TimetableView
  setCalendarView: (view: TimetableView) => void
}) {
  const today = new Date(now)
  const isMonth = calendarView === "month"
  const days = isMonth ? monthDates(selectedDate) : weekDates(selectedDate)
  const periodDays = isMonth
    ? days.filter((day) => day.getMonth() === selectedDate.getMonth())
    : days
  const periodSessions = periodDays.flatMap(sessionsForDay)
  const sessions = sessionsForDay(selectedDate)
  const isToday = sameDay(selectedDate, today)
  const isCurrentPeriod = isMonth
    ? selectedDate.getMonth() === today.getMonth() &&
      selectedDate.getFullYear() === today.getFullYear()
    : sameDay(days[0], weekDates(today)[0])
  const hours = sessions.reduce(
    (sum, session) => sum + sessionDuration(session),
    0,
  )
  const totalHours = periodSessions.reduce(
    (sum, session) => sum + sessionDuration(session),
    0,
  )
  const nextSession = sessions.find(
    (session) => sessionTimestamp(selectedDate, session.end) > now,
  )

  return (
    <>
      <header className="app-header timetable-header">
        <div>
          <Text className="muted greeting-small">
            SẮP XẾP VIỆC HỌC, THEO CÁCH CỦA BẠN
          </Text>
          <Text as="h2">Thời khóa biểu</Text>
        </div>
        <Button
          variant="text"
          className="timetable-today"
          onClick={() => setSelectedDate(new Date(now))}
          disabled={isToday}
        >
          <CalendarDays size={16} />
          Hôm nay
        </Button>
      </header>
      <div className="screen-scroll timetable-content">
        <Card className="timetable-overview">
          <div className="timetable-overview-top">
            <Badge tone="blue">
              <CalendarDays size={12} />
              {isMonth
                ? isCurrentPeriod
                  ? "THÁNG NÀY"
                  : "THÁNG ĐÃ CHỌN"
                : isCurrentPeriod
                  ? "TUẦN NÀY"
                  : "TUẦN ĐÃ CHỌN"}
            </Badge>
            <Text>
              {shortDate.format(periodDays[0])} –{" "}
              {shortDate.format(periodDays[periodDays.length - 1])}
            </Text>
          </div>
          <Text as="h3">
            {isMonth
              ? "Lịch cả tháng, chủ động hơn."
              : "Một tuần học, thật rõ ràng."}
          </Text>
          <div className="timetable-week-stats">
            <div>
              <Text>
                {periodSessions.length}
                <Text>buổi học</Text>
              </Text>
              <Text>
                <BookOpen size={13} />
                {courses.length} môn học
              </Text>
            </div>
            <div>
              <Text>
                {totalHours}
                <Text>giờ học</Text>
              </Text>
              <Text>
                <Clock3 size={13} />
                Lý thuyết & thực hành
              </Text>
            </div>
          </div>
        </Card>
        <Card
          className={`timetable-calendar ${
            isMonth ? "timetable-calendar--month" : ""
          }`}
        >
          <div
            className="timetable-view-switch"
            role="group"
            aria-label="Kiểu xem lịch"
          >
            <Button
              variant="unstyled"
              aria-pressed={!isMonth}
              onClick={() => setCalendarView("week")}
            >
              Tuần
            </Button>
            <Button
              variant="unstyled"
              aria-pressed={isMonth}
              onClick={() => setCalendarView("month")}
            >
              Tháng
            </Button>
          </div>
          <div className="timetable-month">
            <Text as="h3">{monthLabel.format(selectedDate)}</Text>
            <div>
              <Button
                variant="icon"
                aria-label={isMonth ? "Tháng trước" : "Tuần trước"}
                onClick={() =>
                  setSelectedDate((date) =>
                    isMonth ? shiftMonth(date, -1) : shiftDay(date, -7),
                  )
                }
              >
                <ChevronLeft size={20} />
              </Button>
              <Button
                variant="icon"
                aria-label={isMonth ? "Tháng sau" : "Tuần sau"}
                onClick={() =>
                  setSelectedDate((date) =>
                    isMonth ? shiftMonth(date, 1) : shiftDay(date, 7),
                  )
                }
              >
                <ChevronRight size={20} />
              </Button>
            </div>
          </div>
          {isMonth && (
            <div className="timetable-month-weekdays" aria-hidden="true">
              {[1, 2, 3, 4, 5, 6, 0].map((day) => (
                <Text key={day}>{weekdayLabels[day]}</Text>
              ))}
            </div>
          )}
          <div
            className={`timetable-days ${
              isMonth ? "timetable-days--month" : ""
            }`}
            role="group"
            aria-label={isMonth ? "Chọn ngày trong tháng" : "Chọn ngày học"}
          >
            {days.map((day) => {
              const daySessions = sessionsForDay(day)
              const outside =
                isMonth && day.getMonth() !== selectedDate.getMonth()
              return (
                <Button
                  key={day.toDateString()}
                  variant="unstyled"
                  className={`timetable-day ${
                    sameDay(day, today) ? "is-today" : ""
                  } ${outside ? "is-outside-month" : ""}`}
                  aria-pressed={sameDay(day, selectedDate)}
                  aria-current={sameDay(day, today) ? "date" : undefined}
                  aria-label={`${fullDate.format(day)}, ${daySessions.length} buổi học`}
                  onClick={() => setSelectedDate(day)}
                >
                  {!isMonth && <Text>{weekdayLabels[day.getDay()]}</Text>}
                  <Text className="timetable-date-number">{day.getDate()}</Text>
                  {isMonth ? (
                    <span className="timetable-class-dots" aria-hidden="true">
                      {daySessions.map((session) => (
                        <span
                          key={session.id}
                          className={`timetable-class-dot timetable-class-dot--${courses.find((course) => course.id === session.courseId)!.tone}`}
                        />
                      ))}
                    </span>
                  ) : (
                    <span
                      className={`timetable-day-dot ${
                        daySessions.length > 0 ? "has-classes" : ""
                      }`}
                    />
                  )}
                </Button>
              )
            })}
          </div>
          {isMonth ? (
            <div className="timetable-month-legend">
              {courses.map((course) => (
                <Text key={course.id}>
                  <span
                    className={`timetable-class-dot timetable-class-dot--${course.tone}`}
                  />
                  {course.code}
                </Text>
              ))}
            </div>
          ) : (
            <Text className="timetable-calendar-note">
              <span />
              Có lịch học<Text>Giờ học theo giờ địa phương</Text>
            </Text>
          )}
        </Card>
        <section
          className="timetable-agenda"
          aria-labelledby="timetable-agenda-title"
          aria-live="polite"
        >
          <div className="timetable-agenda-heading">
            <div>
              <Text className="timetable-selected-date">
                {isToday ? "HÔM NAY" : fullDate.format(selectedDate)}
              </Text>
              <Text as="h3" id="timetable-agenda-title">
                {sessions.length > 0
                  ? `${sessions.length} buổi học · ${hours} giờ`
                  : "Hôm nay bạn được nghỉ"}
              </Text>
            </div>
            <Badge tone="blue">{shortDate.format(selectedDate)}</Badge>
          </div>
          <div className="timetable-session-list">
            {sessions.map((session) => {
              const course = courses.find(
                (item) => item.id === session.courseId,
              )!
              const start = sessionTimestamp(selectedDate, session.start)
              const end = sessionTimestamp(selectedDate, session.end)
              const finished = end <= now
              const live = start <= now && now < end
              return (
                <div
                  className={`timetable-session ${
                    finished ? "is-finished" : ""
                  }`}
                  key={session.id}
                >
                  <div className="timetable-session-time">
                    <Text>{session.start}</Text>
                    <Text>{session.end}</Text>
                    <span />
                    <Text>{sessionDuration(session)} giờ</Text>
                  </div>
                  <Button
                    variant="unstyled"
                    className={`timetable-class timetable-class--${course.tone}`}
                    onClick={() => selectCourse(course)}
                    aria-label={`${course.title}, ${session.kind}, ${session.start} đến ${session.end}, phòng ${session.room}`}
                  >
                    <div className="timetable-class-top">
                      <Badge tone={course.tone}>{session.kind}</Badge>
                      {live ? (
                        <Badge tone="green">
                          <span className="live-dot" />
                          Đang diễn ra
                        </Badge>
                      ) : finished ? (
                        <Text className="muted tiny">Đã kết thúc</Text>
                      ) : isToday && nextSession?.id === session.id ? (
                        <Badge tone="blue">Sắp tới</Badge>
                      ) : (
                        <ArrowRight size={15} />
                      )}
                    </div>
                    <Text as="h4">{course.title}</Text>
                    <Text className="timetable-class-code">
                      {course.code}
                      <span>·</span>
                      <GraduationCap size={13} />
                      {course.credits} tín chỉ
                    </Text>
                    <div className="timetable-class-details">
                      <Text>
                        <MapPin size={14} />
                        {session.room}
                      </Text>
                      <Text>
                        <UserRound size={14} />
                        {course.teacher}
                      </Text>
                    </div>
                  </Button>
                </div>
              )
            })}
            {sessions.length === 0 && (
              <Card className="timetable-empty">
                <span>
                  <CalendarDays size={32} />
                </span>
                <Text as="h4">Một chút thời gian cho chính mình.</Text>
                <Text as="p">
                  Không có lịch học vào ngày này. Bạn có thể ôn bài, hoàn thành
                  bài tập hoặc nghỉ ngơi.
                </Text>
                <Button
                  variant="text"
                  onClick={() => setSelectedDate((date) => weekDates(date)[1])}
                >
                  Xem lịch thứ Ba
                  <ArrowRight size={15} />
                </Button>
              </Card>
            )}
          </div>
        </section>
        <div className="timetable-note">
          <Info size={16} />
          <Text>
            Lịch minh họa lặp lại hằng tuần, chưa đồng bộ với USTH. Chạm vào một
            buổi học để mở lớp và bài tập.
          </Text>
        </div>
      </div>
    </>
  )
}
