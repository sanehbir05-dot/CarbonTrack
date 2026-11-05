import React from 'react'
import { Icon, Tag, Bar, PageHead, Ring } from '../components/ui.jsx'
import { actions, rules, audit, alerts } from '../data.js'

export function Targets() {
  const statusKind = { Completed: 'good', 'In progress': 'warn', Planned: 'info' }
  return (
    <>
      <PageHead title="Targets & actions" sub="Baseline FY22 · reduction target 45% by FY30">
        <button className="pill-btn dark"><Icon n="plus" s={16} /> Log action</button>
      </PageHead>
      <div className="grid g-3">
        <div className="panel" style={{ textAlign: 'center' }}>
          <h3 style={{ textAlign: 'left' }}>Progress to target</h3>
          <div className="ring-wrap" style={{ width: 170, height: 170 }}><Ring value={25} size={170} /><div className="num">25<small>%</small></div></div>
          <p className="small-note">11.4% reduced of 45% goal</p>
        </div>
        <div className="panel">
          <h3>Baseline vs. now</h3>
          <div style={{ marginTop: 18 }}>
            {[['FY22 baseline', 31000, '#0d0d0d'], ['FY26 (current)', 27600, '#138a1d'], ['FY30 target', 17050, '#f59a4d']].map(([l, v, c]) => (
              <div key={l} style={{ marginBottom: 16 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}><span>{l}</span><b style={{ fontWeight: 500 }}>{v.toLocaleString()} t</b></div>
                <Bar pct={(v / 31000) * 100} color={c} />
              </div>
            ))}
          </div>
        </div>
        <div className="kpi green">
          <div className="k-top"><span>Planned savings</span><div className="k-icon"><Icon n="leaf" /></div></div>
          <div><div className="k-val">4,770<small>tCO₂e/yr</small></div><div className="k-delta">4 actions</div><div className="k-sub">1 completed · 2 in progress</div></div>
        </div>
      </div>
      <div className="panel">
        <div className="panel-head"><h3>Reduction actions</h3></div>
        <table>
          <thead><tr><th>Action</th><th>Category</th><th style={{ width: 200 }}>Progress</th><th className="r">Saving (t/yr)</th><th>Status</th></tr></thead>
          <tbody>{actions.map(a => (
            <tr className="hov" key={a.name}>
              <td><b style={{ fontWeight: 500 }}>{a.name}</b></td><td className="small-note">{a.cat}</td>
              <td><Bar pct={a.pct} /></td><td className="r">{a.saving.toLocaleString()}</td>
              <td><Tag kind={statusKind[a.status]}>{a.status}</Tag></td>
            </tr>))}</tbody>
        </table>
      </div>
    </>
  )
}

export function Incentives({ toast }) {
  return (
    <>
      <PageHead title="Green incentives" sub="Rules are configurable in the database · credits are simulated">
        <button className="pill-btn dark" onClick={() => toast('Eligibility re-evaluated')}><Icon n="check" s={16} /> Re-evaluate</button>
      </PageHead>
      <div className="panel" style={{ marginBottom: 16, display: 'flex', alignItems: 'center', gap: 20, flexWrap: 'wrap' }}>
        <div className="kpi pink" style={{ minHeight: 0, flex: '0 0 280px', padding: 22 }}>
          <div className="k-top"><span>Current standing</span><div className="k-icon"><Icon n="award" /></div></div>
          <div className="k-val" style={{ marginTop: 28 }}>Tier B</div>
          <div className="k-sub">11.4% below baseline</div>
        </div>
        <div style={{ flex: 1, minWidth: 240 }}>
          <h3>3.6% more to reach Tier A</h3>
          <p className="small-note" style={{ margin: '6px 0 14px' }}>Completing waste-heat recovery at Jamshedpur is projected to unlock Tier A.</p>
          <Bar pct={(11.4 / 15) * 100} />
        </div>
      </div>
      <div className="grid g-3">
        {rules.map(r => (
          <div key={r.name} className={`tier ${r.tier === 'B' ? 'earned' : ''}`}>
            <div className="medal" style={{ background: r.color }}>{r.tier}</div>
            <h3>{r.name}</h3>
            <p className="small-note" style={{ margin: '6px 0' }}>{r.cond}</p>
            <b style={{ fontWeight: 500 }}>{r.reward}</b>
            <div style={{ marginTop: 12 }}>{r.tier === 'B' ? <Tag kind="good">Awarded</Tag> : r.tier === 'C' ? <Tag kind="info">Passed</Tag> : <Tag kind="warn">Not yet</Tag>}</div>
          </div>
        ))}
      </div>
    </>
  )
}

export function Alerts() {
  return (
    <>
      <PageHead title="Alerts & audit" sub="Spike detection, target slip and full change history (Hibernate Envers)" />
      <div className="grid g-2">
        <div className="panel">
          <div className="panel-head"><h3>Alerts</h3><Tag kind="bad">2 open</Tag></div>
          {alerts.map((a, i) => (
            <div className="alert-row" key={i}>
              <div className="alert-ic"><Icon n={a.icon} /></div>
              <div><b>{a.title}</b><span>{a.time}</span></div>
              <span style={{ marginLeft: 'auto' }}><Tag kind={a.kind}>{a.kind === 'bad' ? 'Critical' : a.kind === 'warn' ? 'Warning' : a.kind === 'good' ? 'Positive' : 'Info'}</Tag></span>
            </div>
          ))}
        </div>
        <div className="panel">
          <div className="panel-head"><h3>Audit log</h3><span className="link">Immutable</span></div>
          {audit.map((a, i) => (
            <div className="alert-row" key={i}>
              <div className="alert-ic"><Icon n="history" /></div>
              <div><b>{a.what}</b><span>{a.who} · {a.role} · {a.when}</span></div>
            </div>
          ))}
        </div>
      </div>
    </>
  )
}
