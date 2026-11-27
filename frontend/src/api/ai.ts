import { client } from './client'
import type { Anomaly, AssistantResponse, ForecastResponse } from './types'

export const aiApi = {
  forecast: (months = 6) => client.get<ForecastResponse>('/ai/forecast', { params: { months } }).then(r => r.data),
  anomalies: (year?: number) => client.get<Anomaly[]>('/ai/anomalies', { params: { year } }).then(r => r.data),
  ask: (question: string) => client.post<AssistantResponse>('/ai/assistant', { question }).then(r => r.data),
}
