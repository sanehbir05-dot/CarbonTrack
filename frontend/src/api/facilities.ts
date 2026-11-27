import { client } from './client'
import type { Facility, FacilityRequest } from './types'

export const facilitiesApi = {
  list: () => client.get<Facility[]>('/facilities').then(r => r.data),
  create: (body: FacilityRequest) => client.post<Facility>('/facilities', body).then(r => r.data),
  update: (id: number, body: FacilityRequest) => client.put<Facility>(`/facilities/${id}`, body).then(r => r.data),
  remove: (id: number) => client.delete<void>(`/facilities/${id}`).then(() => undefined),
}
