import type { ReactNode } from 'react'
import Modal from './Modal'

interface Props {
  title: string
  message: ReactNode
  confirmLabel?: string
  busy?: boolean
  onConfirm: () => void
  onCancel: () => void
}

/** Confirmation step for destructive actions. */
export default function ConfirmDialog({ title, message, confirmLabel = 'Delete', busy = false, onConfirm, onCancel }: Props) {
  return (
    <Modal title={title} onClose={onCancel} busy={busy}>
      <p style={{ lineHeight: 1.5 }}>{message}</p>
      <div className="modal-actions">
        <button type="button" className="pill-btn soft" disabled={busy} onClick={onCancel}>
          Cancel
        </button>
        <button type="button" className="pill-btn danger solid" disabled={busy} onClick={onConfirm}>
          {busy ? 'Deleting…' : confirmLabel}
        </button>
      </div>
    </Modal>
  )
}
