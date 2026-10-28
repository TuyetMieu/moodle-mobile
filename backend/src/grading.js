// Quy tắc điểm (thang 20) và máy chấm tự động.
//
// Điểm môn = Σ(điểm thành phần × trọng số %) / Σ(trọng số % của các thành phần đã có điểm) + bonus, tối đa 20.
// - Điểm thành phần = trung bình có trọng số (assignment.weight) của các bài ĐÃ CHẤM trong thành phần đó.
// - Bonus (nếu giáo viên bật) = trung bình điểm các bài bonus đã chấm / 20 × bonus tối đa của môn.
// Chưa có bài nào được chấm → điểm môn null.

export const MAX_SCORE = 20

const round1 = (value) => Math.round(value * 10) / 10

/** Trung bình có trọng số các bài đã chấm. Không có bài nào → null. */
function weightedScore(items) {
  let weight = 0
  let points = 0
  for (const a of items) {
    if (a.score == null) continue
    weight += a.weight
    points += a.score * a.weight
  }
  return weight === 0 ? null : points / weight
}

/**
 * Tính điểm một môn cho một sinh viên.
 * components: [{ id, name, weight }] — theo thứ tự hiển thị.
 * assignments: [{ componentId, isBonus, weight, score }] — các bài của môn này, kèm điểm của sinh viên.
 * course: { bonusEnabled, bonusMax }
 */
export function courseGrade(components, assignments, course) {
  const parts = components.map((component) => {
    const items = assignments.filter((a) => !a.isBonus && a.componentId === component.id)
    let totalWeight = 0
    let gradedWeight = 0
    for (const a of items) {
      totalWeight += a.weight
      if (a.score != null) gradedWeight += a.weight
    }
    let status = "IN_PROGRESS"
    if (gradedWeight === 0) status = "NOT_GRADED"
    else if (gradedWeight >= totalWeight) status = "GRADED"
    const score = weightedScore(items)
    return {
      id: component.id,
      name: component.name,
      weight: component.weight,
      score: score == null ? null : round1(score),
      exactScore: score,
      status,
      tasks: items.length,
      gradedShare: totalWeight === 0 ? 0 : Math.round((gradedWeight / totalWeight) * 100),
    }
  })

  let weight = 0
  let points = 0
  for (const part of parts) {
    if (part.exactScore == null) continue
    weight += part.weight
    points += part.exactScore * part.weight
  }
  const base = weight === 0 ? null : points / weight

  let bonus = null
  if (course.bonusEnabled) {
    const bonusScore = weightedScore(assignments.filter((a) => a.isBonus))
    bonus = bonusScore == null ? 0 : round1((bonusScore / MAX_SCORE) * course.bonusMax)
  }

  const grade = base == null ? null : Math.min(MAX_SCORE, round1(base + (bonus ?? 0)))
  return {
    components: parts.map(({ exactScore, ...rest }) => rest),
    baseGrade: base == null ? null : round1(base),
    bonus,
    grade,
  }
}

/**
 * % điểm môn mà một bài đóng góp: trọng số thành phần × phần của bài trong thành phần.
 * Bài bonus không tính vào 100% → null.
 */
export function contributionOf(assignment, component, componentTotalWeight) {
  if (assignment.isBonus || !component || componentTotalWeight === 0) return null
  return round1((component.weight * assignment.weight) / componentTotalWeight)
}

// ------------------------------------------------------------------ máy chấm

/** Băm FNV-1a 32 bit — để điểm tự động luôn giống nhau cho cùng một sinh viên và bài. */
function hash(text) {
  let h = 0x811c9dc5
  for (let i = 0; i < text.length; i++) {
    h ^= text.charCodeAt(i)
    h = Math.imul(h, 0x01000193)
  }
  return h >>> 0
}

/** Điểm máy chấm: 11–19.5 (bước 0.5), nộp muộn trừ 2 điểm. */
export function autoScore(userId, assignmentId, late) {
  const base = 11 + (hash(`${userId}:${assignmentId}`) % 18) * 0.5
  return Math.max(0, late ? base - 2 : base)
}

/**
 * Chấm các bài đã nộp nhưng chưa có điểm.
 * - Mặc định chỉ chấm bài đã quá hạn (máy chấm chạy sau deadline).
 * - options.assignmentId: chỉ chấm một bài; options.force: chấm luôn kể cả khi chưa tới hạn (giáo viên bấm "Chấm ngay").
 * Mỗi bài được chấm sẽ tạo một thông báo GRADE cho sinh viên. Trả về số bài đã chấm.
 */
export function autoGrade(db, now, options = {}) {
  const conditions = ["s.score IS NULL"]
  const params = {}
  if (!options.force) {
    conditions.push("a.due_at <= :now")
    params.now = now
  }
  if (options.assignmentId) {
    conditions.push("a.id = :assignmentId")
    params.assignmentId = options.assignmentId
  }
  const rows = db.prepare(`
    SELECT s.user_id, s.assignment_id, s.completed_at, a.due_at, a.title, a.course_id
    FROM submissions s JOIN assignments a ON a.id = s.assignment_id
    WHERE ${conditions.join(" AND ")}
  `).all(params)
  if (rows.length === 0) return 0

  const update = db.prepare(`
    UPDATE submissions SET score = ?, graded_at = ?, graded_by = 'AUTO' WHERE user_id = ? AND assignment_id = ?
  `)
  const notify = db.prepare(`
    INSERT OR REPLACE INTO notifications (id, user_id, kind, title, body, created_at, read_at, assignment_id, course_id, data)
    VALUES (?, ?, 'GRADE', 'New grade', ?, ?, NULL, ?, ?, ?)
  `)
  for (const row of rows) {
    const late = row.completed_at > row.due_at
    const score = autoScore(row.user_id, row.assignment_id, late)
    update.run(score, now, row.user_id, row.assignment_id)
    notify.run(`n-grade-${row.assignment_id}-${row.user_id}`, row.user_id, `${row.title} · ${score} / ${MAX_SCORE}`, now,
      row.assignment_id, row.course_id, JSON.stringify({ score, maxScore: MAX_SCORE, late }))
  }
  return rows.length
}
