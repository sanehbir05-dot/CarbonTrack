// Mirrors section A8 of the shared contract. Do not rename fields here.

export type Role = 'ADMIN' | 'ANALYST'
export type Scope = 'SCOPE_1' | 'SCOPE_2' | 'SCOPE_3'
export type FacilityType = 'OFFICE' | 'PLANT' | 'WAREHOUSE' | 'DATA_CENTER'
export type RecordSource = 'MANUAL' | 'CSV' | 'SEED'
export type TargetStatus = 'ACHIEVED' | 'ON_TRACK' | 'AT_RISK'
export type Tier = 'NONE' | 'BRONZE' | 'SILVER' | 'GOLD'
export type Category =
  | 'ELECTRICITY'
  | 'DIESEL'
  | 'PETROL'
  | 'NATURAL_GAS'
  | 'LPG'
  | 'BUSINESS_TRAVEL_AIR'
  | 'BUSINESS_TRAVEL_ROAD'
  | 'FREIGHT_ROAD'
  | 'WASTE_LANDFILL'

export interface ApiError {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

// Auth
export interface User {
  id: number
  name: string
  email: string
  role: Role
  companyId: number
  companyName: string
  /** The real backend returns null for a newly registered company (it has no baseline yet). */
  baselineYear: number | null
}
export interface AuthResponse {
  token: string
  user: User
}
export interface LoginRequest {
  email: string
  password: string
}
export interface RegisterRequest {
  companyName: string
  industry: string
  name: string
  email: string
  password: string
}

// Facilities
export interface Facility {
  id: number
  name: string
  city: string
  state: string
  type: FacilityType
}
export type FacilityRequest = Omit<Facility, 'id'>

// Emission factors
export interface Factor {
  id: number
  category: Category
  scope: Scope
  unit: string
  valueKgCo2ePerUnit: number
  source: string
  region: string
  year: number
}

// Activity records
export interface Activity {
  id: number
  facilityId: number
  facilityName: string
  category: Category
  scope: Scope
  quantity: number
  unit: string
  year: number
  month: number
  co2eKg: number
  factorId: number
  factorValue: number
  factorSource: string
  source: RecordSource
  createdAt: string
}
export interface ActivityRequest {
  facilityId: number
  category: Category
  quantity: number
  year: number
  month: number
}
export interface ActivityFilters {
  facilityId?: number
  category?: Category
  year?: number
  page?: number
  size?: number
}
export interface UploadResult {
  imported: number
  failed: number
  errors: { row: number; message: string }[]
}

// Dashboard
export interface DashboardSummary {
  year: number
  totalCo2eKg: number
  scope1Kg: number
  scope2Kg: number
  scope3Kg: number
  changeVsPrevYearPct: number | null
  topCategory: Category | null
  topFacility: string | null
}
export interface MonthlyEmission {
  year: number
  month: number
  scope1Kg: number
  scope2Kg: number
  scope3Kg: number
  totalKg: number
}
export interface CategoryEmission {
  category: Category
  scope: Scope
  co2eKg: number
  pct: number
}
export interface FacilityEmission {
  facilityId: number
  facilityName: string
  co2eKg: number
  pct: number
}

// Targets
export interface Target {
  id: number
  name: string
  baselineYear: number
  targetYear: number
  reductionPct: number
  baselineKg: number
  targetKg: number
  currentYear: number
  currentKg: number
  achievedPct: number
  status: TargetStatus
}
export interface TargetRequest {
  name: string
  baselineYear: number
  targetYear: number
  reductionPct: number
}

// Incentives
export interface IncentiveRule {
  id: number
  tier: Tier
  name: string
  minReductionPct: number
  creditPoints: number
  benefit: string
}
export interface IncentiveStatus {
  year: number
  /** Null when the company has no data yet. */
  baselineYear: number | null
  baselineKg: number
  currentKg: number
  reductionPct: number
  tier: Tier
  creditPoints: number
  benefit: string
  nextTier: { tier: Tier; minReductionPct: number; gapPct: number } | null
}

// AI
export interface ForecastResponse {
  method: string
  history: { year: number; month: number; totalKg: number }[]
  forecast: { year: number; month: number; totalKg: number; lowerKg: number; upperKg: number }[]
}
export interface Anomaly {
  activityId: number
  facilityId: number
  facilityName: string
  category: Category
  year: number
  month: number
  quantity: number
  unit: string
  expectedQuantity: number
  zScore: number
  severity: 'MEDIUM' | 'HIGH'
}
export interface AssistantResponse {
  answer: string
  source: 'LLM' | 'RULES'
}
