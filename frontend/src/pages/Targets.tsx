import { useCallback, useEffect, useState, type FormEvent } from 'react'
import toast from 'react-hot-toast'
import { Plus } from 'lucide-react'
import { targetsApi } from '../api/targets'
import { errorMessage } from '../api/client'
import type { Target, TargetRequest, TargetStatus } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import Bar from '../components/Bar'
import ConfirmDialog from '../components/ConfirmDialog'
import EmptyState from '../components/EmptyState'
import Modal from '../components/Modal'
import PageHead from '../components/PageHead'
import Ring from '../components/Ring'
import Spinner from '../components/Spinner'
import Tag, { type TagKind } from '../components/Tag'
import { formatCo2e } from '../utils/format'
import { usePageTitle } from '../utils/usePageTitle'

const STATUS: Record<TargetStatus, { kind: TagKind; label: string; ring: string }> = {
  ACHIEVED: { kind: 'good', label: 'Achieved', ring: '#138a1d' },
  ON_TRACK: { kind: 'info', label: 'On track', ring: '#138a1d' },
  AT_RISK: { kind: 'bad', label: 'At risk', ring: '#c0392b' },
}

interface FormProps {
  baselineYear: number
  onClose: () => void
  onSaved: () => void
}

function TargetForm({ baselineYear, onClose, onSaved }: FormProps) {
  const [name, setName] = useState('')
  const [baseline, setBaseline] = useState(String(baselineYear))
  const [targetYear, setTargetYear] = useState(String(Math.max(baselineYear + 1, new Date().getFullYear() + 1)))
  const [pct, setPct] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    const body: TargetRequest = { name: name.trim(), baselineYear: Number(baseline), targetYear: Number(targetYear), reductionPct: Number(pct) }
    if (!body.name) return void toast.error('Give the target a name')
    if (!Number.isInteger(body.baselineYear) || !Number.isInteger(body.targetYear)) return void toast.error('Years must be whole numbers')
    if (body.targetYear <= body.baselineYear) return void toast.error('The target year must be after the baseline year')
    if (!(body.reductionPct > 0 && body.reductionPct <= 100)) return void toast.error('Reduction must be more than 0 and at most 100')
    setBusy(true)
    try {
      await targetsApi.create(body)
      toast.success('Target added')
      onSaved()
    } catch (err) {
      toast.error(errorMessage(err, 'Could not save the target'))
      setBusy(false)
    }
  }

  return (
    <Modal title="Add target" onClose={onClose} busy={busy}>
      <form onSubmit={submit} className="form-stack">
        <div className="field">
          <label htmlFor="tg-name">Name</label>
          <input id="tg-name" required maxLength={120} placeholder="e.g. 20% by 2027" value={name} onChange={e => setName(e.target.value)} />
        </div>
        <div className="form-2">
          <div className="field">
            <label htmlFor="tg-base">Baseline year</label>
            <input id="tg-base" type="number" min="2000" max="2100" step="1" required value={baseline} onChange={e => setBaseline(e.target.value)} />
          </div>
          <div className="field">
            <label htmlFor="tg-year">Target year</label>
            <input id="tg-year" type="number" min="2000" max="2100" step="1" required value={targetYear} onChange={e => setTargetYear(e.target.value)} />
          </div>
        </div>
        <div className="field">
          <label htmlFor="tg-pct">Reduction against the baseline</label>
          <div className="input-unit">
            <input id="tg-pct" type="number" min="0" max="100" step="0.1" required placeholder="e.g. 20" value={pct} onChange={e => setPct(e.target.value)} />
            <span className="unit">%</span>
          </div>
        </div>
        <div className="modal-actions">
          <button type="button" className="pill-btn soft" disabled={busy} onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="pill-btn green" disabled={busy}>
            {busy ? 'Saving…' : 'Add target'}
          </button>
        </div>
      </form>
    </Modal>
  )
}

function TargetCard({ target: t, isAdmin, onDelete }: { target: Target; isAdmin: boolean; onDelete: () => void }) {
  const status = STATUS[t.status]
  const rows: [string, number, string][] = [
    [`${t.baselineYear} baseline`, t.baselineKg, '#0d0d0d'],
    [`${t.currentYear} (current)`, t.currentKg, '#138a1d'],
    [`${t.targetYear} target`, t.targetKg, '#f59a4d'],
  ]
  return (
    <div className="panel">
      <div className="panel-head" style={{ alignItems: 'flex-start' }}>
        <div>
          <h3>{t.name}</h3>
          <div className="small-note" style={{ marginTop: 2 }}>
            {t.reductionPct}% below {t.baselineYear} by {t.targetYear}
          </div>
        </div>
        <Tag kind={status.kind}>{status.label}</Tag>
      </div>

      <div style={{ display: 'flex', gap: 22, alignItems: 'center', flexWrap: 'wrap' }}>
        <Ring value={t.achievedPct} color={status.ring}>
          <span>
            {Math.round(t.achievedPct)}
            <small>%</small>
          </span>
        </Ring>
        <div style={{ flex: '1 1 220px', minWidth: 0 }}>
          {rows.map(([label, kg, color]) => (
            <div key={label} style={{ marginBottom: 12 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8, marginBottom: 6 }}>
                <span>{label}</span>
                <b style={{ fontWeight: 500, whiteSpace: 'nowrap' }}>{formatCo2e(kg)}</b>
              </div>
              <Bar pct={t.baselineKg > 0 ? (kg / t.baselineKg) * 100 : 0} color={color} />
            </div>
          ))}
        </div>
      </div>

      <p className="small-note" style={{ marginTop: 6 }}>
        {t.baselineKg > 0 ? 'The ring shows how much of the planned reduction is achieved so far.' : `There is no emissions data for ${t.baselineYear}, so progress cannot be measured yet.`}
      </p>
      {isAdmin && (
        <div className="row-actions" style={{ marginTop: 14 }}>
          <button className="pill-btn danger sm" onClick={onDelete}>
            Delete
          </button>
        </div>
      )}
    </div>
  )
}

export default function Targets() {
  usePageTitle('Targets')
  const { user, isAdmin } = useAuth()
  const [targets, setTargets] = useState<Target[] | null>(null)
  const [loading, setLoading] = useState(true)
  const [adding, setAdding] = useState(false)
  const [deleting, setDeleting] = useState<Target | null>(null)
  const [deleteBusy, setDeleteBusy] = useState(false)

  const load = useCallback(() => {
    setLoading(true)
    targetsApi
      .list()
      .then(setTargets)
      .catch(err => toast.error(errorMessage(err, 'Could not load targets')))
      .finally(() => setLoading(false))
  }, [])

  useEffect(load, [load])

  async function confirmDelete() {
    if (!deleting) return
    setDeleteBusy(true)
    try {
      await targetsApi.remove(deleting.id)
      toast.success('Target deleted')
      setDeleting(null)
      load()
    } catch (err) {
      toast.error(errorMessage(err, 'Could not delete the target'))
    } finally {
      setDeleteBusy(false)
    }
  }

  const addButton = isAdmin && (
    <button className="pill-btn dark" onClick={() => setAdding(true)}>
      <Plus size={16} strokeWidth={1.8} /> Add target
    </button>
  )

  return (
    <>
      <PageHead title="Targets" sub={isAdmin ? 'Reduction targets against a baseline year' : 'Reduction targets against a baseline year · view only'}>
        {addButton}
      </PageHead>

      {loading && !targets ? (
        <div className="panel empty">
          <Spinner label="Loading targets" />
        </div>
      ) : !targets || targets.length === 0 ? (
        <div className="panel">
          <EmptyState
            title="No targets yet"
            message={isAdmin ? 'Set a reduction target to see your progress and status.' : 'An admin needs to set the first target.'}
            action={addButton || undefined}
          />
        </div>
      ) : (
        <div className="grid-x g-2" style={{ opacity: loading ? 0.6 : 1 }}>
          {targets.map(t => (
            <TargetCard key={t.id} target={t} isAdmin={isAdmin} onDelete={() => setDeleting(t)} />
          ))}
        </div>
      )}

      {adding && (
        <TargetForm
          baselineYear={user?.baselineYear ?? new Date().getFullYear() - 1}
          onClose={() => setAdding(false)}
          onSaved={() => {
            setAdding(false)
            load()
          }}
        />
      )}
      {deleting && (
        <ConfirmDialog
          title="Delete target"
          message={
            <>
              Delete the target <b>{deleting.name}</b>? This cannot be undone.
            </>
          }
          busy={deleteBusy}
          onConfirm={confirmDelete}
          onCancel={() => setDeleting(null)}
        />
      )}
    </>
  )
}
