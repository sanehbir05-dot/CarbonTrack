interface Props {
  years: number[]
  value: number | null
  onChange: (year: number) => void
}

/** Rule 4: default to the last full calendar year if it has data, else the latest year. */
export function defaultYear(years: number[]): number | null {
  if (!years.length) return null
  const lastFullYear = new Date().getFullYear() - 1
  return years.includes(lastFullYear) ? lastFullYear : Math.max(...years)
}

/** Year selector drawn as segmented pill buttons. */
export default function YearSelect({ years, value, onChange }: Props) {
  return (
    <div className="seg" role="group" aria-label="Year">
      {years.map(y => (
        <button key={y} type="button" className={y === value ? 'on' : ''} aria-pressed={y === value} onClick={() => onChange(y)}>
          {y}
        </button>
      ))}
    </div>
  )
}
