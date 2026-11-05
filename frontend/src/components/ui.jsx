import React from 'react'

const P = {
  dash: 'M3 3h7v7H3zM14 3h7v7h-7zM3 14h7v7H3zM14 14h7v7h-7z',
  upload: 'M12 16V4m0 0l-4 4m4-4l4 4M4 16v4h16v-4',
  leaf: 'M5 19c0-9 5-14 15-14 0 10-5 15-14 15m0 0c1-5 4-8 8-10',
  target: 'M12 3a9 9 0 100 18 9 9 0 000-18zm0 5a4 4 0 100 8 4 4 0 000-8zm0 3a1 1 0 100 2 1 1 0 000-2z',
  award: 'M12 3a6 6 0 100 12 6 6 0 000-12zM8.5 14L7 21l5-3 5 3-1.5-7',
  file: 'M6 3h8l5 5v13H6zM14 3v5h5M9 13h6M9 17h6',
  spark: 'M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8zM19 16l.8 2.2L22 19l-2.2.8L19 22l-.8-2.2L16 19l2.2-.8z',
  bell: 'M6 16V11a6 6 0 1112 0v5l2 2H4zM10 21h4',
  history: 'M3 12a9 9 0 109-9 9 9 0 00-7 3.5M3 4v4h4M12 7v5l3 2',
  search: 'M11 4a7 7 0 100 14 7 7 0 000-14zM20 20l-4-4',
  down: 'M12 5v14m0 0l-5-5m5 5l5-5',
  up: 'M12 19V5m0 0l-5 5m5-5l5 5',
  warn: 'M12 3l10 18H2zM12 10v5M12 18h.01',
  info: 'M12 3a9 9 0 100 18 9 9 0 000-18zM12 11v6M12 7.5h.01',
  bolt: 'M13 2L4 14h7l-1 8 9-12h-7z',
  truck: 'M2 6h12v10H2zM14 9h4l3 3v4h-7zM6 19a2 2 0 100-4 2 2 0 000 4zM17 19a2 2 0 100-4 2 2 0 000 4z',
  plane: 'M2 14l20-8-8 20-3-9z',
  flame: 'M12 2c1 4 5 5 5 10a5 5 0 01-10 0c0-2 1-3 2-4 0 2 1 3 2 3 0-4-1-6 1-9z',
  chev: 'M6 9l6 6 6-6',
  plus: 'M12 5v14M5 12h14',
  send: 'M3 11l18-8-8 18-2-8z',
  check: 'M5 12l5 5L20 7',
  db: 'M4 6c0-1.7 3.6-3 8-3s8 1.3 8 3-3.6 3-8 3-8-1.3-8-3zM4 6v6c0 1.7 3.6 3 8 3s8-1.3 8-3V6M4 12v6c0 1.7 3.6 3 8 3s8-1.3 8-3v-6',
  users: 'M9 11a4 4 0 100-8 4 4 0 000 8zM2 21c0-4 3-6 7-6s7 2 7 6M17 4a4 4 0 010 7M22 21c0-3-1.5-5-4-5.7',
}
export const Icon = ({ n, s = 18, c = 'currentColor' }) => (
  <svg width={s} height={s} viewBox="0 0 24 24" fill="none" stroke={c} strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
    <path d={P[n]} />
  </svg>
)

export const Tag = ({ kind = 'info', children }) => <span className={`tag ${kind}`}>{children}</span>

export const Ring = ({ value, size = 150, stroke = 3, color = '#138a1d' }) => {
  const r = size / 2 - stroke - 2
  const C = 2 * Math.PI * r
  return (
    <svg width={size} height={size}>
      <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke="#ececea" strokeWidth={stroke} />
      <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke={color} strokeWidth={stroke + 1} strokeLinecap="round"
        strokeDasharray={`${(C * value) / 100} ${C}`} transform={`rotate(-90 ${size / 2} ${size / 2})`} />
    </svg>
  )
}

export const Bar = ({ pct, color }) => (
  <div className="bar-track"><div className="bar-fill" style={{ width: `${Math.min(100, pct)}%`, background: color }} /></div>
)

export function PageHead({ title, sub, children }) {
  return (
    <div className="page-head">
      <div><h1>{title}</h1>{sub && <p>{sub}</p>}</div>
      <div className="seg">{children}</div>
    </div>
  )
}

export function Segmented({ options, value, onChange }) {
  return (
    <div className="seg">
      {options.map(o => <button key={o} className={o === value ? 'on' : ''} onClick={() => onChange(o)}>{o}</button>)}
    </div>
  )
}

/* ---------- charts (plain SVG) ---------- */
const COL = { s1: '#0d0d0d', s2: '#138a1d', s3: '#e973a8' }

export function StackedBars({ data, target, height = 240 }) {
  const W = 760, H = height, pl = 44, pb = 26, pt = 10
  const max = Math.ceil(Math.max(...data.map(d => d.s1 + d.s2 + d.s3), target || 0) / 500) * 500
  const bw = (W - pl) / data.length
  const y = v => pt + (H - pt - pb) * (1 - v / max)
  const ticks = [0, 0.25, 0.5, 0.75, 1].map(t => Math.round(max * t))
  return (
    <svg viewBox={`0 0 ${W} ${H}`} width="100%" style={{ display: 'block' }}>
      {ticks.map(t => (
        <g key={t}>
          <line x1={pl} x2={W} y1={y(t)} y2={y(t)} stroke="#f0f0ee" />
          <text x={pl - 8} y={y(t) + 4} textAnchor="end" fontSize="11" fill="#8a8a88">{t}</text>
        </g>
      ))}
      {data.map((d, i) => {
        const x = pl + i * bw + bw * 0.22, w = bw * 0.56
        let acc = 0
        return (
          <g key={d.m}>
            {['s1', 's2', 's3'].map(k => {
              const h = (H - pt - pb) * (d[k] / max); const yy = y(0) - acc - h; acc += h
              return <rect key={k} x={x} y={yy} width={w} height={h} fill={COL[k]} rx={k === 's3' ? 5 : 0} />
            })}
            <text x={x + w / 2} y={H - 8} textAnchor="middle" fontSize="11" fill="#8a8a88">{d.m}</text>
          </g>
        )
      })}
      {target && <>
        <line x1={pl} x2={W} y1={y(target)} y2={y(target)} stroke="#f59a4d" strokeDasharray="5 4" strokeWidth="1.6" />
        <text x={W - 4} y={y(target) - 6} textAnchor="end" fontSize="11" fill="#c46a14">Monthly target pace</text>
      </>}
    </svg>
  )
}

export function ForecastChart({ actual, forecast, height = 240 }) {
  const W = 760, H = height, pl = 44, pb = 26, pt = 10
  const all = [...actual, ...forecast]
  const max = Math.ceil(Math.max(...all.map(d => d.hi ?? d.v)) / 500) * 500
  const step = (W - pl - 10) / (all.length - 1)
  const X = i => pl + i * step
  const Y = v => pt + (H - pt - pb) * (1 - v / max)
  const line = pts => pts.map((p, i) => `${i ? 'L' : 'M'}${p[0]},${p[1]}`).join(' ')
  const aPts = actual.map((d, i) => [X(i), Y(d.v)])
  const off = actual.length - 1
  const fPts = [[X(off), Y(actual[off].v)], ...forecast.map((d, i) => [X(off + 1 + i), Y(d.v)])]
  const hi = [[X(off), Y(actual[off].v)], ...forecast.map((d, i) => [X(off + 1 + i), Y(d.hi)])]
  const lo = [[X(off), Y(actual[off].v)], ...forecast.map((d, i) => [X(off + 1 + i), Y(d.lo)])].reverse()
  return (
    <svg viewBox={`0 0 ${W} ${H}`} width="100%" style={{ display: 'block' }}>
      {[0, 0.25, 0.5, 0.75, 1].map(t => (
        <g key={t}>
          <line x1={pl} x2={W} y1={Y(max * t)} y2={Y(max * t)} stroke="#f0f0ee" />
          <text x={pl - 8} y={Y(max * t) + 4} textAnchor="end" fontSize="11" fill="#8a8a88">{Math.round(max * t)}</text>
        </g>
      ))}
      <path d={line([...hi, ...lo]) + 'Z'} fill="#e973a8" opacity=".18" />
      <path d={line(aPts)} stroke="#138a1d" strokeWidth="2.5" fill="none" />
      <path d={line(fPts)} stroke="#e973a8" strokeWidth="2.5" fill="none" strokeDasharray="6 5" />
      {all.map((d, i) => i % 2 === 0 && <text key={i} x={X(i)} y={H - 8} textAnchor="middle" fontSize="11" fill="#8a8a88">{d.m}</text>)}
      {actual.filter(d => d.anomaly).map(d => {
        const i = actual.indexOf(d)
        return <g key={i}><circle cx={X(i)} cy={Y(d.v)} r="7" fill="#fff" stroke="#c0392b" strokeWidth="2" /><circle cx={X(i)} cy={Y(d.v)} r="2.5" fill="#c0392b" /></g>
      })}
    </svg>
  )
}

export function Donut({ parts, size = 170 }) {
  const total = parts.reduce((a, p) => a + p.v, 0)
  const r = 62, C = 2 * Math.PI * r
  let off = 0
  return (
    <svg width={size} height={size} viewBox="0 0 170 170">
      <g transform="rotate(-90 85 85)">
        {parts.map(p => {
          const len = (p.v / total) * C
          const el = <circle key={p.name} cx="85" cy="85" r={r} fill="none" stroke={p.color} strokeWidth="22"
            strokeDasharray={`${len - 3} ${C}`} strokeDashoffset={-off} />
          off += len
          return el
        })}
      </g>
      <text x="85" y="82" textAnchor="middle" fontSize="22" fontWeight="500">{(total / 1000).toFixed(1)}k</text>
      <text x="85" y="100" textAnchor="middle" fontSize="11" fill="#8a8a88">tCO₂e</text>
    </svg>
  )
}

export function Spark({ values, color = '#138a1d', w = 90, h = 28 }) {
  const mx = Math.max(...values), mn = Math.min(...values)
  const pts = values.map((v, i) => `${(i / (values.length - 1)) * w},${h - 3 - ((v - mn) / (mx - mn || 1)) * (h - 6)}`).join(' ')
  return <svg width={w} height={h}><polyline points={pts} fill="none" stroke={color} strokeWidth="2" strokeLinejoin="round" /></svg>
}
