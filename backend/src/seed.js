import { hashPassword } from "./auth.js"

const HOUR = 60 * 60 * 1000
const DAY = 24 * HOUR

export const DEMO_ACCOUNTS = [
  { username: "ba22-041", password: "usth2025" },
  { username: "ba22-087", password: "usth2025" },
]

/** Học kỳ theo lịch USTH: kỳ 1 từ tháng 9 đến tháng 1, kỳ 2 từ tháng 2 đến tháng 7. */
export function academicTerm(now) {
  const date = new Date(now)
  const month = date.getMonth() + 1
  const year = date.getFullYear()
  if (month >= 9) return { label: "Semester 1", academicYear: `${year}–${year + 1}` }
  if (month === 1) return { label: "Semester 1", academicYear: `${year - 1}–${year}` }
  return { label: "Semester 2", academicYear: `${year - 1}–${year}` }
}

function startOfWeek(now) {
  const date = new Date(now)
  date.setHours(0, 0, 0, 0)
  date.setDate(date.getDate() - ((date.getDay() + 6) % 7))
  return date.getTime()
}

const COURSES = [
  {
    id: "web", title: "Web Application Development", shortTitle: "Web Development", code: "ICT 3103",
    teacher: "Dr. Nguyen Minh", teacherInitials: "NM", image: "development", credits: 4, tone: "blue",
    summary: "Build modern, accessible web applications: HTML & CSS foundations, JavaScript, component-based UI, REST APIs and deployment.",
  },
  {
    id: "bio", title: "Molecular Biology", shortTitle: "Molecular Biology", code: "BIO 2201",
    teacher: "Dr. Tran Linh", teacherInitials: "TL", image: "biology", credits: 3, tone: "green",
    summary: "Structure and function of DNA, RNA and proteins; replication, transcription, translation and gene regulation, with weekly wet-lab practice.",
  },
  {
    id: "earth", title: "Earth & Space Science", shortTitle: "Earth & Space Science", code: "ESS 2102",
    teacher: "Dr. Le Quang", teacherInitials: "LQ", image: "earth", credits: 3, tone: "purple",
    summary: "Earth systems, plate tectonics, remote sensing and the solar system, with labs on maps and satellite imagery.",
  },
]

/** Bài tập: `due` tính bằng giờ so với thời điểm seed (âm = đã qua hạn). */
const ASSIGNMENTS = [
  {
    id: "proposal", courseId: "web", title: "Final project proposal", category: "FINAL_PROJECT", contribution: 15, due: 6 + 42 / 60 + 18 / 3600,
    description: "Outline your web application idea, user needs, technical stack, and project milestones. Include a wireframe and a short explanation of your database design. This proposal is one milestone of the final project, not the entire 60% component.",
  },
  {
    id: "midterm", courseId: "web", title: "Mid-term practical", category: "MIDTERM_LABS", contribution: 20, due: 72,
    description: "Build a responsive interface using the concepts from weeks 1–8. Review component composition, state management, and accessible form patterns before the practical.",
  },
  {
    id: "lab3", courseId: "web", title: "Lab 03 · React components", category: "MIDTERM_LABS", contribution: 10, due: -72,
    description: "Build a small reusable React component library (button, card, modal) with props, composition and accessible markup. Submit the repository link and a short README.",
  },
  {
    id: "lab", courseId: "bio", title: "Lab 04 · DNA replication", category: "MIDTERM_LABS", contribution: 15, due: 20,
    description: "Complete the DNA replication worksheet and discuss your lab observations. Include your annotated diagram and answers to the review questions.",
  },
  {
    id: "bio-quiz", courseId: "bio", title: "Quiz 05 · Gene expression", category: "MIDTERM_LABS", contribution: 10, due: 48,
    description: "Review transcription, translation, and gene regulation. Complete the practice questions before taking the quiz in your university Moodle course.",
  },
  {
    id: "bio-reading", courseId: "bio", title: "Reading reflection · Cell signalling", category: "MIDTERM_LABS", contribution: 5, due: 96,
    description: "Read the cell signalling chapter and prepare a short reflection connecting receptor pathways to the examples discussed in class.",
  },
  {
    id: "earth-report", courseId: "earth", title: "Satellite imagery report", category: "FINAL_PROJECT", contribution: 30, due: 120,
    description: "Compare two satellite images and discuss environmental changes visible in the data. Include your methods and a summary of your findings.",
  },
  {
    id: "earth-lab1", courseId: "earth", title: "Lab 01 · Reading topographic maps", category: "MIDTERM_LABS", contribution: 10, due: -30 * 24,
    description: "Interpret contour lines, scale and relief on the provided topographic sheet and answer the guided questions.",
  },
  {
    id: "earth-lab2", courseId: "earth", title: "Lab 02 · Plate tectonics model", category: "MIDTERM_LABS", contribution: 10, due: -16 * 24,
    description: "Use the plate boundary dataset to map earthquake and volcano distributions, then explain the patterns you observe.",
  },
  {
    id: "earth-quiz", courseId: "earth", title: "Mid-term quiz · Solar system", category: "MIDTERM_LABS", contribution: 10, due: -9 * 24,
    description: "Timed quiz covering planetary formation, orbits and the structure of the solar system.",
  },
]

/** Điểm danh/tham gia hằng tuần (thuộc thành phần Attendance 10%, mỗi tuần 1%). */
const CHECK_IN_SCORES = {
  web: [8, 9, 7, 8.5, 8, 9, 7.5],
  bio: [8, 8.5, 8, 7.5, 9, 8],
  earth: [9, 10, 9, 9.5, 8.5, 9],
}

/** Kết quả đã chấm cho sinh viên chính (BA22-041). */
const GRADED = {
  lab3: { score: 9.0, feedback: "Excellent component structure and thoughtful accessibility choices. Your reusable React component library has been reviewed." },
  "earth-lab1": { score: 9.0, feedback: "Accurate contour reading. Label your cross-section axes next time." },
  "earth-lab2": { score: 9.5, feedback: "Very clear map and a convincing explanation of subduction zones." },
  "earth-quiz": { score: 8.5, feedback: null },
}

const CLASS_SESSIONS = [
  { id: "bio-lecture", courseId: "bio", weekday: 1, start: "08:00", end: "10:00", kind: "Lý thuyết", room: "A · 305" },
  { id: "web-lecture", courseId: "web", weekday: 2, start: "09:00", end: "11:00", kind: "Lý thuyết", room: "A · 402" },
  { id: "earth-lecture", courseId: "earth", weekday: 2, start: "14:00", end: "16:00", kind: "Lý thuyết", room: "B · 201" },
  { id: "bio-lab", courseId: "bio", weekday: 3, start: "13:00", end: "15:00", kind: "Thực hành", room: "Phòng lab · B203" },
  { id: "web-lab", courseId: "web", weekday: 4, start: "14:00", end: "16:00", kind: "Thực hành", room: "A · 402" },
  { id: "earth-lab", courseId: "earth", weekday: 5, start: "09:00", end: "11:00", kind: "Thực hành", room: "Phòng lab · B204" },
]

/** Tài liệu môn học: [tuần, tiêu đề, loại, định dạng, dung lượng KB]. */
const RESOURCES = {
  web: [
    [9, "Week 09 · State management & data fetching", "SLIDES", "PDF", 2450],
    [9, "Practice set · Forms and validation", "EXERCISE", "ZIP", 830],
    [8, "Week 08 · Component composition", "SLIDES", "PDF", 1980],
    [8, "Lecture notes · Accessibility checklist", "NOTES", "PDF", 410],
  ],
  bio: [
    [9, "Week 09 · Gene regulation in prokaryotes", "SLIDES", "PDF", 3120],
    [9, "Lab 04 worksheet · DNA replication", "EXERCISE", "PDF", 560],
    [8, "Week 08 · Transcription & translation", "SLIDES", "PDF", 2760],
  ],
  earth: [
    [9, "Week 09 · Remote sensing basics", "SLIDES", "PDF", 4210],
    [9, "Landsat sample scenes for the final report", "DATASET", "ZIP", 18400],
    [8, "Week 08 · The solar system", "SLIDES", "PDF", 3650],
  ],
}

export function seed(db, now) {
  const term = academicTerm(now)
  const weekStart = startOfWeek(now)
  const semesterId = `${term.academicYear}-${term.label}`.replace(/\s+/g, "-").toLowerCase()
  const weeksElapsed = 9

  db.prepare("INSERT INTO semesters (id, label, academic_year, starts_at, ends_at) VALUES (?, ?, ?, ?, ?)")
    .run(semesterId, term.label, term.academicYear, weekStart - weeksElapsed * 7 * DAY, weekStart + 9 * 7 * DAY)

  const insertUser = db.prepare(`
    INSERT INTO users (username, password_hash, full_name, short_name, initials, student_id, program, cohort, credits_earned, credits_milestone)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `)
  const an = insertUser.run(DEMO_ACCOUNTS[0].username, hashPassword(DEMO_ACCOUNTS[0].password),
    "Hoang An", "An", "HA", "BA22-041", "Information & Communication Technology", "USTH · Class of 2026", 18, 24).lastInsertRowid
  const chau = insertUser.run(DEMO_ACCOUNTS[1].username, hashPassword(DEMO_ACCOUNTS[1].password),
    "Tran Minh Chau", "Chau", "MC", "BA22-087", "Information & Communication Technology", "USTH · Class of 2026", 15, 24).lastInsertRowid

  const insertCourse = db.prepare(`
    INSERT INTO courses (id, semester_id, title, short_title, code, teacher, teacher_initials, image, credits, tone, summary)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `)
  for (const c of COURSES) {
    insertCourse.run(c.id, semesterId, c.title, c.shortTitle, c.code, c.teacher, c.teacherInitials, c.image, c.credits, c.tone, c.summary)
  }

  const enroll = db.prepare("INSERT INTO enrollments (user_id, course_id) VALUES (?, ?)")
  for (const c of COURSES) enroll.run(an, c.id)
  enroll.run(chau, "web")
  enroll.run(chau, "earth")

  const insertAssignment = db.prepare(`
    INSERT INTO assignments (id, course_id, title, category, contribution, description, due_at) VALUES (?, ?, ?, ?, ?, ?, ?)
  `)
  const submit = db.prepare(`
    INSERT INTO submissions (user_id, assignment_id, completed_at, score, feedback) VALUES (?, ?, ?, ?, ?)
  `)

  for (const a of ASSIGNMENTS) {
    insertAssignment.run(a.id, a.courseId, a.title, a.category, a.contribution, a.description, Math.round(now + a.due * HOUR))
  }

  // Bài đã qua hạn của sinh viên chính đều đã nộp và được chấm.
  for (const [id, result] of Object.entries(GRADED)) {
    const assignment = ASSIGNMENTS.find((a) => a.id === id)
    submit.run(an, id, Math.round(now + assignment.due * HOUR - 5 * HOUR), result.score, result.feedback)
  }

  for (const [courseId, scores] of Object.entries(CHECK_IN_SCORES)) {
    scores.forEach((score, index) => {
      const week = index + 1
      const id = `${courseId}-checkin-${week}`
      // Tuần gần nhất kết thúc 2 ngày trước; các tuần trước lùi dần 7 ngày.
      const dueAt = now - 2 * DAY - (scores.length - week) * 7 * DAY
      insertAssignment.run(id, courseId, `Week ${String(week).padStart(2, "0")} · Participation check-in`, "ATTENDANCE", 1,
        "Attend the session and complete the short in-class check-in on Moodle. Each weekly check-in counts 1% towards the attendance component.",
        dueAt)
      submit.run(an, id, dueAt - 2 * HOUR, score, null)
      if (courseId !== "bio") submit.run(chau, id, dueAt - 3 * HOUR, Math.max(6, score - 0.5), null)
    })
  }

  // Sinh viên thứ hai có tiến độ khác để thấy dữ liệu tách theo từng tài khoản.
  submit.run(chau, "proposal", now - 20 * HOUR, null, null)
  submit.run(chau, "lab3", now - 76 * HOUR, 8.0, "Solid work. Consider extracting shared styles into tokens.")
  submit.run(chau, "earth-lab1", now - 31 * DAY, 8.0, null)
  submit.run(chau, "earth-lab2", now - 17 * DAY, 8.5, null)

  const insertSession = db.prepare(`
    INSERT INTO class_sessions (id, course_id, weekday, start_time, end_time, kind, room) VALUES (?, ?, ?, ?, ?, ?, ?)
  `)
  for (const s of CLASS_SESSIONS) insertSession.run(s.id, s.courseId, s.weekday, s.start, s.end, s.kind, s.room)

  const insertAnnouncement = db.prepare(`
    INSERT INTO announcements (id, course_id, kind, title, body, posted_at, pinned) VALUES (?, ?, ?, ?, ?, ?, ?)
  `)
  const todayAt = (h, m) => { const d = new Date(now); d.setHours(h, m, 0, 0); return Math.min(d.getTime(), now) }
  const reminders = {
    web: "Please check your upcoming classwork and make sure your project milestones are on track. Bring your questions to our next session!",
    bio: "Lab 04 is due soon. Bring your lab coat and safety glasses to Wednesday's practical, and review the replication fork diagram beforehand.",
    earth: "The satellite imagery report is our main final-project milestone. Start by choosing your two images this week so you have time for the analysis.",
  }
  for (const c of COURSES) {
    insertAnnouncement.run(`${c.id}-reminder`, c.id, "ANNOUNCEMENT", "A little reminder for this week", reminders[c.id], todayAt(8, 30), 1)
    insertAnnouncement.run(`${c.id}-week9`, c.id, "RESOURCE", "Week 09 resources are ready",
      "Lecture notes and practice materials are now available. Review the key concepts before starting your next assignment.", now - 6 * DAY, 0)
  }

  const insertResource = db.prepare(`
    INSERT INTO course_resources (id, course_id, week, title, kind, file_type, size_kb, published_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
  `)
  for (const [courseId, items] of Object.entries(RESOURCES)) {
    items.forEach(([week, title, kind, fileType, sizeKb], index) => {
      // Tài liệu tuần 9 đăng cùng lúc với thông báo "Week 09 resources are ready" (6 ngày trước).
      insertResource.run(`${courseId}-res-${index + 1}`, courseId, week, title, kind, fileType, sizeKb, now - 6 * DAY - (9 - week) * 7 * DAY)
    })
  }

  const notify = db.prepare(`
    INSERT INTO notifications (id, user_id, kind, title, body, created_at, read_at, assignment_id, course_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
  `)
  notify.run("n-grade-lab3", an, "GRADE", "A new grade to feel good about", "Lab 03 · React components · 9.0 / 10", now - 2 * HOUR, null, "lab3", "web")
  notify.run("n-msg-web", an, "MESSAGE", "A note from Dr. Nguyen Minh", "Week 09 resources are ready to explore.", now - 3 * HOUR, null, null, "web")
  notify.run("n-grade-earth", an, "GRADE", "Mid-term quiz graded", "Mid-term quiz · Solar system · 8.5 / 10", now - 8 * DAY, now - 7 * DAY, "earth-quiz", "earth")
  notify.run("n-grade-lab3-chau", chau, "GRADE", "Your Lab 03 grade is ready", "Lab 03 · React components · 8.0 / 10", now - 2 * HOUR, null, "lab3", "web")
}
