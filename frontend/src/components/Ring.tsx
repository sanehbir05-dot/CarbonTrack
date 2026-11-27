import type { ReactNode } from 'react'

interface Props {
  /** 0 to 100 */
  value: number
  size?: number
  stroke?: number
  color?: string
  children?: ReactNode
}

/** Circular progress ring, as in the approved prototype. */
export default function Ring({ value, size = 130, stroke = 3, color = '#138a1d', children }: Props) {
  const r = size / 2 - stroke - 2
  const circumference = 2 * Math.PI * r
  const pct = Math.min(100, Math.max(0, value))
  return (
    <div className="ring-wrap" style={{ width: size, height: size }}>
      <svg width={size} height={size} aria-hidden="true">
        <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke="#ececea" strokeWidth={stroke} />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={r}
          fill="none"
          stroke={color}
          strokeWidth={stroke + 1}
          strokeLinecap="round"
          strokeDasharray={`${(circumference * pct) / 100} ${circumference}`}
          transform={`rotate(-90 ${size / 2} ${size / 2})`}
        />
      </svg>
      <div className="num">{children}</div>
    </div>
  )
}
