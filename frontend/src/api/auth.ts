import { client } from './client'
import type { AuthResponse, LoginRequest, RegisterRequest, User } from './types'

export const authApi = {
  login: (body: LoginRequest) => client.post<AuthResponse>('/auth/login', body).then(r => r.data),
  register: (body: RegisterRequest) => client.post<AuthResponse>('/auth/register', body).then(r => r.data),
  me: () => client.get<User>('/auth/me').then(r => r.data),
}
