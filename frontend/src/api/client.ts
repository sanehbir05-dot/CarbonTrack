import axios, { AxiosError } from 'axios'
import type { ApiError } from './types'
import { mockAdapter } from '../mocks/adapter'

const TOKEN_KEY = 'carbontrack.token'

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

export const USE_MOCKS = import.meta.env.VITE_USE_MOCKS === 'true'

export const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  headers: { 'Content-Type': 'application/json' },
})

if (USE_MOCKS) {
  client.defaults.adapter = mockAdapter
}

client.interceptors.request.use(config => {
  const token = tokenStore.get()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

client.interceptors.response.use(
  response => response,
  (error: AxiosError) => {
    const url = error.config?.url ?? ''
    const isAuthCall = url.startsWith('/auth/login') || url.startsWith('/auth/register')
    if (error.response?.status === 401 && !isAuthCall) {
      tokenStore.clear()
      if (window.location.pathname !== '/login') window.location.assign('/login')
    }
    return Promise.reject(error)
  },
)

/** The `message` field of the contract's error body, with a fallback. */
export function errorMessage(error: unknown, fallback = 'Something went wrong. Please try again.'): string {
  if (axios.isAxiosError(error)) {
    const body = error.response?.data as Partial<ApiError> | undefined
    if (body?.message) return body.message
    if (!error.response) return 'Cannot reach the server. Check your connection.'
  }
  return fallback
}
