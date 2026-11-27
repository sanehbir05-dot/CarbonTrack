export default function Spinner({ label = 'Loading' }: { label?: string }) {
  return (
    <div role="status" aria-label={label} className="spinner">
      <i />
      <span>{label}…</span>
    </div>
  )
}
