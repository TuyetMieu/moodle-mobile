import { createContext, useContext } from "react"
import { api } from "./api"
import type { Assignment, ClassSession, Course } from "./types"

export type LearningData = {
  courses: Course[]
  assignments: Assignment[]
  sessions: ClassSession[]
}

export async function loadLearningData(signal: AbortSignal): Promise<LearningData> {
  const [courses, assignments, sessions] = await Promise.all([
    api.getCourses(signal),
    api.getAssignments({}, signal),
    api.getTimetable(signal),
  ])
  return { courses, assignments, sessions }
}

export const LearningDataContext = createContext<LearningData | null>(null)

export function useLearningData(): LearningData {
  const data = useContext(LearningDataContext)
  if (!data) throw new Error("useLearningData must be used inside LearningDataContext.Provider")
  return data
}
