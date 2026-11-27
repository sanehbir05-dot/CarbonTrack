import type { ReactNode } from 'react'
import { Leaf } from 'lucide-react'

interface Props {
  title: string
  subtitle: string
  children: ReactNode
  footer: ReactNode
}

/** Shared frame for the Login and Register pages. */
export default function AuthShell({ title, subtitle, children, footer }: Props) {
  return (
    <div className="auth-stage">
      <div className="auth-card">
        <div className="logo">
          <span className="logo-mark">
            <Leaf size={18} color="#8fd48f" strokeWidth={1.8} />
          </span>
          CarbonTrack
        </div>
        <div className="panel">
          <h1>{title}</h1>
          <p className="small-note" style={{ fontSize: 14, marginTop: 4 }}>
            {subtitle}
          </p>
          {children}
        </div>
        <p className="auth-foot">{footer}</p>
      </div>
    </div>
  )
}
