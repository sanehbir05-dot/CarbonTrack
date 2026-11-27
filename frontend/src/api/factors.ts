import { client } from './client'
import type { Factor } from './types'

export const factorsApi = {
  list: (year?: number) => client.get<Factor[]>('/factors', { params: { year } }).then(r => r.data),
}
