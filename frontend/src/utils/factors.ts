import type { Category, Factor } from '../api/types'

/**
 * Factor selection, rule A7.2 of the shared contract: for a category and activity year take the
 * greatest factor year at or before it, prefer region IN, else GLOBAL. If no factor is at or before
 * that year, use the earliest available. Used only for the estimate shown before saving.
 */
export function pickFactor(factors: Factor[], category: Category, year: number): Factor | null {
  const rows = factors.filter(f => f.category === category)
  if (!rows.length) return null
  const atOrBefore = rows.filter(f => f.year <= year)
  const chosenYear = atOrBefore.length ? Math.max(...atOrBefore.map(f => f.year)) : Math.min(...rows.map(f => f.year))
  const sameYear = rows.filter(f => f.year === chosenYear)
  return sameYear.find(f => f.region === 'IN') ?? sameYear.find(f => f.region === 'GLOBAL') ?? sameYear[0]
}
