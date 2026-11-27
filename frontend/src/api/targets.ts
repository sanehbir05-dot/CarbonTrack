import { client } from './client'
import type { Target, TargetRequest } from './types'

export const targetsApi = {
  list: () => client.get<Target[]>('/targets').then(r => r.data),
  create: (body: TargetRequest) => client.post<Target>('/targets', body).then(r => r.data),
  remove: (id: number) => client.delete<void>(`/targets/${id}`).then(() => undefined),
}
