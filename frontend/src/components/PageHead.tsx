import type { ReactNode } from 'react'

interface Props {
  title: string
  sub?: string
  children?: ReactNode
}

export default function PageHead({ title, sub, children }: Props) {
  return (
    <div className="page-head">
      <div>
        <h1>{title}</h1>
        {sub && <p>{sub}</p>}
      </div>
      {children && <div className="seg">{children}</div>}
    </div>
  )
}
