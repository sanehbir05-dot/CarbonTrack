import type { ReactNode } from 'react'
import { Leaf } from 'lucide-react'

interface Props {
  title: string
  message?: string
  action?: ReactNode
}

export default function EmptyState({ title, message, action }: Props) {
  return (
    <div className="empty">
      <span className="alert-ic">
        <Leaf size={18} strokeWidth={1.8} />
      </span>
      <h3>{title}</h3>
      {message && <p>{message}</p>}
      {action && <div style={{ marginTop: 8 }}>{action}</div>}
    </div>
  )
}
