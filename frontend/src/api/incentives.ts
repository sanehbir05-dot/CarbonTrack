import { client } from './client'
import type { IncentiveRule, IncentiveStatus } from './types'

export const incentivesApi = {
  rules: () => client.get<IncentiveRule[]>('/incentives/rules').then(r => r.data),
  status: (year?: number) => client.get<IncentiveStatus>('/incentives/status', { params: { year } }).then(r => r.data),
}
