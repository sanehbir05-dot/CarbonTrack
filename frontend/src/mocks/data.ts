// Mock data for VITE_USE_MOCKS=true. Shapes follow section A8 of the shared contract and the
// demo company follows A9. Everything lives in memory and resets on page reload.
// Factor values are approximate placeholders. The real ones come from the backend migration.

import type {
  Activity,
  ActivityRequest,
  Anomaly,
  Category,
  CategoryEmission,
  DashboardSummary,
  Facility,
  FacilityEmission,
  Factor,
  ForecastResponse,
  IncentiveRule,
  IncentiveStatus,
  MonthlyEmission,
  Role,
  Scope,
  Target,
  TargetRequest,
  TargetStatus,
  User,
} from '../api/types'
import { CATEGORY_META } from '../utils/format'

export const DEMO_PASSWORD = 'Demo@1234'
export const REPORTING_YEAR = new Date().getFullYear() - 1

export interface MockUser extends User {
  password: string
}

export const users: MockUser[] = [
  { id: 1, name: 'Demo Admin', email: 'demo@carbontrack.in', password: DEMO_PASSWORD, role: 'ADMIN', companyId: 1, companyName: 'GreenSteel Industries Ltd', baselineYear: 2023 },
  { id: 2, name: 'Demo Analyst', email: 'analyst@carbontrack.in', password: DEMO_PASSWORD, role: 'ANALYST', companyId: 1, companyName: 'GreenSteel Industries Ltd', baselineYear: 2023 },
]

export function toUser(u: MockUser): User {
  const { password: _password, ...user } = u
  return user
}

export function addUser(input: { companyName: string; name: string; email: string; password: string }): MockUser {
  const user: MockUser = {
    id: users.length + 1,
    name: input.name,
    email: input.email,
    password: input.password,
    role: 'ADMIN' as Role,
    companyId: 1, // mock mode has a single company, so new sign-ups see the demo data
    companyName: input.companyName,
    baselineYear: 2023,
  }
  users.push(user)
  return user
}

export const facilities: Facility[] = [
  { id: 1, name: 'Pune Plant', city: 'Pune', state: 'Maharashtra', type: 'PLANT' },
  { id: 2, name: 'Ludhiana Plant', city: 'Ludhiana', state: 'Punjab', type: 'PLANT' },
  { id: 3, name: 'Gurugram HQ', city: 'Gurugram', state: 'Haryana', type: 'OFFICE' },
  { id: 4, name: 'Chennai Warehouse', city: 'Chennai', state: 'Tamil Nadu', type: 'WAREHOUSE' },
]

const CEA = 'CEA CO2 Baseline Database'
const UK = 'UK Government GHG Conversion Factors'
const factorSeed: [Category, number, string, string][] = [
  ['ELECTRICITY', 0.716, CEA, 'IN'],
  ['DIESEL', 2.51, UK, 'GLOBAL'],
  ['PETROL', 2.08, UK, 'GLOBAL'],
  ['NATURAL_GAS', 2.04, UK, 'GLOBAL'],
  ['LPG', 2.94, UK, 'GLOBAL'],
  ['BUSINESS_TRAVEL_AIR', 0.18, UK, 'GLOBAL'],
  ['BUSINESS_TRAVEL_ROAD', 0.17, UK, 'GLOBAL'],
  ['FREIGHT_ROAD', 0.11, UK, 'GLOBAL'],
  ['WASTE_LANDFILL', 497, UK, 'GLOBAL'],
]

export const factors: Factor[] = factorSeed.map(([category, value, source, region], i) => ({
  id: i + 1,
  category,
  scope: CATEGORY_META[category].scope,
  unit: CATEGORY_META[category].unit,
  valueKgCo2ePerUnit: value,
  source,
  region,
  year: 2023,
}))

export const incentiveRules: IncentiveRule[] = [
  { id: 1, tier: 'BRONZE', name: 'Bronze', minReductionPct: 5, creditPoints: 100, benefit: 'Recognition certificate and listing as a participating company (simulated).' },
  { id: 2, tier: 'SILVER', name: 'Silver', minReductionPct: 10, creditPoints: 250, benefit: 'Priority review for green financing and 250 credit points (simulated).' },
  { id: 3, tier: 'GOLD', name: 'Gold', minReductionPct: 20, creditPoints: 500, benefit: 'Top tier green rating and 500 credit points (simulated).' },
]

// ---------- activity records ----------

function mulberry32(seed: number) {
  let a = seed
  return () => {
    a |= 0
    a = (a + 0x6d2b79f5) | 0
    let t = Math.imul(a ^ (a >>> 15), 1 | a)
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

// Base monthly quantity in 2023 for each sensible facility and category pair.
const baseQuantities: Record<number, Partial<Record<Category, number>>> = {
  1: { ELECTRICITY: 900000, DIESEL: 40000, NATURAL_GAS: 120000, FREIGHT_ROAD: 800000, WASTE_LANDFILL: 60 },
  2: { ELECTRICITY: 700000, DIESEL: 55000, LPG: 20000, FREIGHT_ROAD: 600000, WASTE_LANDFILL: 45 },
  3: { ELECTRICITY: 120000, PETROL: 3000, BUSINESS_TRAVEL_AIR: 250000, BUSINESS_TRAVEL_ROAD: 40000 },
  4: { ELECTRICITY: 150000, DIESEL: 12000, FREIGHT_ROAD: 900000, WASTE_LANDFILL: 20 },
}
const yearTrend: Record<number, number> = { 2023: 1, 2024: 0.93, 2025: 0.85, 2026: 0.83 }
const summerPeak = [0.9, 0.9, 0.97, 1.08, 1.18, 1.2, 1.1, 1.05, 1.02, 0.95, 0.88, 0.87]
// Three injected spikes in 2025 for anomaly detection: [facilityId, category, month, multiplier]
const spikes: [number, Category, number, number][] = [
  [2, 'DIESEL', 5, 1.9],
  [1, 'ELECTRICITY', 8, 1.5],
  [4, 'FREIGHT_ROAD', 3, 1.8],
]

function factorFor(category: Category): Factor {
  return factors.find(f => f.category === category)!
}

function buildActivity(id: number, req: ActivityRequest, source: Activity['source'], createdAt: string): Activity {
  const factor = factorFor(req.category)
  const facility = facilities.find(f => f.id === req.facilityId)
  return {
    id,
    facilityId: req.facilityId,
    facilityName: facility?.name ?? 'Unknown facility',
    category: req.category,
    scope: factor.scope,
    quantity: req.quantity,
    unit: factor.unit,
    year: req.year,
    month: req.month,
    co2eKg: Math.round(req.quantity * factor.valueKgCo2ePerUnit * 1000) / 1000,
    factorId: factor.id,
    factorValue: factor.valueKgCo2ePerUnit,
    factorSource: factor.source,
    source,
    createdAt,
  }
}

function seedActivities(): Activity[] {
  const rand = mulberry32(20231101)
  const out: Activity[] = []
  let id = 1
  for (let year = 2023; year <= 2026; year++) {
    const lastMonth = year === 2026 ? 10 : 12
    for (let month = 1; month <= lastMonth; month++) {
      for (const facility of facilities) {
        const pairs = baseQuantities[facility.id] ?? {}
        for (const category of Object.keys(pairs) as Category[]) {
          let quantity = pairs[category]! * yearTrend[year] * (0.96 + rand() * 0.08)
          if (category === 'ELECTRICITY') quantity *= summerPeak[month - 1]
          const spike = spikes.find(s => year === 2025 && s[0] === facility.id && s[1] === category && s[2] === month)
          if (spike) quantity *= spike[3]
          const createdAt = `${year}-${String(month).padStart(2, '0')}-28T10:00:00Z`
          out.push(buildActivity(id++, { facilityId: facility.id, category, quantity: Math.round(quantity), year, month }, 'SEED', createdAt))
        }
      }
    }
  }
  return out
}

export let activities: Activity[] = seedActivities()
let nextActivityId = activities.length + 1

export function addActivity(req: ActivityRequest): Activity {
  const activity = buildActivity(nextActivityId++, req, 'MANUAL', new Date().toISOString())
  activities.push(activity)
  return activity
}

export function replaceActivity(id: number, req: ActivityRequest): Activity | null {
  const index = activities.findIndex(a => a.id === id)
  if (index < 0) return null
  activities[index] = buildActivity(id, req, activities[index].source, activities[index].createdAt)
  return activities[index]
}

export function removeActivity(id: number): boolean {
  const before = activities.length
  activities = activities.filter(a => a.id !== id)
  return activities.length < before
}

// ---------- dashboard ----------

const sum = (rows: Activity[]) => rows.reduce((total, a) => total + a.co2eKg, 0)
const round = (n: number, decimals = 1) => Math.round(n * 10 ** decimals) / 10 ** decimals
const inYear = (year: number) => activities.filter(a => a.year === year)

export function yearTotal(year: number): number {
  return sum(inYear(year))
}

export function dashboardYears(): number[] {
  return [...new Set(activities.map(a => a.year))].sort((a, b) => a - b)
}

export function dashboardByCategory(year: number): CategoryEmission[] {
  const rows = inYear(year)
  const total = sum(rows)
  const byCategory = new Map<Category, number>()
  rows.forEach(a => byCategory.set(a.category, (byCategory.get(a.category) ?? 0) + a.co2eKg))
  return [...byCategory.entries()]
    .map(([category, co2eKg]) => ({
      category,
      scope: CATEGORY_META[category].scope,
      co2eKg: round(co2eKg, 3),
      pct: total ? round((co2eKg / total) * 100) : 0,
    }))
    .sort((a, b) => b.co2eKg - a.co2eKg)
}

export function dashboardByFacility(year: number): FacilityEmission[] {
  const rows = inYear(year)
  const total = sum(rows)
  const byFacility = new Map<number, number>()
  rows.forEach(a => byFacility.set(a.facilityId, (byFacility.get(a.facilityId) ?? 0) + a.co2eKg))
  return [...byFacility.entries()]
    .map(([facilityId, co2eKg]) => ({
      facilityId,
      facilityName: facilities.find(f => f.id === facilityId)?.name ?? 'Unknown facility',
      co2eKg: round(co2eKg, 3),
      pct: total ? round((co2eKg / total) * 100) : 0,
    }))
    .sort((a, b) => b.co2eKg - a.co2eKg)
}

export function dashboardSummary(year: number): DashboardSummary {
  const rows = inYear(year)
  const scopeTotal = (scope: Scope) => round(sum(rows.filter(a => a.scope === scope)), 3)
  const total = sum(rows)
  const previous = yearTotal(year - 1)
  return {
    year,
    totalCo2eKg: round(total, 3),
    scope1Kg: scopeTotal('SCOPE_1'),
    scope2Kg: scopeTotal('SCOPE_2'),
    scope3Kg: scopeTotal('SCOPE_3'),
    changeVsPrevYearPct: previous ? round(((total - previous) / previous) * 100) : null,
    topCategory: dashboardByCategory(year)[0]?.category ?? null,
    topFacility: dashboardByFacility(year)[0]?.facilityName ?? null,
  }
}

export function dashboardMonthly(year: number, facilityId?: number): MonthlyEmission[] {
  const rows = inYear(year).filter(a => !facilityId || a.facilityId === facilityId)
  return Array.from({ length: 12 }, (_, i) => {
    const month = i + 1
    const monthRows = rows.filter(a => a.month === month)
    const scopeTotal = (scope: Scope) => round(sum(monthRows.filter(a => a.scope === scope)), 3)
    return {
      year,
      month,
      scope1Kg: scopeTotal('SCOPE_1'),
      scope2Kg: scopeTotal('SCOPE_2'),
      scope3Kg: scopeTotal('SCOPE_3'),
      totalKg: round(sum(monthRows), 3),
    }
  })
}

// ---------- targets ----------

let targetRows: (TargetRequest & { id: number })[] = [
  { id: 1, name: '20% by 2027', baselineYear: 2023, targetYear: 2027, reductionPct: 20 },
]
let nextTargetId = 2

function toTarget(row: TargetRequest & { id: number }): Target {
  const baselineKg = yearTotal(row.baselineYear)
  const targetKg = baselineKg * (1 - row.reductionPct / 100)
  const currentYear = REPORTING_YEAR
  const currentKg = yearTotal(currentYear)
  const span = baselineKg - targetKg
  const achievedPct = span > 0 ? Math.min(100, Math.max(0, ((baselineKg - currentKg) / span) * 100)) : 0
  const actualReductionPct = baselineKg ? ((baselineKg - currentKg) / baselineKg) * 100 : 0
  const years = row.targetYear - row.baselineYear
  const expectedPct = years > 0 ? row.reductionPct * Math.min(1, (currentYear - row.baselineYear) / years) : row.reductionPct
  let status: TargetStatus = 'AT_RISK'
  if (currentKg <= targetKg) status = 'ACHIEVED'
  else if (actualReductionPct >= expectedPct) status = 'ON_TRACK'
  return { ...row, baselineKg: round(baselineKg, 3), targetKg: round(targetKg, 3), currentYear, currentKg: round(currentKg, 3), achievedPct: round(achievedPct), status }
}

export const listTargets = (): Target[] => targetRows.map(toTarget)

export function addTarget(req: TargetRequest): Target {
  const row = { id: nextTargetId++, ...req }
  targetRows.push(row)
  return toTarget(row)
}

export function removeTarget(id: number): boolean {
  const before = targetRows.length
  targetRows = targetRows.filter(t => t.id !== id)
  return targetRows.length < before
}

// ---------- incentives ----------

export function incentiveStatus(year: number, baselineYear: number | null): IncentiveStatus {
  const baselineKg = baselineYear === null ? 0 : yearTotal(baselineYear)
  const currentKg = yearTotal(year)
  const reductionPct = baselineKg ? ((baselineKg - currentKg) / baselineKg) * 100 : 0
  const ascending = [...incentiveRules].sort((a, b) => a.minReductionPct - b.minReductionPct)
  const earned = ascending.filter(r => reductionPct >= r.minReductionPct).pop()
  const next = ascending.find(r => reductionPct < r.minReductionPct)
  return {
    year,
    baselineYear,
    baselineKg: round(baselineKg, 3),
    currentKg: round(currentKg, 3),
    reductionPct: round(reductionPct, 2),
    tier: earned?.tier ?? 'NONE',
    creditPoints: earned?.creditPoints ?? 0,
    benefit: earned?.benefit ?? 'No tier reached yet.',
    nextTier: next ? { tier: next.tier, minReductionPct: next.minReductionPct, gapPct: round(next.minReductionPct - reductionPct, 2) } : null,
  }
}

// ---------- AI ----------

export function forecast(months: number): ForecastResponse {
  const totals = new Map<string, { year: number; month: number; totalKg: number }>()
  activities.forEach(a => {
    const key = `${a.year}-${String(a.month).padStart(2, '0')}`
    const row = totals.get(key) ?? { year: a.year, month: a.month, totalKg: 0 }
    row.totalKg += a.co2eKg
    totals.set(key, row)
  })
  const history = [...totals.entries()].sort(([a], [b]) => a.localeCompare(b)).map(([, row]) => ({ ...row, totalKg: round(row.totalKg, 3) }))
  const n = history.length
  if (n < 12) return { method: 'LINEAR_TREND_SEASONAL', history, forecast: [] }

  // Linear trend on the monthly totals.
  const meanX = (n - 1) / 2
  const meanY = history.reduce((s, h) => s + h.totalKg, 0) / n
  let sxy = 0
  let sxx = 0
  history.forEach((h, i) => {
    sxy += (i - meanX) * (h.totalKg - meanY)
    sxx += (i - meanX) ** 2
  })
  const slope = sxy / sxx
  const intercept = meanY - slope * meanX
  const trend = (i: number) => intercept + slope * i

  // Seasonal index for each calendar month.
  const ratios: number[][] = Array.from({ length: 12 }, () => [])
  history.forEach((h, i) => ratios[h.month - 1].push(h.totalKg / trend(i)))
  const seasonal = ratios.map(r => (r.length ? r.reduce((s, v) => s + v, 0) / r.length : 1))

  const residuals = history.map((h, i) => h.totalKg - trend(i) * seasonal[h.month - 1])
  const sd = Math.sqrt(residuals.reduce((s, r) => s + r * r, 0) / n)

  const last = history[n - 1]
  const out = Array.from({ length: months }, (_, k) => {
    const monthIndex = last.month - 1 + k + 1
    const year = last.year + Math.floor(monthIndex / 12)
    const month = (monthIndex % 12) + 1
    const totalKg = trend(n + k) * seasonal[month - 1]
    return { year, month, totalKg: round(totalKg, 3), lowerKg: round(Math.max(0, totalKg - 1.96 * sd), 3), upperKg: round(totalKg + 1.96 * sd, 3) }
  })
  return { method: 'LINEAR_TREND_SEASONAL', history, forecast: out }
}

export function anomalies(year?: number): Anomaly[] {
  const groups = new Map<string, Activity[]>()
  activities.forEach(a => {
    const key = `${a.facilityId}:${a.category}`
    groups.set(key, [...(groups.get(key) ?? []), a])
  })
  const out: Anomaly[] = []
  groups.forEach(rows => {
    if (rows.length < 6) return
    const mean = rows.reduce((s, a) => s + a.quantity, 0) / rows.length
    const sd = Math.sqrt(rows.reduce((s, a) => s + (a.quantity - mean) ** 2, 0) / rows.length)
    if (!sd) return
    rows.forEach(a => {
      const z = (a.quantity - mean) / sd
      if (Math.abs(z) < 2.5) return
      if (year && a.year !== year) return
      out.push({
        activityId: a.id,
        facilityId: a.facilityId,
        facilityName: a.facilityName,
        category: a.category,
        year: a.year,
        month: a.month,
        quantity: a.quantity,
        unit: a.unit,
        expectedQuantity: round(mean, 0),
        zScore: round(z, 2),
        severity: Math.abs(z) >= 3.5 ? 'HIGH' : 'MEDIUM',
      })
    })
  })
  return out.sort((a, b) => Math.abs(b.zScore) - Math.abs(a.zScore))
}

export function assistantAnswer(question: string, baselineYear: number | null): string {
  const year = REPORTING_YEAR
  const summary = dashboardSummary(year)
  const status = incentiveStatus(year, baselineYear)
  const tonnes = (kg: number) => `${round(kg / 1000).toLocaleString('en-US')} t CO2e`
  const q = question.toLowerCase()
  if (/anomal|spike|unusual/.test(q)) {
    const found = anomalies(year)
    if (!found.length) return `No anomalies were flagged in ${year}.`
    return `${found.length} records were flagged in ${year}. The largest is ${CATEGORY_META[found[0].category].label} at ${found[0].facilityName} in month ${found[0].month}, with a z-score of ${found[0].zScore}.`
  }
  if (/target|track|tier|incentive/.test(q)) {
    return `Emissions in ${year} are ${status.reductionPct}% below the ${baselineYear} baseline. The current incentive tier is ${status.tier}.${status.nextTier ? ` Another ${status.nextTier.gapPct}% is needed for ${status.nextTier.tier}.` : ''}`
  }
  return `In ${year} total emissions were ${tonnes(summary.totalCo2eKg)}: Scope 1 ${tonnes(summary.scope1Kg)}, Scope 2 ${tonnes(summary.scope2Kg)}, Scope 3 ${tonnes(summary.scope3Kg)}. The largest category is ${summary.topCategory ? CATEGORY_META[summary.topCategory].label : 'not available'} and the largest facility is ${summary.topFacility ?? 'not available'}.`
}
