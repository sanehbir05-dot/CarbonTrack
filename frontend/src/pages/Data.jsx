import React, { useState } from 'react'
import { Icon, Tag, Bar, PageHead } from '../components/ui.jsx'
import { activity, factors } from '../data.js'

const catFactor = { 'Electricity (grid)': 0.82, 'Diesel – stationary': 2.68, 'Natural gas': 1.9, 'Road freight (HGV)': 0.092, 'Business travel – air': 0.148 }
const catUnit = { 'Electricity (grid)': 'kWh', 'Diesel – stationary': 'litres', 'Natural gas': 'm³', 'Road freight (HGV)': 'tonne-km', 'Business travel – air': 'passenger-km' }
const scopeKind = { 1: 'info', 2: 'good', 3: 'pink' }

export function Ingestion({ toast }) {
  const [cat, setCat] = useState('Electricity (grid)')
  const [qty, setQty] = useState('')
  const co2e = qty ? ((+qty * catFactor[cat]) / 1000).toFixed(2) : null
  return (
    <>
      <PageHead title="Activity data" sub="Manual entry, CSV/Excel upload, REST API and simulated IoT meters">
        <button className="pill-btn"><Icon n="db" s={16} /> API keys</button>
        <button className="pill-btn dark" onClick={() => toast('Template downloaded')}><Icon n="file" s={16} /> Download template</button>
      </PageHead>

      <div className="grid g-main">
        <div className="panel">
          <div className="panel-head"><h3>Add activity record</h3><Tag kind="info">Live CO₂e preview</Tag></div>
          <div className="form-grid">
            <div className="field"><label>Facility</label><select><option>Pune Plant</option><option>Jamshedpur Works</option><option>Delhi HQ</option></select></div>
            <div className="field"><label>Category</label>
              <select value={cat} onChange={e => setCat(e.target.value)}>{Object.keys(catFactor).map(c => <option key={c}>{c}</option>)}</select></div>
            <div className="field"><label>Period</label><input type="month" defaultValue="2026-10" /></div>
            <div className="field"><label>Quantity</label><input type="number" placeholder="e.g. 48000" value={qty} onChange={e => setQty(e.target.value)} /></div>
            <div className="field"><label>Unit</label><input value={catUnit[cat]} readOnly /></div>
            <div className="field"><label>Emission factor</label><input value={`${catFactor[cat]} kg CO₂e / ${catUnit[cat]}`} readOnly /></div>
          </div>
          <div className="preview">
            <div><div className="small-note">Calculated emissions (activity × factor)</div><b>{co2e ? `${co2e} tCO₂e` : '—'}</b></div>
            <button className="pill-btn green" onClick={() => { toast('Record saved & CO₂e calculated'); setQty('') }}><Icon n="check" s={16} /> Save record</button>
          </div>
        </div>
        <div className="panel">
          <div className="panel-head"><h3>Bulk upload</h3></div>
          <div className="drop"><Icon n="upload" s={28} /><b style={{ marginTop: 8 }}>Drop CSV / Excel here</b>or click to browse · .csv .xlsx</div>
          <div style={{ marginTop: 16 }}>
            <div className="alert-row"><div className="alert-ic"><Icon n="bolt" /></div><div><b>Simulated IoT meters</b><span>Pune, Chennai · every 15 min</span></div><span style={{ marginLeft: 'auto' }}><Tag kind="good">Running</Tag></span></div>
            <div className="alert-row"><div className="alert-ic"><Icon n="db" /></div><div><b>REST ingestion</b><span>POST /api/v1/activity</span></div><span style={{ marginLeft: 'auto' }}><Tag kind="good">Active</Tag></span></div>
          </div>
        </div>
      </div>

      <div className="panel">
        <div className="panel-head"><h3>Recent records · Oct 2026</h3><span className="link">{activity.length} of 1,284</span></div>
        <table>
          <thead><tr><th>Facility / category</th><th>Source</th><th className="r">Quantity</th><th>Scope</th><th className="r">tCO₂e</th><th>Factor</th></tr></thead>
          <tbody>
            {activity.map(a => (
              <tr className="hov" key={a.id}>
                <td><span className="cell-ic"><i><Icon n={a.icon} s={16} /></i><span><b style={{ fontWeight: 500 }}>{a.cat}</b><div className="small-note">{a.fac}</div></span></span></td>
                <td><Tag kind="info">{a.via}</Tag></td>
                <td className="r">{a.qty.toLocaleString()} {a.unit}</td>
                <td><Tag kind={scopeKind[a.scope]}>Scope {a.scope}</Tag></td>
                <td className="r"><b style={{ fontWeight: 500 }}>{a.co2e}</b></td>
                <td className="small-note">{a.src}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  )
}

export function Factors({ toast }) {
  const [q, setQ] = useState('')
  const rows = factors.filter(f => f.cat.toLowerCase().includes(q.toLowerCase()))
  return (
    <>
      <PageHead title="Emission factors" sub="Versioned by source, region and year">
        <button className="pill-btn" onClick={() => toast('Climatiq lookup: 212 / 250 calls left')}><Icon n="spark" s={16} /> Climatiq lookup</button>
        <button className="pill-btn dark" onClick={() => toast('Excel importer opened')}><Icon n="upload" s={16} /> Import Excel</button>
      </PageHead>
      <div className="grid g-3">
        {[['CEA CO₂ Baseline DB v20', 'India grid · Scope 2', 'green'], ['UK DESNZ 2024', 'Fuels, travel, freight, waste', 'pink'], ['US EPA Hub', 'Backup source', 'orange']].map(([t, s, c]) => (
          <div key={t} className={`kpi ${c}`} style={{ minHeight: 130 }}>
            <div className="k-top"><span>{s}</span><div className="k-icon"><Icon n="db" /></div></div>
            <div style={{ fontSize: 20 }}>{t}</div>
          </div>
        ))}
      </div>
      <div className="panel">
        <div className="panel-head">
          <h3>Factor library</h3>
          <div className="search" style={{ padding: '8px 14px', background: 'var(--canvas)' }}><Icon n="search" s={16} /><input placeholder="Search category…" value={q} onChange={e => setQ(e.target.value)} /></div>
        </div>
        <table>
          <thead><tr><th>Category</th><th className="r">Value</th><th>Unit</th><th>Source</th><th>Region</th><th>Year</th><th>Version</th></tr></thead>
          <tbody>{rows.map(f => (
            <tr className="hov" key={f.cat}>
              <td><b style={{ fontWeight: 500 }}>{f.cat}</b></td><td className="r">{f.value}</td><td className="small-note">{f.unit}</td>
              <td><Tag kind="info">{f.src}</Tag></td><td>{f.region}</td><td>{f.year}</td><td>{f.ver}</td>
            </tr>))}</tbody>
        </table>
      </div>
    </>
  )
}
