import type { Assignment, AssignmentFilter, ClassSession, Course } from "./types"

// API contract — keep in sync with the API sheet.
export type HttpMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE"

export type ApiErrorBody = {
  message?: string
  code?: string
  errors?: Record<string, string[]>
}

/** GET /assignments?status=&course= — both optional. */
export type AssignmentQuery = { status?: AssignmentFilter; course?: string }

export type CompleteAssignmentResponse = { id: string; completed: boolean }

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "/api"
const USE_MOCK = import.meta.env.VITE_USE_MOCK === "true"
const TIMEOUT_MS = 10_000

/** Every failure (HTTP, offline, network, timeout, bad JSON) surfaces as an ApiError. status 0 = no response. */
export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly details?: Record<string, string[]>

  constructor(status: number, code: string, message: string, details?: Record<string, string[]>) {
    super(message)
    this.name = "ApiError"
    this.status = status
    this.code = code
    this.details = details
  }
}

export type RequestOptions = {
  method?: HttpMethod
  body?: unknown
  query?: Record<string, string | number | boolean | undefined>
  headers?: HeadersInit
  signal?: AbortSignal
}

function buildQuery(query: RequestOptions["query"]): string {
  if (!query) return ""
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined) params.set(key, String(value))
  }
  const text = params.toString()
  return text ? `?${text}` : ""
}

// Mock mode: GET /courses/web → /mock/courses/web.json, POST /x → /mock/x.post.json.
function mockUrl(path: string, method: HttpMethod): string {
  const clean = path.replace(/^\/+|\/+$/g, "")
  const suffix = method === "GET" ? "" : `.${method.toLowerCase()}`
  return `${import.meta.env.BASE_URL}mock/${clean}${suffix}.json`
}

const defaultMessages: Record<number, string> = {
  400: "Yêu cầu không hợp lệ.",
  401: "Phiên đăng nhập đã hết hạn.",
  403: "Bạn không có quyền thực hiện thao tác này.",
  404: "Không tìm thấy dữ liệu.",
  409: "Dữ liệu đã bị thay đổi, vui lòng thử lại.",
  422: "Dữ liệu gửi lên không hợp lệ.",
  500: "Máy chủ gặp sự cố, vui lòng thử lại sau.",
}

async function toApiError(response: Response): Promise<ApiError> {
  let body: ApiErrorBody = {}
  try {
    body = await response.json()
  } catch {
    // Non-JSON error page (proxy, HTML 502, …) — fall back to status text.
  }
  const message = body.message ?? defaultMessages[response.status] ?? `Lỗi ${response.status}`
  return new ApiError(response.status, body.code ?? `HTTP_${response.status}`, message, body.errors)
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const method = options.method ?? "GET"
  const url = USE_MOCK ? mockUrl(path, method) : `${API_BASE}${path}${buildQuery(options.query)}`

  if (!navigator.onLine) throw new ApiError(0, "OFFLINE", "Mất kết nối mạng. Kiểm tra Wi-Fi hoặc dữ liệu di động.")

  const timeout = AbortSignal.timeout(TIMEOUT_MS)
  const signal = options.signal ? AbortSignal.any([options.signal, timeout]) : timeout

  let response: Response
  try {
    response = await fetch(url, {
      // Static mock files can only be read with GET.
      method: USE_MOCK ? "GET" : method,
      headers: {
        Accept: "application/json",
        ...(options.body !== undefined && { "Content-Type": "application/json" }),
        ...options.headers,
      },
      body: !USE_MOCK && options.body !== undefined ? JSON.stringify(options.body) : undefined,
      signal,
    })
  } catch (error) {
    if (options.signal?.aborted) throw error
    if (timeout.aborted) throw new ApiError(0, "TIMEOUT", "Máy chủ không phản hồi sau 10 giây.")
    if (!navigator.onLine) throw new ApiError(0, "OFFLINE", "Mất kết nối mạng. Kiểm tra Wi-Fi hoặc dữ liệu di động.")
    throw new ApiError(0, "NETWORK_ERROR", "Không thể kết nối tới máy chủ.")
  }

  if (!response.ok) throw await toApiError(response)
  if (response.status === 204) return undefined as T

  try {
    return (await response.json()) as T
  } catch {
    throw new ApiError(response.status, "INVALID_JSON", "Phản hồi từ máy chủ không hợp lệ.")
  }
}

export function isAbortError(error: unknown): boolean {
  return error instanceof DOMException && error.name === "AbortError"
}

export const api = {
  getCourses: (signal?: AbortSignal) => request<Course[]>("/courses", { signal }),
  getCourse: (id: string, signal?: AbortSignal) => request<Course>(`/courses/${id}`, { signal }),
  getAssignments: (query: AssignmentQuery = {}, signal?: AbortSignal) =>
    request<Assignment[]>("/assignments", { query, signal }),
  completeAssignment: (id: string) =>
    request<CompleteAssignmentResponse>(`/assignments/${id}/complete`, { method: "POST" }),
  getTimetable: (signal?: AbortSignal) => request<ClassSession[]>("/timetable", { signal }),
}
