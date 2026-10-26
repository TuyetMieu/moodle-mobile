/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** "true" → api.ts reads static JSON from public/mock instead of calling the backend. */
  readonly VITE_USE_MOCK?: string
  /** Defaults to "/api" (proxied to localhost:5000 in dev). */
  readonly VITE_API_BASE_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
