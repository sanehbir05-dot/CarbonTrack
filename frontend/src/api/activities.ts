import { client } from './client'
import type { Activity, ActivityFilters, ActivityRequest, Page } from './types'

export const activitiesApi = {
  list: (filters: ActivityFilters = {}) =>
    client.get<Page<Activity>>('/activities', { params: filters }).then(r => r.data),
  create: (body: ActivityRequest) => client.post<Activity>('/activities', body).then(r => r.data),
  update: (id: number, body: ActivityRequest) => client.put<Activity>(`/activities/${id}`, body).then(r => r.data),
  remove: (id: number) => client.delete<void>(`/activities/${id}`).then(() => undefined),
}
