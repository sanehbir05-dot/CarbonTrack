import type { ReactNode } from 'react'

interface Props {
  label: string
  value: string
  unit?: string
  icon: ReactNode
  color: 'green' | 'pink' | 'orange' | 'dark'
  delta?: string
  sub?: string
}

export default function KpiCard({ label, value, unit, icon, color, delta, sub }: Props) {
  return (
    <div className={`kpi ${color}`}>
      <div className="k-top">
        <span>{label}</span>
        <div className="k-icon">{icon}</div>
      </div>
      <div>
        <div className="k-val">
          {value}
          {unit && <small>{unit}</small>}
        </div>
        {delta && <div className="k-delta">{delta}</div>}
        {sub && <div className="k-sub">{sub}</div>}
      </div>
    </div>
  )
}
