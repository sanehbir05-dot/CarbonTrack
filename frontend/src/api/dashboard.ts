import { client } from './client'
import type { CategoryEmission, DashboardSummary, FacilityEmission, MonthlyEmission } from './types'

export const dashboardApi = {
  years: () => client.get<number[]>('/dashboard/years').then(r => r.data),
  summary: (year: number) => client.get<DashboardSummary>('/dashboard/summary', { params: { year } }).then(r => r.data),
  monthly: (year: number, facilityId?: number) =>
    client.get<MonthlyEmission[]>('/dashboard/monthly', { params: { year, facilityId } }).then(r => r.data),
  byCategory: (year: number) =>
    client.get<CategoryEmission[]>('/dashboard/by-category', { params: { year } }).then(r => r.data),
  byFacility: (year: number) =>
    client.get<FacilityEmission[]>('/dashboard/by-facility', { params: { year } }).then(r => r.data),
}
