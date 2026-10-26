import { useCallback, useEffect, useRef, useState, type DependencyList } from "react"
import { ApiError, isAbortError } from "./api"

export type UseApiResult<T> = {
  data: T | undefined
  loading: boolean
  error: ApiError | undefined
  refetch: () => void
}

/**
 * Runs `fetcher` on mount and whenever `deps` change; the previous request is aborted.
 *   const { data, loading, error, refetch } = useApi((signal) => api.getCourses(signal), [])
 */
export function useApi<T>(
  fetcher: (signal: AbortSignal) => Promise<T>,
  deps: DependencyList,
  options: { enabled?: boolean } = {},
): UseApiResult<T> {
  const enabled = options.enabled ?? true
  const [data, setData] = useState<T>()
  const [error, setError] = useState<ApiError>()
  const [loading, setLoading] = useState(enabled)
  const [reloadKey, setReloadKey] = useState(0)

  // Latest fetcher without forcing callers to memoize it.
  const fetcherRef = useRef(fetcher)
  fetcherRef.current = fetcher

  useEffect(() => {
    if (!enabled) {
      setLoading(false)
      return
    }
    const controller = new AbortController()
    setLoading(true)
    setError(undefined)

    fetcherRef.current(controller.signal)
      .then((result) => {
        if (!controller.signal.aborted) setData(result)
      })
      .catch((caught: unknown) => {
        if (controller.signal.aborted || isAbortError(caught)) return
        setError(caught instanceof ApiError ? caught : new ApiError(0, "UNKNOWN", String(caught)))
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false)
      })

    return () => controller.abort()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, enabled, reloadKey])

  const refetch = useCallback(() => setReloadKey((key) => key + 1), [])

  // A request that failed for lack of network is retried as soon as the connection is back.
  useEffect(() => {
    if (!error || error.status !== 0) return
    window.addEventListener("online", refetch)
    return () => window.removeEventListener("online", refetch)
  }, [error, refetch])

  return { data, loading, error, refetch }
}
