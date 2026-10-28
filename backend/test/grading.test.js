import { describe, test } from "node:test"
import assert from "node:assert/strict"
import { autoScore, contributionOf, courseGrade } from "../src/grading.js"

const components = [
  { id: "att", name: "Attendance", weight: 10 },
  { id: "mid", name: "Midterm", weight: 30 },
  { id: "fin", name: "Final", weight: 60 },
]
const task = (componentId, score, weight = 1) => ({ componentId, isBonus: false, weight, score })
const bonusTask = (score) => ({ componentId: null, isBonus: true, weight: 1, score })

describe("courseGrade", () => {
  test("no graded work → no grade", () => {
    const result = courseGrade(components, [task("att", null), task("fin", null)], { bonusEnabled: false, bonusMax: 0 })
    assert.equal(result.grade, null)
    assert.deepEqual(result.components.map((c) => c.status), ["NOT_GRADED", "NOT_GRADED", "NOT_GRADED"])
  })

  test("weighted average over the components that already have a grade", () => {
    const result = courseGrade(components, [task("att", 16), task("mid", 12, 2), task("mid", 18, 1), task("fin", null)],
      { bonusEnabled: false, bonusMax: 0 })
    // Midterm = (12×2 + 18) / 3 = 14; môn = (16×10 + 14×30) / 40 = 14.5
    assert.equal(result.components[1].score, 14)
    assert.equal(result.grade, 14.5)
    assert.equal(result.bonus, null)
    assert.deepEqual(result.components.map((c) => c.status), ["GRADED", "GRADED", "NOT_GRADED"])
  })

  test("bonus is added on top and capped at 20", () => {
    const course = { bonusEnabled: true, bonusMax: 2 }
    const half = courseGrade(components, [task("att", 15), bonusTask(10)], course)
    assert.deepEqual([half.baseGrade, half.bonus, half.grade], [15, 1, 16])
    const capped = courseGrade(components, [task("att", 19.5), bonusTask(20)], course)
    assert.deepEqual([capped.bonus, capped.grade], [2, 20])
    const noneYet = courseGrade(components, [task("att", 15), bonusTask(null)], course)
    assert.deepEqual([noneYet.bonus, noneYet.grade], [0, 15])
  })

  test("partially graded component is IN_PROGRESS", () => {
    const result = courseGrade(components, [task("mid", 12), task("mid", null)], { bonusEnabled: false, bonusMax: 0 })
    assert.equal(result.components[1].status, "IN_PROGRESS")
    assert.equal(result.components[1].gradedShare, 50)
  })
})

test("contribution = component weight × the task's share of the component", () => {
  assert.equal(contributionOf({ isBonus: false, weight: 25 }, { weight: 60 }, 100), 15)
  assert.equal(contributionOf({ isBonus: true, weight: 1 }, null, 0), null)
})

test("auto scores are deterministic, in range, and late work loses 2 points", () => {
  for (let user = 1; user <= 20; user++) {
    const score = autoScore(user, "lab", false)
    assert.ok(score >= 11 && score <= 19.5)
    assert.equal(score * 2, Math.round(score * 2)) // bước 0.5
    assert.equal(autoScore(user, "lab", false), score)
    assert.equal(autoScore(user, "lab", true), score - 2)
  }
})
