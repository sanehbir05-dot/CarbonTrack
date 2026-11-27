import type { ReactNode } from 'react'

export type TagKind = 'good' | 'warn' | 'bad' | 'info' | 'pink'

export default function Tag({ kind = 'info', children }: { kind?: TagKind; children: ReactNode }) {
  return <span className={`tag ${kind}`}>{children}</span>
}
