import type { Category, FacilityType, Scope } from '../api/types'

export const FACILITY_TYPES: { value: FacilityType; label: string }[] = [
  { value: 'OFFICE', label: 'Office' },
  { value: 'PLANT', label: 'Plant' },
  { value: 'WAREHOUSE', label: 'Warehouse' },
  { value: 'DATA_CENTER', label: 'Data centre' },
]

export function facilityTypeLabel(type: FacilityType | string): string {
  return FACILITY_TYPES.find(t => t.value === type)?.label ?? type
}

export const CATEGORY_META: Record<Category, { label: string; unit: string; scope: Scope }> = {
  ELECTRICITY: { label: 'Electricity', unit: 'kWh', scope: 'SCOPE_2' },
  DIESEL: { label: 'Diesel', unit: 'litre', scope: 'SCOPE_1' },
  PETROL: { label: 'Petrol', unit: 'litre', scope: 'SCOPE_1' },
  NATURAL_GAS: { label: 'Natural gas', unit: 'm3', scope: 'SCOPE_1' },
  LPG: { label: 'LPG', unit: 'kg', scope: 'SCOPE_1' },
  BUSINESS_TRAVEL_AIR: { label: 'Business travel (air)', unit: 'passenger_km', scope: 'SCOPE_3' },
  BUSINESS_TRAVEL_ROAD: { label: 'Business travel (road)', unit: 'km', scope: 'SCOPE_3' },
  FREIGHT_ROAD: { label: 'Freight (road)', unit: 'tonne_km', scope: 'SCOPE_3' },
  WASTE_LANDFILL: { label: 'Waste to landfill', unit: 'tonne', scope: 'SCOPE_3' },
}

export const CATEGORIES = Object.keys(CATEGORY_META) as Category[]

// Scope colours stay the same in every chart.
export const SCOPE_COLORS: Record<Scope, string> = {
  SCOPE_1: '#0d0d0d',
  SCOPE_2: '#138a1d',
  SCOPE_3: '#e973a8',
}

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

export function formatNumber(value: number, maxDecimals = 0): string {
  return value.toLocaleString('en-US', { maximumFractionDigits: maxDecimals })
}

/** The API returns kg. Show tonnes from 1000 kg upwards. */
export function formatCo2e(kg: number): string {
  if (Math.abs(kg) >= 1000) {
    const t = (kg / 1000).toLocaleString('en-US', { minimumFractionDigits: 1, maximumFractionDigits: 1 })
    return `${t} tCO₂e`
  }
  return `${formatNumber(kg, 1)} kgCO₂e`
}

/** Tonnes as a bare number, for chart axes and tooltips. */
export function toTonnes(kg: number): number {
  return Math.round((kg / 1000) * 10) / 10
}

export function monthName(month: number): string {
  return MONTHS[month - 1] ?? String(month)
}

export function categoryLabel(category: Category | string): string {
  return CATEGORY_META[category as Category]?.label ?? category
}

export function scopeLabel(scope: Scope): string {
  return scope.replace('SCOPE_', 'Scope ')
}

/** `passenger_km` becomes "passenger km" and `m3` becomes "m³". */
export function unitLabel(unit: string): string {
  return unit === 'm3' ? 'm³' : unit.replace(/_/g, ' ')
}

/** "Oct 2026" */
export function formatPeriod(year: number, month: number): string {
  return `${monthName(month)} ${year}`
}

export const MONTH_OPTIONS = MONTHS.map((label, i) => ({ value: i + 1, label }))

export function formatPct(value: number, decimals = 1): string {
  return `${value.toFixed(decimals)}%`
}
