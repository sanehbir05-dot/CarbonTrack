import { useEffect, useState } from 'react'
import toast from 'react-hot-toast'
import { Award } from 'lucide-react'
import { dashboardApi } from '../api/dashboard'
import { errorMessage } from '../api/client'
import { incentivesApi } from '../api/incentives'
import type { IncentiveRule, IncentiveStatus, Tier } from '../api/types'
import Bar from '../components/Bar'
import EmptyState from '../components/EmptyState'
import PageHead from '../components/PageHead'
import Spinner from '../components/Spinner'
import Tag from '../components/Tag'
import YearSelect, { defaultYear } from '../components/YearSelect'
import { formatNumber } from '../utils/format'
import { usePageTitle } from '../utils/usePageTitle'

const MEDAL: Record<Tier, string> = { NONE: '#8a8a88', BRONZE: '#f59a4d', SILVER: '#8a8a88', GOLD: '#d99100' }

const titleCase = (s: string) => s.charAt(0) + s.slice(1).toLowerCase()

export default function Incentives() {
  usePageTitle('Incentives')
  const [rules, setRules] = useState<IncentiveRule[] | null>(null)
  const [years, setYears] = useState<number[] | null>(null)
  const [year, setYear] = useState<number | null>(null)
  const [status, setStatus] = useState<IncentiveStatus | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([incentivesApi.rules(), dashboardApi.years()])
      .then(([r, y]) => {
        setRules([...r].sort((a, b) => a.minReductionPct - b.minReductionPct))
        setYears(y)
        setYear(defaultYear(y))
        if (!y.length) setLoading(false)
      })
      .catch(err => {
        toast.error(errorMessage(err, 'Could not load incentives'))
        setRules([])
        setYears([])
        setLoading(false)
      })
  }, [])

  useEffect(() => {
    if (year === null) return
    let cancelled = false
    setLoading(true)
    incentivesApi
      .status(year)
      .then(s => {
        if (!cancelled) setStatus(s)
      })
      .catch(err => {
        if (!cancelled) {
          setStatus(null)
          toast.error(errorMessage(err, 'Could not load your incentive status'))
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
    <PageHead title="Green incentives" sub="Your tier depends on the reduction against the baseline year">
      <Tag kind="pink">Simulated scheme</Tag>
      <YearSelect years={years ?? []} value={year} onChange={setYear} />
    </PageHead>
  )

  if (rules === null || (loading && !status)) {
    return (
      <>
        {header}
        <div className="panel empty">
          <Spinner label="Loading incentives" />
        </div>
      </>
    )
  }

  const reduction = status?.reductionPct ?? 0
  const tierName = status ? (rules.find(r => r.tier === status.tier)?.name ?? titleCase(status.tier)) : ''
  const next = status?.nextTier ?? null
  const nextName = next ? (rules.find(r => r.tier === next.tier)?.name ?? titleCase(next.tier)) : ''
  const toNext = next && next.minReductionPct > 0 ? (Math.max(0, reduction) / next.minReductionPct) * 100 : 100

  return (
    <div style={{ opacity: loading ? 0.6 : 1, transition: 'opacity .15s' }}>
      {header}

      {!status ? (
        <div className="panel" style={{ marginBottom: 16 }}>
          <EmptyState title="No incentive status yet" message="Add activity records for the baseline year and a later year to see your tier." />
        </div>
      ) : (
        <div className="panel" style={{ marginBottom: 16, display: 'flex', alignItems: 'center', gap: 20, flexWrap: 'wrap' }}>
          <div className="kpi pink" style={{ minHeight: 0, flex: '1 1 260px', maxWidth: 320, padding: 22 }}>
            <div className="k-top">
              <span>Current standing · {status.year}</span>
              <div className="k-icon">
                <Award size={18} strokeWidth={1.8} />
              </div>
            </div>
            <div className="k-val" style={{ marginTop: 28 }}>
              {status.tier === 'NONE' ? 'No tier yet' : tierName}
            </div>
            <div className="k-sub">
              {status.baselineYear === null
                ? 'No baseline data yet'
                : `${formatNumber(Math.abs(reduction), 1)}% ${reduction >= 0 ? 'below' : 'above'} the ${status.baselineYear} baseline`}
            </div>
          </div>
          <div style={{ flex: '2 1 280px', minWidth: 0 }}>
            <h3>{next ? `${formatNumber(next.gapPct, 1)}% more to reach ${nextName}` : 'You have reached the top tier'}</h3>
            <p className="small-note" style={{ margin: '6px 0 14px' }}>
              {status.benefit}
            </p>
            <Bar pct={toNext} />
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 14 }}>
              <Tag kind="good">{formatNumber(status.creditPoints)} credit points</Tag>
              <Tag kind="pink">Simulated scheme</Tag>
            </div>
          </div>
        </div>
      )}

      {rules.length === 0 ? (
        <div className="panel">
          <EmptyState title="No tiers defined" message="The incentive rules have not been set up yet." />
        </div>
      ) : (
        <div className="grid-x g-3">
          {rules.map(r => {
            const earned = status?.tier === r.tier
            const passed = !earned && status !== null && reduction >= r.minReductionPct
            return (
              <div key={r.id} className={`tier ${earned ? 'earned' : ''}`}>
                <div className="medal" style={{ background: MEDAL[r.tier] }}>
                  {r.name[0]}
                </div>
                <h3>{r.name}</h3>
                <p className="small-note" style={{ margin: '6px 0' }}>
                  Emissions at least {formatNumber(r.minReductionPct, 1)}% below the baseline year
                </p>
                <b style={{ fontWeight: 500 }}>{formatNumber(r.creditPoints)} credit points</b>
                <p className="small-note" style={{ marginTop: 6 }}>
                  {r.benefit}
                </p>
                <div style={{ marginTop: 12 }}>{earned ? <Tag kind="good">Current tier</Tag> : passed ? <Tag kind="info">Passed</Tag> : <Tag kind="warn">Not yet</Tag>}</div>
              </div>
            )
          })}
        </div>
      )}

      <p className="small-note" style={{ marginTop: 16 }}>
        This incentive scheme is simulated for this project. It is not a real government scheme, and the credit points have no monetary value.
      </p>
    </div>
  )
}
