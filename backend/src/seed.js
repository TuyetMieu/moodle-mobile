import { hashPassword } from "./auth.js"

const HOUR = 60 * 60 * 1000
const DAY = 24 * HOUR

export const DEMO_ACCOUNTS = [
  { username: "ba22-041", password: "usth2025", role: "STUDENT" },
  { username: "ba22-087", password: "usth2025", role: "STUDENT" },
  { username: "gv-minh", password: "usth2025", role: "TEACHER" },
  { username: "gv-linh", password: "usth2025", role: "TEACHER" },
  { username: "gv-quang", password: "usth2025", role: "TEACHER" },
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

const PROGRAMS = [
  { code: "ICT", name: "Information & Communication Technology" },
  { code: "SA", name: "Space & Applications" },
  { code: "BIO", name: "Biotechnology & Pharmacology" },
]

/** [username, họ tên, tên ngắn, viết tắt, mã, ngành, khoá, tín chỉ đã có, mốc tín chỉ] */
const USERS = [
  ["ba22-041", "STUDENT", "Hoang An", "An", "HA", "BA22-041", "ICT", "USTH · Class of 2026", 18, 24],
  ["ba22-087", "STUDENT", "Tran Minh Chau", "Chau", "MC", "BA22-087", "SA", "USTH · Class of 2026", 15, 24],
  ["gv-minh", "TEACHER", "Dr. Nguyen Minh", "Minh", "NM", "GV-0112", "ICT", "Faculty of ICT", 0, 0],
  ["gv-linh", "TEACHER", "Dr. Tran Linh", "Linh", "TL", "GV-0245", "BIO", "Faculty of Life Sciences", 0, 0],
  ["gv-quang", "TEACHER", "Dr. Le Quang", "Quang", "LQ", "GV-0318", "SA", "Faculty of Space & Applications", 0, 0],
]

const COURSES = [
  {
    id: "web", teacher: "gv-minh", title: "Web Application Development", shortTitle: "Web Development", code: "ICT 3103",
    image: "development", credits: 4, tone: "blue", bonusEnabled: true, bonusMax: 1,
    summary: "Build modern, accessible web applications: HTML & CSS foundations, JavaScript, component-based UI, REST APIs and deployment.",
    components: [["web-att", "Attendance", 10], ["web-labs", "Mid-term & labs", 30], ["web-final", "Final project", 60]],
  },
  {
    id: "bio", teacher: "gv-linh", title: "Molecular Biology", shortTitle: "Molecular Biology", code: "BIO 2201",
    image: "biology", credits: 3, tone: "green", bonusEnabled: false, bonusMax: 0,
    summary: "Structure and function of DNA, RNA and proteins; replication, transcription, translation and gene regulation, with weekly wet-lab practice.",
    components: [["bio-att", "Attendance", 10], ["bio-labs", "Mid-term & labs", 30], ["bio-final", "Final exam", 60]],
  },
  {
    id: "earth", teacher: "gv-quang", title: "Earth & Space Science", shortTitle: "Earth & Space Science", code: "ESS 2102",
    image: "earth", credits: 3, tone: "purple", bonusEnabled: true, bonusMax: 2,
    summary: "Earth systems, plate tectonics, remote sensing and the solar system, with labs on maps and satellite imagery.",
    components: [["earth-att", "Attendance", 10], ["earth-labs", "Labs", 20], ["earth-mid", "Mid-term", 20], ["earth-final", "Final project", 50]],
  },
]

/**
 * Bài tập: `due` tính bằng giờ so với thời điểm seed (âm = đã qua hạn).
 * `component` = id thành phần điểm, hoặc null cho bài bonus. `weight` = trọng số trong thành phần.
 */
const ASSIGNMENTS = [
  {
    id: "proposal", courseId: "web", component: "web-final", weight: 25, title: "Final project proposal", due: 6 + 42 / 60 + 18 / 3600,
    description: "Outline your web application idea, user needs, technical stack, and project milestones. Include a wireframe and a short explanation of your database design. This proposal is one milestone of the final project.",
  },
  {
    id: "web-demo", courseId: "web", component: "web-final", weight: 75, title: "Final project · Demo & report", due: 5 * 7 * 24,
    description: "Present a working version of your web application and submit a short technical report covering architecture, testing and deployment.",
  },
  {
    id: "midterm", courseId: "web", component: "web-labs", weight: 20, title: "Mid-term practical", due: 72,
    description: "Build a responsive interface using the concepts from weeks 1–8. Review component composition, state management, and accessible form patterns before the practical.",
  },
  {
    id: "lab3", courseId: "web", component: "web-labs", weight: 10, title: "Lab 03 · React components", due: -72,
    description: "Build a small reusable React component library (button, card, modal) with props, composition and accessible markup. Submit the repository link and a short README.",
  },
  {
    id: "web-bonus", courseId: "web", component: null, weight: 1, title: "Bonus · Accessibility audit", due: 10 * 24,
    description: "Optional: audit a public website with a screen reader and an automated checker, then write up three concrete fixes. Counts towards the course bonus.",
  },
  {
    id: "lab", courseId: "bio", component: "bio-labs", weight: 15, title: "Lab 04 · DNA replication", due: 20,
    description: "Complete the DNA replication worksheet and discuss your lab observations. Include your annotated diagram and answers to the review questions.",
  },
  {
    id: "bio-quiz", courseId: "bio", component: "bio-labs", weight: 10, title: "Quiz 05 · Gene expression", due: 48,
    description: "Review transcription, translation, and gene regulation. Complete the practice questions before taking the quiz in your university Moodle course.",
  },
  {
    id: "bio-reading", courseId: "bio", component: "bio-labs", weight: 5, title: "Reading reflection · Cell signalling", due: 96,
    description: "Read the cell signalling chapter and prepare a short reflection connecting receptor pathways to the examples discussed in class.",
  },
  {
    id: "bio-final", courseId: "bio", component: "bio-final", weight: 1, title: "Final exam", due: 6 * 7 * 24,
    description: "Written exam covering the whole course. Bring your student card; no notes allowed.",
  },
  {
    id: "earth-report", courseId: "earth", component: "earth-final", weight: 1, title: "Satellite imagery report", due: 120,
    description: "Compare two satellite images and discuss environmental changes visible in the data. Include your methods and a summary of your findings.",
  },
  {
    id: "earth-lab1", courseId: "earth", component: "earth-labs", weight: 1, title: "Lab 01 · Reading topographic maps", due: -30 * 24,
    description: "Interpret contour lines, scale and relief on the provided topographic sheet and answer the guided questions.",
  },
  {
    id: "earth-lab2", courseId: "earth", component: "earth-labs", weight: 1, title: "Lab 02 · Plate tectonics model", due: -16 * 24,
    description: "Use the plate boundary dataset to map earthquake and volcano distributions, then explain the patterns you observe.",
  },
  {
    id: "earth-quiz", courseId: "earth", component: "earth-mid", weight: 1, title: "Mid-term quiz · Solar system", due: -9 * 24,
    description: "Timed quiz covering planetary formation, orbits and the structure of the solar system.",
  },
  {
    id: "earth-bonus", courseId: "earth", component: null, weight: 1, title: "Bonus · Field trip journal", due: 14 * 24,
    description: "Optional: keep a short illustrated journal of the geology field trip. Counts towards the course bonus.",
  },
]

/** Điểm danh hằng tuần (thang 20), thuộc thành phần Attendance. */
const CHECK_IN_SCORES = {
  web: [16, 18, 14, 17, 16, 18, 15],
  bio: [16, 17, 16, 15, 18, 16],
  earth: [18, 20, 18, 19, 17, 18],
}

/** Kết quả đã chấm của sinh viên chính (BA22-041), thang 20. */
const GRADED = {
  lab3: { score: 18, feedback: "Excellent component structure and thoughtful accessibility choices. Your reusable React component library has been reviewed." },
  "earth-lab1": { score: 18, feedback: "Accurate contour reading. Label your cross-section axes next time." },
  "earth-lab2": { score: 19, feedback: "Very clear map and a convincing explanation of subduction zones." },
  "earth-quiz": { score: 17, feedback: null },
}

const CLASS_SESSIONS = [
  { id: "bio-lecture", courseId: "bio", weekday: 1, start: "08:00", end: "10:00", kind: "LECTURE", room: "A · 305" },
  { id: "web-lecture", courseId: "web", weekday: 2, start: "09:00", end: "11:00", kind: "LECTURE", room: "A · 402" },
  { id: "earth-lecture", courseId: "earth", weekday: 2, start: "14:00", end: "16:00", kind: "LECTURE", room: "B · 201" },
  { id: "bio-lab", courseId: "bio", weekday: 3, start: "13:00", end: "15:00", kind: "LAB", room: "Lab · B203" },
  { id: "web-lab", courseId: "web", weekday: 4, start: "14:00", end: "16:00", kind: "LAB", room: "A · 402" },
  { id: "earth-lab", courseId: "earth", weekday: 5, start: "09:00", end: "11:00", kind: "LAB", room: "Lab · B204" },
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

/**
 * Tin của ngành / toàn trường: [id, audience, ngành, loại, tiêu đề, nội dung, người đăng (username hoặc tên phòng ban),
 * sự kiện diễn ra sau bao nhiêu giờ (null = không phải sự kiện), địa điểm, đăng cách đây bao nhiêu giờ].
 */
const BULLETINS = [
  ["b-ict-career", "PROGRAM", "ICT", "EVENT", "ICT Career Day 2026",
    "Meet 20+ tech companies, join CV clinics and mock interviews. Open to all ICT students — bring a printed CV.",
    "ICT Department", 5 * 24 + 1, "Hall A · Building A21", 26],
  ["b-ict-hackathon", "PROGRAM", "ICT", "NOTICE", "USTH Hackathon: registration is open",
    "Teams of 3–4 students. Theme: tools that make campus life easier. Registration closes in 10 days.",
    "gv-minh", null, null, 50],
  ["b-sa-visit", "PROGRAM", "SA", "EVENT", "Visit to the satellite ground station",
    "Guided visit of the Vietnam National Space Center ground station. 30 seats — register with the SA office.",
    "SA Department", 8 * 24 + 2, "VNSC · Hoa Lac", 30],
  ["b-bio-seminar", "PROGRAM", "BIO", "EVENT", "Seminar: CRISPR in plant research",
    "Guest talk followed by a Q&A session with the research team.",
    "gv-linh", 3 * 24 + 6, "Room C · 101", 20],
  ["b-all-exams", "ALL", null, "NOTICE", "Mid-term exam schedule published",
    "The official mid-term schedule is now available on the student portal. Check your rooms and seat numbers.",
    "Academic Affairs Office", null, null, 48],
  ["b-all-lecture", "ALL", null, "EVENT", "Open lecture: AI in science",
    "A public lecture for all students on how machine learning is changing research in physics, biology and earth science.",
    "USTH Communications", 3 * 24 + 8, "Auditorium · Building A21", 6],
  ["b-all-library", "ALL", null, "NOTICE", "Library opens until 22:00 during exams",
    "The library extends its opening hours for the next three weeks. Silent study rooms can be booked online.",
    "USTH Library", null, null, 96],
]

export function seed(db, now) {
  const term = academicTerm(now)
  const weekStart = startOfWeek(now)
  const semesterId = `${term.academicYear}-${term.label}`.replace(/\s+/g, "-").toLowerCase()
  const weeksElapsed = 9

  db.prepare("INSERT INTO semesters (id, label, academic_year, starts_at, ends_at) VALUES (?, ?, ?, ?, ?)")
    .run(semesterId, term.label, term.academicYear, weekStart - weeksElapsed * 7 * DAY, weekStart + 9 * 7 * DAY)

  const insertProgram = db.prepare("INSERT INTO programs (code, name) VALUES (?, ?)")
  for (const p of PROGRAMS) insertProgram.run(p.code, p.name)

  const insertUser = db.prepare(`
    INSERT INTO users (username, password_hash, role, full_name, short_name, initials, member_code, program_code, cohort,
                       credits_earned, credits_milestone)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `)
  const userId = {}
  for (const [username, role, fullName, shortName, initials, code, program, cohort, earned, milestone] of USERS) {
    const password = DEMO_ACCOUNTS.find((a) => a.username === username).password
    userId[username] = Number(insertUser.run(username, hashPassword(password), role, fullName, shortName, initials, code,
      program, cohort, earned, milestone).lastInsertRowid)
  }
  const an = userId["ba22-041"]
  const chau = userId["ba22-087"]

  const insertCourse = db.prepare(`
    INSERT INTO courses (id, semester_id, teacher_id, title, short_title, code, image, credits, tone, summary, bonus_enabled, bonus_max)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `)
  const insertComponent = db.prepare("INSERT INTO grade_components (id, course_id, name, weight, position) VALUES (?, ?, ?, ?, ?)")
  for (const c of COURSES) {
    insertCourse.run(c.id, semesterId, userId[c.teacher], c.title, c.shortTitle, c.code, c.image, c.credits, c.tone, c.summary,
      c.bonusEnabled ? 1 : 0, c.bonusMax)
    c.components.forEach(([id, name, weight], index) => insertComponent.run(id, c.id, name, weight, index))
  }

  const enroll = db.prepare("INSERT INTO enrollments (user_id, course_id) VALUES (?, ?)")
  for (const c of COURSES) enroll.run(an, c.id)
  enroll.run(chau, "web")
  enroll.run(chau, "earth")

  const insertAssignment = db.prepare(`
    INSERT INTO assignments (id, course_id, component_id, is_bonus, title, description, weight, due_at, created_at, updated_at)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `)
  const submit = db.prepare(`
    INSERT INTO submissions (user_id, assignment_id, completed_at, score, feedback, graded_at, graded_by) VALUES (?, ?, ?, ?, ?, ?, ?)
  `)

  for (const a of ASSIGNMENTS) {
    const dueAt = Math.round(now + a.due * HOUR)
    const createdAt = Math.min(now - 3 * DAY, dueAt - 7 * DAY)
    insertAssignment.run(a.id, a.courseId, a.component, a.component ? 0 : 1, a.title, a.description, a.weight, dueAt, createdAt, createdAt)
  }

  // Bài đã qua hạn của sinh viên chính đều đã nộp và được chấm.
  for (const [id, result] of Object.entries(GRADED)) {
    const assignment = ASSIGNMENTS.find((a) => a.id === id)
    const dueAt = Math.round(now + assignment.due * HOUR)
    submit.run(an, id, dueAt - 5 * HOUR, result.score, result.feedback, dueAt + DAY, "TEACHER")
  }

  const componentOf = { web: "web-att", bio: "bio-att", earth: "earth-att" }
  for (const [courseId, scores] of Object.entries(CHECK_IN_SCORES)) {
    scores.forEach((score, index) => {
      const week = index + 1
      const id = `${courseId}-checkin-${week}`
      // Tuần gần nhất kết thúc 2 ngày trước; các tuần trước lùi dần 7 ngày.
      const dueAt = now - 2 * DAY - (scores.length - week) * 7 * DAY
      insertAssignment.run(id, courseId, componentOf[courseId], 0, `Week ${String(week).padStart(2, "0")} · Participation check-in`,
        "Attend the session and complete the short in-class check-in. Each weekly check-in counts equally towards the attendance component.",
        1, dueAt, dueAt - 7 * DAY, dueAt - 7 * DAY)
      submit.run(an, id, dueAt - 2 * HOUR, score, null, dueAt, "TEACHER")
      if (courseId !== "bio") submit.run(chau, id, dueAt - 3 * HOUR, Math.max(12, score - 1), null, dueAt, "TEACHER")
    })
  }

  // Sinh viên thứ hai có tiến độ khác để thấy dữ liệu tách theo từng tài khoản.
  submit.run(chau, "proposal", now - 20 * HOUR, null, null, null, null)
  submit.run(chau, "lab3", now - 76 * HOUR, 16, "Solid work. Consider extracting shared styles into tokens.", now - 50 * HOUR, "TEACHER")
  submit.run(chau, "earth-lab1", now - 31 * DAY, 16, null, now - 29 * DAY, "TEACHER")
  submit.run(chau, "earth-lab2", now - 17 * DAY, 17, null, now - 15 * DAY, "TEACHER")

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
    INSERT INTO notifications (id, user_id, kind, title, body, created_at, read_at, assignment_id, course_id, data)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `)
  const grade = (score) => JSON.stringify({ score, maxScore: 20 })
  notify.run("n-grade-lab3", an, "GRADE", "New grade", "Lab 03 · React components · 18 / 20", now - 2 * HOUR, null, "lab3", "web", grade(18))
  notify.run("n-msg-web", an, "ANNOUNCEMENT", "Week 09 resources are ready", "Lecture notes and practice materials are now available.",
    now - 3 * HOUR, null, null, "web", null)
  notify.run("n-grade-earth", an, "GRADE", "New grade", "Mid-term quiz · Solar system · 17 / 20", now - 8 * DAY, now - 7 * DAY,
    "earth-quiz", "earth", grade(17))
  notify.run("n-grade-lab3-chau", chau, "GRADE", "New grade", "Lab 03 · React components · 16 / 20", now - 2 * HOUR, null, "lab3", "web", grade(16))

  const insertBulletin = db.prepare(`
    INSERT INTO bulletins (id, audience, program_code, kind, title, body, author_id, author_name, event_at, location, created_at)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `)
  for (const [id, audience, program, kind, title, body, author, eventIn, location, postedAgo] of BULLETINS) {
    const authorId = userId[author] ?? null
    const authorName = authorId ? USERS.find((u) => u[0] === author)[2] : author
    insertBulletin.run(id, audience, program, kind, title, body, authorId, authorName,
      eventIn == null ? null : now + eventIn * HOUR, location, now - postedAgo * HOUR)
  }
  // An đã đọc tin thư viện (cũ nhất).
  db.prepare("INSERT INTO bulletin_reads (user_id, bulletin_id, read_at) VALUES (?, ?, ?)").run(an, "b-all-library", now - 90 * HOUR)
}
