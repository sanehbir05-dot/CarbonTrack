import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import toast from 'react-hot-toast'
import { Bar, BarChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { ArrowDown, ArrowUp, Building2, CalendarDays, Flame, Leaf, Truck, Zap } from 'lucide-react'
import { dashboardApi } from '../api/dashboard'
import { errorMessage } from '../api/client'
import type { CategoryEmission, DashboardSummary, FacilityEmission, MonthlyEmission } from '../api/types'
import EmptyState from '../components/EmptyState'
import KpiCard from '../components/KpiCard'
import PageHead from '../components/PageHead'
import Spinner from '../components/Spinner'
import YearSelect, { defaultYear } from '../components/YearSelect'
import { SCOPE_COLORS, categoryLabel, formatCo2e, formatNumber, monthName, toTonnes } from '../utils/format'
import { usePageTitle } from '../utils/usePageTitle'

interface DashboardData {
  summary: DashboardSummary
  monthly: MonthlyEmission[]
  byCategory: CategoryEmission[]
  byFacility: FacilityEmission[]
}

const CATEGORY_COLORS = ['#138a1d', '#0d0d0d', '#e973a8', '#f59a4d', '#70c27a', '#8a8a88', '#efa0c6', '#f7b47c', '#d99100']
const AXIS = { fontSize: 11, fill: '#8a8a88' }
const TOOLTIP_STYLE = { borderRadius: 14, border: '1px solid #ececea', boxShadow: '0 8px 24px rgba(0,0,0,.08)', fontSize: 13 }

const tonnesTooltip = (value: number | string) => `${formatNumber(Number(value), 1)} tCO₂e`

/** Big number for a KPI card: tonnes from 1000 kg upwards, else kg. */
function kpiValue(kg: number): { value: string; unit: string } {
  if (Math.abs(kg) < 1000) return { value: formatNumber(kg, 1), unit: 'kgCO₂e' }
  const tonnes = kg / 1000
  return { value: formatNumber(tonnes, tonnes >= 1000 ? 0 : 1), unit: 'tCO₂e' }
}

export default function Dashboard() {
  usePageTitle('Dashboard')
  const [years, setYears] = useState<number[] | null>(null)
  const [year, setYear] = useState<number | null>(null)
  const [data, setData] = useState<DashboardData | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    dashboardApi
      .years()
      .then(list => {
        setYears(list)
        setYear(defaultYear(list))
        if (!list.length) setLoading(false)
      })
      .catch(err => {
        toast.error(errorMessage(err, 'Could not load the dashboard'))
        setYears([])
        setLoading(false)
      })
  }, [])

  useEffect(() => {
    if (year === null) return
    let cancelled = false
    setLoading(true)
    Promise.all([dashboardApi.summary(year), dashboardApi.monthly(year), dashboardApi.byCategory(year), dashboardApi.byFacility(year)])
      .then(([summary, monthly, byCategory, byFacility]) => {
        if (!cancelled) setData({ summary, monthly, byCategory, byFacility })
      })
      .catch(err => {
        if (!cancelled) {
          setData(null)
          toast.error(errorMessage(err, 'Could not load the dashboard'))
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [year])

  const header = (
    <PageHead title="Dashboard" sub={`${year ?? ''} · GHG Protocol · all facilities`.replace(/^ · /, '')}>
      <YearSelect years={years ?? []} value={year} onChange={setYear} />
    </PageHead>
  )

  if (loading && !data) {
    return (
      <>
        {header}
        <div className="panel empty">
          <Spinner label="Loading dashboard" />
        </div>
      </>
    )
  }

  if (!data || data.summary.totalCo2eKg === 0) {
    return (
      <>
        {header}
        <div className="panel">
          <EmptyState
            title="No emissions data yet"
            message="Add activity records for your facilities to see your emissions here."
            action={
              <Link to="/activities" className="pill-btn dark">
                Add activity records
              </Link>
            }
          />
        </div>
      </>
    )
  }

  const { summary, monthly, byCategory, byFacility } = data
  const change = summary.changeVsPrevYearPct
  const share = (kg: number) => `${((kg / summary.totalCo2eKg) * 100).toFixed(0)}% of total`
  const total = kpiValue(summary.totalCo2eKg)
  const s1 = kpiValue(summary.scope1Kg)
  const s2 = kpiValue(summary.scope2Kg)
  const s3 = kpiValue(summary.scope3Kg)

  const monthlyRows = monthly.map(m => ({
    month: monthName(m.month),
    'Scope 1': toTonnes(m.scope1Kg),
    'Scope 2': toTonnes(m.scope2Kg),
    'Scope 3': toTonnes(m.scope3Kg),
  }))
  const categoryRows = byCategory.map((c, i) => ({
    name: categoryLabel(c.category),
    value: toTonnes(c.co2eKg),
    pct: c.pct,
    color: CATEGORY_COLORS[i % CATEGORY_COLORS.length],
  }))
  const peak = monthly.reduce((best, m) => (m.totalKg > best.totalKg ? m : best), monthly[0])
  const topCategory = byCategory[0]
  const topFacility = byFacility[0]
  const maxFacilityKg = Math.max(...byFacility.map(f => f.co2eKg), 1)

  return (
    <div style={{ opacity: loading ? 0.6 : 1, transition: 'opacity .15s' }}>
      {header}

      <div className="grid-x g-4">
        <KpiCard
          color="orange"
          label="Total emissions"
          icon={<Leaf size={18} strokeWidth={1.8} />}
          value={total.value}
          unit={total.unit}
          delta={change === null ? 'No previous year' : `${change <= 0 ? '↓' : '↑'} ${Math.abs(change).toFixed(1)}%`}
          sub={change === null ? undefined : `vs. ${summary.year - 1}`}
        />
        <KpiCard color="dark" label="Scope 1" icon={<Flame size={18} strokeWidth={1.8} />} value={s1.value} unit={s1.unit} delta={share(summary.scope1Kg)} sub="Direct fuel use" />
        <KpiCard color="green" label="Scope 2" icon={<Zap size={18} strokeWidth={1.8} />} value={s2.value} unit={s2.unit} delta={share(summary.scope2Kg)} sub="Purchased electricity" />
        <KpiCard color="pink" label="Scope 3" icon={<Truck size={18} strokeWidth={1.8} />} value={s3.value} unit={s3.unit} delta={share(summary.scope3Kg)} sub="Travel, freight, waste" />
      </div>

      <div className="grid-x g-main">
        <div className="panel">
          <div className="panel-head">
            <h3>Monthly emissions (tCO₂e)</h3>
            <div className="legend">
              <span>
                <i style={{ background: SCOPE_COLORS.SCOPE_1 }} />
                Scope 1
              </span>
              <span>
                <i style={{ background: SCOPE_COLORS.SCOPE_2 }} />
                Scope 2
              </span>
              <span>
                <i style={{ background: SCOPE_COLORS.SCOPE_3 }} />
                Scope 3
              </span>
            </div>
          </div>
          <div style={{ height: 260 }}>
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={monthlyRows} margin={{ top: 6, right: 4, left: 0, bottom: 0 }} barCategoryGap="22%">
                <CartesianGrid vertical={false} stroke="#f0f0ee" />
                <XAxis dataKey="month" tickLine={false} axisLine={false} tick={AXIS} />
                <YAxis tickLine={false} axisLine={false} tick={AXIS} width={44} tickFormatter={v => formatNumber(v)} />
                <Tooltip formatter={tonnesTooltip} cursor={{ fill: '#f4f4f3' }} contentStyle={TOOLTIP_STYLE} />
                <Bar dataKey="Scope 1" stackId="scope" fill={SCOPE_COLORS.SCOPE_1} />
                <Bar dataKey="Scope 2" stackId="scope" fill={SCOPE_COLORS.SCOPE_2} />
                <Bar dataKey="Scope 3" stackId="scope" fill={SCOPE_COLORS.SCOPE_3} radius={[5, 5, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="panel">
          <div className="panel-head">
            <h3>Highlights</h3>
            <span className="link">{summary.year}</span>
          </div>
          <div className="alert-row">
            <div className="alert-ic">
              <Zap size={18} strokeWidth={1.8} />
            </div>
            <div>
              <b style={{ fontWeight: 500, display: 'block' }}>Largest source: {topCategory ? categoryLabel(topCategory.category) : 'not available'}</b>
              <span className="small-note">{topCategory ? `${topCategory.pct.toFixed(1)}% of emissions` : ''}</span>
            </div>
          </div>
          <div className="alert-row">
            <div className="alert-ic">
              <Building2 size={18} strokeWidth={1.8} />
            </div>
            <div>
              <b style={{ fontWeight: 500, display: 'block' }}>Largest facility: {topFacility?.facilityName ?? 'not available'}</b>
              <span className="small-note">{topFacility ? `${topFacility.pct.toFixed(1)}% of emissions` : ''}</span>
            </div>
          </div>
          <div className="alert-row">
            <div className="alert-ic">
              <CalendarDays size={18} strokeWidth={1.8} />
            </div>
            <div>
              <b style={{ fontWeight: 500, display: 'block' }}>Peak month: {monthName(peak.month)}</b>
              <span className="small-note">{formatCo2e(peak.totalKg)}</span>
            </div>
          </div>
          <div className="alert-row">
            <div className="alert-ic">{change !== null && change > 0 ? <ArrowUp size={18} strokeWidth={1.8} /> : <ArrowDown size={18} strokeWidth={1.8} />}</div>
            <div>
              <b style={{ fontWeight: 500, display: 'block' }}>
                {change === null ? 'No previous year to compare' : `${Math.abs(change).toFixed(1)}% ${change <= 0 ? 'lower' : 'higher'} than ${summary.year - 1}`}
              </b>
              <span className="small-note">Total emissions, year on year</span>
            </div>
            {change !== null && (
              <span style={{ marginLeft: 'auto' }}>
                <span className={`tag ${change <= 0 ? 'good' : 'bad'}`}>{change <= 0 ? 'Improving' : 'Rising'}</span>
              </span>
            )}
          </div>
        </div>
      </div>

      <div className="grid-x g-2">
        <div className="panel">
          <div className="panel-head">
            <h3>Category split</h3>
            <span className="link">{summary.year}</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 24, flexWrap: 'wrap' }}>
            <div style={{ position: 'relative', width: 170, height: 170, flexShrink: 0 }}>
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie data={categoryRows} dataKey="value" nameKey="name" innerRadius={51} outerRadius={73} paddingAngle={2} stroke="none" startAngle={90} endAngle={-270}>
                    {categoryRows.map(row => (
                      <Cell key={row.name} fill={row.color} />
                    ))}
                  </Pie>
                  <Tooltip formatter={tonnesTooltip} contentStyle={TOOLTIP_STYLE} />
                </PieChart>
              </ResponsiveContainer>
              <div style={{ position: 'absolute', inset: 0, display: 'grid', placeItems: 'center', textAlign: 'center', pointerEvents: 'none' }}>
                <div>
                  <div style={{ fontSize: 22, fontWeight: 500, lineHeight: 1.1 }}>{(summary.totalCo2eKg / 1e6).toFixed(1)}k</div>
                  <div style={{ fontSize: 11, color: '#8a8a88' }}>tCO₂e</div>
                </div>
              </div>
            </div>
            <div style={{ flex: 1, minWidth: 180 }}>
              {categoryRows.map(row => (
                <div key={row.name} style={{ marginBottom: 10 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 5 }}>
                    <span>
                      <i style={{ display: 'inline-block', width: 10, height: 10, borderRadius: 3, background: row.color, marginRight: 8 }} />
                      {row.name}
                    </span>
                    <b style={{ fontWeight: 500 }}>{row.pct.toFixed(1)}%</b>
                  </div>
                  <div className="bar-track">
                    <div className="bar-fill" style={{ width: `${Math.min(100, row.pct)}%`, background: row.color }} />
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="panel">
          <div className="panel-head">
            <h3>Facility comparison</h3>
            <span className="link">tCO₂e · {summary.year}</span>
          </div>
          <table className="tbl">
            <thead>
              <tr>
                <th>Facility</th>
                <th style={{ width: '40%' }}>Emissions</th>
                <th className="r">tCO₂e</th>
                <th className="r">Share</th>
              </tr>
            </thead>
            <tbody>
              {byFacility.map(f => (
                <tr className="hov" key={f.facilityId}>
                  <td>
                    <b style={{ fontWeight: 500 }}>{f.facilityName}</b>
                  </td>
                  <td>
                    <div className="bar-track">
                      <div className="bar-fill" style={{ width: `${(f.co2eKg / maxFacilityKg) * 100}%` }} />
                    </div>
                  </td>
                  <td className="r">{formatNumber(toTonnes(f.co2eKg), 1)}</td>
                  <td className="r">
                    <span className="tag info">{f.pct.toFixed(1)}%</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
