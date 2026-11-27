import { useCallback, useEffect, useState, type FormEvent } from 'react'
import toast from 'react-hot-toast'
import { Building2, Factory, Plus, Server, Warehouse, type LucideIcon } from 'lucide-react'
import { facilitiesApi } from '../api/facilities'
import { errorMessage } from '../api/client'
import type { Facility, FacilityRequest, FacilityType } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import ConfirmDialog from '../components/ConfirmDialog'
import EmptyState from '../components/EmptyState'
import Modal from '../components/Modal'
import PageHead from '../components/PageHead'
import Spinner from '../components/Spinner'
import Tag from '../components/Tag'
import { FACILITY_TYPES, facilityTypeLabel } from '../utils/format'
import { usePageTitle } from '../utils/usePageTitle'

const TYPE_ICONS: Record<FacilityType, LucideIcon> = {
  OFFICE: Building2,
  PLANT: Factory,
  WAREHOUSE: Warehouse,
  DATA_CENTER: Server,
}

interface FormProps {
  facility: Facility | null
  onClose: () => void
  onSaved: () => void
}

function FacilityForm({ facility, onClose, onSaved }: FormProps) {
  const [form, setForm] = useState<FacilityRequest>({
    name: facility?.name ?? '',
    city: facility?.city ?? '',
    state: facility?.state ?? '',
    type: facility?.type ?? 'PLANT',
  })
  const [busy, setBusy] = useState(false)
  const set = (field: keyof FacilityRequest) => (e: { target: { value: string } }) => setForm(f => ({ ...f, [field]: e.target.value }))

  async function submit(e: FormEvent) {
    e.preventDefault()
    const body: FacilityRequest = { ...form, name: form.name.trim(), city: form.city.trim(), state: form.state.trim() }
    if (!body.name || !body.city || !body.state) {
      toast.error('Name, city and state are required')
      return
    }
    setBusy(true)
    try {
      if (facility) await facilitiesApi.update(facility.id, body)
      else await facilitiesApi.create(body)
      toast.success(facility ? 'Facility updated' : 'Facility added')
      onSaved()
    } catch (err) {
      toast.error(errorMessage(err, 'Could not save the facility'))
      setBusy(false)
    }
  }

  return (
    <Modal title={facility ? 'Edit facility' : 'Add facility'} onClose={onClose} busy={busy}>
      <form onSubmit={submit} className="form-stack">
        <div className="field">
          <label htmlFor="fac-name">Name</label>
          <input id="fac-name" required maxLength={120} value={form.name} onChange={set('name')} placeholder="e.g. Pune Plant" />
        </div>
        <div className="form-2">
          <div className="field">
            <label htmlFor="fac-city">City</label>
            <input id="fac-city" required maxLength={80} value={form.city} onChange={set('city')} />
          </div>
          <div className="field">
            <label htmlFor="fac-state">State</label>
            <input id="fac-state" required maxLength={80} value={form.state} onChange={set('state')} />
          </div>
        </div>
        <div className="field">
          <label htmlFor="fac-type">Type</label>
          <select id="fac-type" value={form.type} onChange={set('type')}>
            {FACILITY_TYPES.map(t => (
              <option key={t.value} value={t.value}>
                {t.label}
              </option>
            ))}
          </select>
        </div>
        <div className="modal-actions">
          <button type="button" className="pill-btn soft" disabled={busy} onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="pill-btn green" disabled={busy}>
            {busy ? 'Saving…' : facility ? 'Save changes' : 'Add facility'}
          </button>
        </div>
      </form>
    </Modal>
  )
}

export default function Facilities() {
  usePageTitle('Facilities')
  const { isAdmin } = useAuth()
  const [facilities, setFacilities] = useState<Facility[] | null>(null)
  const [loading, setLoading] = useState(true)
  const [editing, setEditing] = useState<Facility | 'new' | null>(null)
  const [deleting, setDeleting] = useState<Facility | null>(null)
  const [deleteBusy, setDeleteBusy] = useState(false)

  const load = useCallback(() => {
    setLoading(true)
    facilitiesApi
      .list()
      .then(setFacilities)
      .catch(err => toast.error(errorMessage(err, 'Could not load facilities')))
      .finally(() => setLoading(false))
  }, [])

  useEffect(load, [load])

  async function confirmDelete() {
    if (!deleting) return
    setDeleteBusy(true)
    try {
      await facilitiesApi.remove(deleting.id)
      toast.success(`${deleting.name} deleted`)
      setDeleting(null)
      load()
    } catch (err) {
      toast.error(errorMessage(err, 'Could not delete the facility'))
    } finally {
      setDeleteBusy(false)
    }
  }

  const addButton = isAdmin && (
    <button className="pill-btn dark" onClick={() => setEditing('new')}>
      <Plus size={16} strokeWidth={1.8} /> Add facility
    </button>
  )

  return (
    <>
      <PageHead title="Facilities" sub={isAdmin ? 'Offices, plants, warehouses and data centres' : 'Offices, plants, warehouses and data centres · view only'}>
        {addButton}
      </PageHead>

      {loading && !facilities ? (
        <div className="panel empty">
          <Spinner label="Loading facilities" />
        </div>
      ) : !facilities || facilities.length === 0 ? (
        <div className="panel">
          <EmptyState
            title="No facilities yet"
            message={isAdmin ? 'Add your offices, plants and warehouses to start logging emissions.' : 'An admin needs to add the first facility.'}
            action={addButton || undefined}
          />
        </div>
      ) : (
        <div className="grid-x g-3" style={{ opacity: loading ? 0.6 : 1 }}>
          {facilities.map(f => {
            const Icon = TYPE_ICONS[f.type] ?? Building2
            return (
              <div className="panel" key={f.id}>
                <div className="panel-head">
                  <span className="cell-ic">
                    <i>
                      <Icon size={16} strokeWidth={1.8} />
                    </i>
                  </span>
                  <Tag kind="info">{facilityTypeLabel(f.type)}</Tag>
                </div>
                <h3>{f.name}</h3>
                <p className="small-note" style={{ marginTop: 4 }}>
                  {f.city}, {f.state}
                </p>
                {isAdmin && (
                  <div className="row-actions" style={{ marginTop: 16 }}>
                    <button className="pill-btn soft sm" onClick={() => setEditing(f)}>
                      Edit
                    </button>
                    <button className="pill-btn danger sm" onClick={() => setDeleting(f)}>
                      Delete
                    </button>
                  </div>
                )}
              </div>
            )
          })}
        </div>
      )}

      {editing && (
        <FacilityForm
          facility={editing === 'new' ? null : editing}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null)
            load()
          }}
        />
      )}
      {deleting && (
        <ConfirmDialog
          title="Delete facility"
          message={
            <>
              Delete <b>{deleting.name}</b>? This cannot be undone.
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
