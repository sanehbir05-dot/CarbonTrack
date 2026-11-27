import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import toast from 'react-hot-toast'
import { Plus } from 'lucide-react'
import { activitiesApi } from '../api/activities'
import { dashboardApi } from '../api/dashboard'
import { errorMessage } from '../api/client'
import { facilitiesApi } from '../api/facilities'
import { factorsApi } from '../api/factors'
import type { Activity, Category, Facility, Factor, Page, Scope } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import CategoryIcon from '../components/CategoryIcon'
import ConfirmDialog from '../components/ConfirmDialog'
import EmptyState from '../components/EmptyState'
import Modal from '../components/Modal'
import PageHead from '../components/PageHead'
import Spinner from '../components/Spinner'
import Tag, { type TagKind } from '../components/Tag'
import { pickFactor } from '../utils/factors'
import { CATEGORIES, CATEGORY_META, MONTH_OPTIONS, categoryLabel, formatCo2e, formatNumber, formatPeriod, scopeLabel, unitLabel } from '../utils/format'
import { usePageTitle } from '../utils/usePageTitle'

const PAGE_SIZE = 20
const SCOPE_TAG: Record<Scope, TagKind> = { SCOPE_1: 'info', SCOPE_2: 'good', SCOPE_3: 'pink' }

interface Filters {
  facilityId: string
  category: string
  year: string
}
const NO_FILTERS: Filters = { facilityId: '', category: '', year: '' }

interface FormProps {
  facilities: Facility[]
  factors: Factor[]
  isAdmin: boolean
  onClose: () => void
  onSaved: () => void
}

function RecordForm({ facilities, factors, isAdmin, onClose, onSaved }: FormProps) {
  const today = new Date()
  const [facilityId, setFacilityId] = useState(facilities[0] ? String(facilities[0].id) : '')
  const [category, setCategory] = useState<Category>('ELECTRICITY')
  const [quantity, setQuantity] = useState('')
  const [year, setYear] = useState(String(today.getFullYear()))
  const [month, setMonth] = useState(String(today.getMonth() + 1))
  const [busy, setBusy] = useState(false)

  const meta = CATEGORY_META[category]
  const factor = pickFactor(factors, category, Number(year))
  const qty = Number(quantity)
  const estimate = factor && qty > 0 ? qty * factor.valueKgCo2ePerUnit : null

  async function submit(e: FormEvent) {
    e.preventDefault()
    const yearNumber = Number(year)
    if (!facilityId) return void toast.error('Choose a facility')
    if (!(qty > 0)) return void toast.error('Quantity must be greater than zero')
    if (!Number.isInteger(yearNumber) || yearNumber < 2000 || yearNumber > 2100) return void toast.error('Enter a year between 2000 and 2100')
    setBusy(true)
    try {
      const saved = await activitiesApi.create({ facilityId: Number(facilityId), category, quantity: qty, year: yearNumber, month: Number(month) })
      toast.success(`Record saved: ${formatCo2e(saved.co2eKg)}`)
      onSaved()
    } catch (err) {
      toast.error(errorMessage(err, 'Could not save the record'))
      setBusy(false)
    }
  }

  if (facilities.length === 0) {
    return (
      <Modal title="Add activity record" onClose={onClose}>
        <p style={{ lineHeight: 1.5 }}>You need at least one facility before you can add records.</p>
        <div className="modal-actions">
          <button type="button" className="pill-btn soft" onClick={onClose}>
            Close
          </button>
          {isAdmin && (
            <Link to="/facilities" className="pill-btn dark">
              Add a facility
            </Link>
          )}
        </div>
      </Modal>
    )
  }

  return (
    <Modal title="Add activity record" onClose={onClose} busy={busy}>
      <form onSubmit={submit} className="form-stack">
        <div className="field">
          <label htmlFor="rec-facility">Facility</label>
          <select id="rec-facility" value={facilityId} onChange={e => setFacilityId(e.target.value)}>
            {facilities.map(f => (
              <option key={f.id} value={f.id}>
                {f.name}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="rec-category">Category</label>
          <select id="rec-category" value={category} onChange={e => setCategory(e.target.value as Category)}>
            {CATEGORIES.map(c => (
              <option key={c} value={c}>
                {categoryLabel(c)}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="rec-qty">Quantity</label>
          <div className="input-unit">
            <input id="rec-qty" type="number" inputMode="decimal" min="0" step="any" required placeholder="e.g. 48000" value={quantity} onChange={e => setQuantity(e.target.value)} />
            <span className="unit">{unitLabel(meta.unit)}</span>
          </div>
        </div>
        <div className="form-2">
          <div className="field">
            <label htmlFor="rec-month">Month</label>
            <select id="rec-month" value={month} onChange={e => setMonth(e.target.value)}>
              {MONTH_OPTIONS.map(m => (
                <option key={m.value} value={m.value}>
                  {m.label}
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label htmlFor="rec-year">Year</label>
            <input id="rec-year" type="number" min="2000" max="2100" step="1" required value={year} onChange={e => setYear(e.target.value)} />
          </div>
        </div>

        <div className="preview" style={{ marginTop: 0 }}>
          <div>
            <div className="small-note">Estimated emissions (quantity × factor)</div>
            <b>{estimate !== null ? formatCo2e(estimate) : '—'}</b>
            {factor && (
              <div className="small-note" style={{ marginTop: 2 }}>
                {formatNumber(factor.valueKgCo2ePerUnit, 4)} kgCO₂e / {unitLabel(factor.unit)} · {factor.source}
              </div>
            )}
          </div>
          <Tag kind="info">{scopeLabel(meta.scope)}</Tag>
        </div>
        <p className="small-note">The final value is calculated by the server when you save.</p>

        <div className="modal-actions" style={{ marginTop: 4 }}>
          <button type="button" className="pill-btn soft" disabled={busy} onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="pill-btn green" disabled={busy}>
            {busy ? 'Saving…' : 'Save record'}
          </button>
        </div>
      </form>
    </Modal>
  )
}

export default function Activities() {
  usePageTitle('Activity data')
  const { isAdmin } = useAuth()
  const [facilities, setFacilities] = useState<Facility[]>([])
  const [years, setYears] = useState<number[]>([])
  const [factors, setFactors] = useState<Factor[]>([])
  const [filters, setFilters] = useState<Filters>(NO_FILTERS)
  const [page, setPage] = useState(0)
  const [data, setData] = useState<Page<Activity> | null>(null)
  const [loading, setLoading] = useState(true)
  const [reload, setReload] = useState(0)
  const [adding, setAdding] = useState(false)
  const [deleting, setDeleting] = useState<Activity | null>(null)
  const [deleteBusy, setDeleteBusy] = useState(false)
  const requestId = useRef(0)

  // Reference lists for the filters and the add form.
  useEffect(() => {
    Promise.all([facilitiesApi.list(), dashboardApi.years(), factorsApi.list()])
      .then(([f, y, fa]) => {
        setFacilities(f)
        setYears([...y].sort((a, b) => b - a))
        setFactors(fa)
      })
      .catch(err => toast.error(errorMessage(err, 'Could not load the filter options')))
  }, [])

  useEffect(() => {
    const id = ++requestId.current
    setLoading(true)
    activitiesApi
      .list({
        facilityId: filters.facilityId ? Number(filters.facilityId) : undefined,
        category: (filters.category || undefined) as Category | undefined,
        year: filters.year ? Number(filters.year) : undefined,
        page,
        size: PAGE_SIZE,
      })
      .then(res => {
        if (id === requestId.current) setData(res)
      })
      .catch(err => {
        if (id === requestId.current) toast.error(errorMessage(err, 'Could not load activity records'))
      })
      .finally(() => {
        if (id === requestId.current) setLoading(false)
      })
  }, [filters, page, reload])

  // After deleting the last row of the last page, step back one page.
  useEffect(() => {
    if (data && data.content.length === 0 && page > 0) setPage(p => p - 1)
  }, [data, page])

  const setFilter = (key: keyof Filters) => (e: { target: { value: string } }) => {
    setFilters(f => ({ ...f, [key]: e.target.value }))
    setPage(0)
  }
  const filtersActive = Boolean(filters.facilityId || filters.category || filters.year)
  const clearFilters = () => {
    setFilters(NO_FILTERS)
    setPage(0)
  }

  async function confirmDelete() {
    if (!deleting) return
    setDeleteBusy(true)
    try {
      await activitiesApi.remove(deleting.id)
      toast.success('Record deleted')
      setDeleting(null)
      setReload(n => n + 1)
    } catch (err) {
      toast.error(errorMessage(err, 'Could not delete the record'))
    } finally {
      setDeleteBusy(false)
    }
  }

  const addButton = (
    <button className="pill-btn dark" onClick={() => setAdding(true)}>
      <Plus size={16} strokeWidth={1.8} /> Add record
    </button>
  )

  const rows = data?.content ?? []
  const first = data && data.totalElements > 0 ? data.page * data.size + 1 : 0
  const last = data ? data.page * data.size + rows.length : 0

  return (
    <>
      <PageHead title="Activity data" sub="Monthly records per facility · CO₂e is calculated when you save">
        {addButton}
      </PageHead>

      <div className="panel">
        <div className="filters">
          <div className="field">
            <label htmlFor="flt-facility">Facility</label>
            <select id="flt-facility" value={filters.facilityId} onChange={setFilter('facilityId')}>
              <option value="">All facilities</option>
              {facilities.map(f => (
                <option key={f.id} value={f.id}>
                  {f.name}
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label htmlFor="flt-category">Category</label>
            <select id="flt-category" value={filters.category} onChange={setFilter('category')}>
              <option value="">All categories</option>
              {CATEGORIES.map(c => (
                <option key={c} value={c}>
                  {categoryLabel(c)}
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label htmlFor="flt-year">Year</label>
            <select id="flt-year" value={filters.year} onChange={setFilter('year')}>
              <option value="">All years</option>
              {years.map(y => (
                <option key={y} value={y}>
                  {y}
                </option>
              ))}
            </select>
          </div>
          {filtersActive && (
            <button className="pill-btn soft" onClick={clearFilters}>
              Clear filters
            </button>
          )}
        </div>

        {loading && !data ? (
          <div className="empty">
            <Spinner label="Loading records" />
          </div>
        ) : rows.length === 0 && !loading ? (
          filtersActive ? (
            <EmptyState
              title="No records match these filters"
              message="Try a different facility, category or year."
              action={
                <button className="pill-btn dark" onClick={clearFilters}>
                  Clear filters
                </button>
              }
            />
          ) : (
            <EmptyState title="No activity records yet" message="Add your first monthly record and CarbonTrack will calculate the emissions." action={addButton} />
          )
        ) : (
          <div style={{ opacity: loading ? 0.6 : 1, transition: 'opacity .15s' }}>
            <div className="table-wrap">
              <table className="tbl">
                <thead>
                  <tr>
                    <th>Period</th>
                    <th>Category / facility</th>
                    <th className="r">Quantity</th>
                    <th>Scope</th>
                    <th className="r">Emissions</th>
                    <th>Factor used</th>
                    <th>Source</th>
                    {isAdmin && <th />}
                  </tr>
                </thead>
                <tbody>
                  {rows.map(a => (
                    <tr className="hov" key={a.id}>
                      <td style={{ whiteSpace: 'nowrap' }}>{formatPeriod(a.year, a.month)}</td>
                      <td>
                        <span className="cell-ic">
                          <i>
                            <CategoryIcon category={a.category} />
                          </i>
                          <span>
                            <b style={{ fontWeight: 500 }}>{categoryLabel(a.category)}</b>
                            <div className="small-note">{a.facilityName}</div>
                          </span>
                        </span>
                      </td>
                      <td className="r" style={{ whiteSpace: 'nowrap' }}>
                        {formatNumber(a.quantity, 3)} <span className="small-note">{unitLabel(a.unit)}</span>
                      </td>
                      <td>
                        <Tag kind={SCOPE_TAG[a.scope]}>{scopeLabel(a.scope)}</Tag>
                      </td>
                      <td className="r" style={{ whiteSpace: 'nowrap' }}>
                        <b style={{ fontWeight: 500 }}>{formatCo2e(a.co2eKg)}</b>
                      </td>
                      <td>
                        <div style={{ whiteSpace: 'nowrap' }}>
                          {formatNumber(a.factorValue, 4)} <span className="small-note">kgCO₂e / {unitLabel(a.unit)}</span>
                        </div>
                        <div className="small-note">{a.factorSource}</div>
                      </td>
                      <td>
                        <Tag kind="info">{a.source}</Tag>
                      </td>
                      {isAdmin && (
                        <td className="r">
                          <button className="pill-btn danger sm" onClick={() => setDeleting(a)}>
                            Delete
                          </button>
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {data && (
              <div className="pager">
                <span className="small-note">
                  Showing {first}–{last} of {formatNumber(data.totalElements)}
                </span>
                <div className="seg">
                  <button className="pill-btn soft sm" disabled={page === 0 || loading} onClick={() => setPage(p => p - 1)}>
                    Previous
                  </button>
                  <span className="small-note">
                    Page {data.page + 1} of {Math.max(1, data.totalPages)}
                  </span>
                  <button className="pill-btn soft sm" disabled={page + 1 >= data.totalPages || loading} onClick={() => setPage(p => p + 1)}>
                    Next
                  </button>
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      {adding && (
        <RecordForm
          facilities={facilities}
          factors={factors}
          isAdmin={isAdmin}
          onClose={() => setAdding(false)}
          onSaved={() => {
            setAdding(false)
            setPage(0)
            setReload(n => n + 1)
          }}
        />
      )}
      {deleting && (
        <ConfirmDialog
          title="Delete record"
          message={
            <>
              Delete the <b>{categoryLabel(deleting.category)}</b> record for <b>{deleting.facilityName}</b>, {formatPeriod(deleting.year, deleting.month)}? This cannot be undone.
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
