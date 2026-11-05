import React, { useState } from 'react'
import { Icon, Tag, Ring, Bar, PageHead, Segmented, StackedBars, Donut, Spark } from '../components/ui.jsx'
import { monthly, scopeSplit, facilities, alerts } from '../data.js'

const total = scopeSplit.reduce((a, p) => a + p.v, 0)

export default function Dashboard({ go }) {
  const [range, setRange] = useState('12 months')
  const [view, setView] = useState('Scope')
  return (
    <>
      <PageHead title="Dashboard" sub="FY 2025-26 · GHG Protocol · all facilities">
        <Segmented options={['30 days', 'Quarter', '12 months']} value={range} onChange={setRange} />
        <button className="pill-btn dark" onClick={() => go('reports')}><Icon n="upload" s={16} /> Export BRSR</button>
      </PageHead>

      <div className="panel grid g-health" style={{ padding: 20 }}>
        <div className="health">
          <div className="health-title"><h3 style={{ fontSize: 22 }}>Climate Health</h3><Tag kind="good">On watch</Tag></div>
          <div className="ring-wrap">
            <Ring value={74} />
            <div className="num">74<small>/100</small></div>
          </div>
          <p style={{ fontSize: 13 }}>2 risks need attention</p>
          <p className="small-note">Scope 1 diesel spike at Jamshedpur and YTD emissions slightly behind target pace.</p>
        </div>
        <div className="grid g-4" style={{ margin: 0 }}>
          <div className="kpi green">
            <div className="k-top"><span>Total emissions</span><div className="k-icon"><Icon n="leaf" /></div></div>
            <div><div className="k-val">{(total / 1000).toFixed(1)}k<small>tCO₂e</small></div><div className="k-delta">↓ 6.8%</div><div className="k-sub">vs. previous 12 months</div></div>
          </div>
          <div className="kpi pink">
            <div className="k-top"><span>vs. baseline (FY22)</span><div className="k-icon"><Icon n="target" /></div></div>
            <div><div className="k-val">−11.4<small>%</small></div><div className="k-delta">Target: −45% by 2030</div><div className="k-sub">Tier B eligible</div></div>
          </div>
          <div className="kpi orange">
            <div className="k-top"><span>Emission intensity</span><div className="k-icon"><Icon n="bolt" /></div></div>
            <div><div className="k-val">1.84<small>t / ₹ Cr</small></div><div className="k-delta">↓ 3.1%</div><div className="k-sub">vs. previous 12 months</div></div>
          </div>
          <div className="kpi dark">
            <div className="k-top"><span>Credits earned</span><div className="k-icon"><Icon n="award" /></div></div>
            <div><div className="k-val">4,000</div><div className="k-delta">Tier B · simulated</div><div className="k-sub">1 award pending review</div></div>
          </div>
        </div>
      </div>

      <div className="grid g-main">
        <div className="panel">
          <div className="panel-head">
            <h3>Monthly emissions (tCO₂e)</h3>
            <div className="legend">
              <span><i style={{ background: '#0d0d0d' }} />Scope 1</span>
              <span><i style={{ background: '#138a1d' }} />Scope 2</span>
              <span><i style={{ background: '#e973a8' }} />Scope 3</span>
            </div>
          </div>
          <StackedBars data={monthly} target={2100} />
        </div>
        <div className="panel">
          <div className="panel-head"><h3>Recent alerts</h3><button className="link" onClick={() => go('alerts')}>View all</button></div>
          {alerts.map((a, i) => (
            <div className="alert-row" key={i}>
              <div className="alert-ic"><Icon n={a.icon} /></div>
              <div><b>{a.title}</b><span>{a.time}</span></div>
            </div>
          ))}
        </div>
      </div>

      <div className="grid g-2">
        <div className="panel">
          <div className="panel-head"><h3>Scope split</h3><span className="link">Last 12 months</span></div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 24, flexWrap: 'wrap' }}>
            <Donut parts={scopeSplit} />
            <div style={{ flex: 1, minWidth: 180 }}>
              {scopeSplit.map(p => (
                <div key={p.name} style={{ marginBottom: 14 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                    <span><i style={{ display: 'inline-block', width: 10, height: 10, borderRadius: 3, background: p.color, marginRight: 8 }} />{p.name}</span>
                    <b style={{ fontWeight: 500 }}>{Math.round((p.v / total) * 100)}%</b>
                  </div>
                  <Bar pct={(p.v / total) * 100} color={p.color} />
                </div>
              ))}
            </div>
          </div>
        </div>
        <div className="panel">
          <div className="panel-head"><h3>Facility comparison</h3><span className="link">tCO₂e · 12 mo</span></div>
          <table>
            <thead><tr><th>Facility</th><th className="r">Trend</th><th className="r">Emissions</th><th className="r">YoY</th></tr></thead>
            <tbody>
              {facilities.map(f => (
                <tr className="hov" key={f.name}>
                  <td><b style={{ fontWeight: 500 }}>{f.name}</b><div className="small-note">{f.region}</div></td>
                  <td className="r"><Spark values={f.spark} color={f.delta > 0 ? '#c0392b' : '#138a1d'} /></td>
                  <td className="r">{f.t.toLocaleString()}</td>
                  <td className="r"><Tag kind={f.delta > 0 ? 'bad' : 'good'}>{f.delta > 0 ? '+' : ''}{f.delta}%</Tag></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </>
  )
}
