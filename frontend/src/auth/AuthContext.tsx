import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { authApi } from '../api/auth'
import { tokenStore } from '../api/client'
import type { LoginRequest, RegisterRequest, User } from '../api/types'

interface AuthState {
  user: User | null
  loading: boolean
  isAdmin: boolean
  login: (body: LoginRequest) => Promise<void>
  register: (body: RegisterRequest) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(() => Boolean(tokenStore.get()))

  // Restore the session from a stored token.
  useEffect(() => {
    if (!tokenStore.get()) return
    authApi
      .me()
      .then(setUser)
      .catch(() => tokenStore.clear())
      .finally(() => setLoading(false))
  }, [])

  const login = useCallback(async (body: LoginRequest) => {
    const res = await authApi.login(body)
    tokenStore.set(res.token)
    setUser(res.user)
  }, [])

  const register = useCallback(async (body: RegisterRequest) => {
    const res = await authApi.register(body)
    tokenStore.set(res.token)
    setUser(res.user)
  }, [])

  const logout = useCallback(() => {
    tokenStore.clear()
    setUser(null)
  }, [])

  const value = useMemo<AuthState>(
    () => ({ user, loading, isAdmin: user?.role === 'ADMIN', login, register, logout }),
    [user, loading, login, register, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
