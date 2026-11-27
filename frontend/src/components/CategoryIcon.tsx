import { Car, Flame, Plane, Trash2, Truck, Zap, type LucideIcon } from 'lucide-react'
import type { Category } from '../api/types'

const ICONS: Record<Category, LucideIcon> = {
  ELECTRICITY: Zap,
  DIESEL: Flame,
  PETROL: Flame,
  NATURAL_GAS: Flame,
  LPG: Flame,
  BUSINESS_TRAVEL_AIR: Plane,
  BUSINESS_TRAVEL_ROAD: Car,
  FREIGHT_ROAD: Truck,
  WASTE_LANDFILL: Trash2,
}

export default function CategoryIcon({ category, size = 16 }: { category: Category; size?: number }) {
  const Icon = ICONS[category] ?? Zap
  return <Icon size={size} strokeWidth={1.8} />
}
