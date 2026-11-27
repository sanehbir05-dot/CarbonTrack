/** Horizontal progress bar. `pct` is clamped to 0 to 100. */
export default function Bar({ pct, color }: { pct: number; color?: string }) {
  const width = Math.min(100, Math.max(0, Number.isFinite(pct) ? pct : 0))
  return (
    <div className="bar-track" role="progressbar" aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round(width)}>
      <div className="bar-fill" style={{ width: `${width}%`, background: color }} />
    </div>
  )
}
