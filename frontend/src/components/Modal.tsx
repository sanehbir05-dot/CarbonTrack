import { useEffect, useId, useRef, type ReactNode } from 'react'
import { X } from 'lucide-react'

interface Props {
  title: string
  onClose: () => void
  children: ReactNode
  /** While true the dialog cannot be dismissed (a request is in flight). */
  busy?: boolean
}

export default function Modal({ title, onClose, children, busy = false }: Props) {
  const titleId = useId()
  const dialogRef = useRef<HTMLDivElement>(null)
  const closeRef = useRef(onClose)
  const busyRef = useRef(busy)
  closeRef.current = onClose
  busyRef.current = busy

  // Focus the first field once, when the dialog opens.
  useEffect(() => {
    const dialog = dialogRef.current
    const first = dialog?.querySelector<HTMLElement>('input, select, textarea') ?? dialog?.querySelector<HTMLElement>('.modal-actions button')
    first?.focus()
  }, [])

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !busyRef.current) closeRef.current()
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [])

  return (
    <div
      className="modal-backdrop"
      onMouseDown={e => {
        if (e.target === e.currentTarget && !busy) onClose()
      }}
    >
      <div ref={dialogRef} className="modal" role="dialog" aria-modal="true" aria-labelledby={titleId}>
        <div className="modal-head">
          <h2 id={titleId}>{title}</h2>
          <button type="button" className="icon-btn sm" aria-label="Close" disabled={busy} onClick={onClose}>
            <X size={16} strokeWidth={1.8} />
          </button>
        </div>
        {children}
      </div>
    </div>
  )
}
