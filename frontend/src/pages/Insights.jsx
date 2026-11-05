import React, { useState, useRef, useEffect } from 'react'
import { Icon, Tag, PageHead, ForecastChart } from '../components/ui.jsx'
import { actualSeries, forecastSeries, botReplies } from '../data.js'

export function AIInsights() {
  const [msgs, setMsgs] = useState([{ r: 'bot', t: 'Hi! I am your sustainability assistant. Ask me about your emissions, anomalies or reduction options.' }])
  const [input, setInput] = useState('')
  const end = useRef(null)
  useEffect(() => { end.current?.scrollIntoView({ behavior: 'smooth' }) }, [msgs])
  const ask = text => {
    if (!text.trim()) return
    const k = /anomal|spike|june/i.test(text) ? 'anomaly' : /target|track|pace/i.test(text) ? 'target' : 'default'
    setMsgs(m => [...m, { r: 'user', t: text }, { r: 'bot', t: botReplies[k] }])
    setInput('')
  }
  return (
    <>
      <PageHead title="AI insights" sub="Forecasting and anomaly detection (Tribuo) · assistant (Spring AI)">
        <Tag kind="pink">Preview data</Tag>
      </PageHead>
      <div className="grid g-main">
        <div className="panel">
          <div className="panel-head">
            <h3>Emissions forecast · next 6 months</h3>
            <div className="legend">
              <span><i style={{ background: '#138a1d' }} />Actual</span>
              <span><i style={{ background: '#e973a8' }} />Forecast ±band</span>
              <span><i style={{ background: '#c0392b', borderRadius: 9 }} />Anomaly</span>
            </div>
          </div>
          <ForecastChart actual={actualSeries} forecast={forecastSeries} />
        </div>
        <div className="panel">
          <div className="panel-head"><h3>Reduction suggestions</h3></div>
          {[['Green PPA for Jamshedpur', '−900 t/yr', 'High impact'], ['Shift 30% fleet to CNG/EV', '−620 t/yr', 'Medium'], ['LED + HVAC schedule, Delhi HQ', '−85 t/yr', 'Quick win']].map(([t, s, tag]) => (
            <div className="alert-row" key={t}>
              <div className="alert-ic"><Icon n="leaf" /></div>
              <div><b>{t}</b><span>{s}</span></div>
              <span style={{ marginLeft: 'auto' }}><Tag kind="good">{tag}</Tag></span>
            </div>
          ))}
        </div>
      </div>
      <div className="grid g-2">
        <div className="panel">
          <div className="panel-head"><h3>Detected anomalies</h3><Tag kind="bad">1 new</Tag></div>
          <table>
            <thead><tr><th>Where</th><th>Period</th><th className="r">Score</th></tr></thead>
            <tbody>
              <tr className="hov"><td><b style={{ fontWeight: 500 }}>Jamshedpur · Diesel</b><div className="small-note">+41% vs. 3-month average</div></td><td>May 2026</td><td className="r"><Tag kind="bad">0.82</Tag></td></tr>
              <tr className="hov"><td><b style={{ fontWeight: 500 }}>Fleet · Road freight</b><div className="small-note">+17% vs. seasonal expectation</div></td><td>Aug 2026</td><td className="r"><Tag kind="warn">0.61</Tag></td></tr>
            </tbody>
          </table>
        </div>
        <div className="panel">
          <div className="panel-head"><h3><Icon n="spark" s={16} /> Ask CarbonTrack</h3></div>
          <div className="chat">{msgs.map((m, i) => <div key={i} className={`msg ${m.r}`}>{m.t}</div>)}<div ref={end} /></div>
          <div className="chips">{['Why the spike in May?', 'Are we on track for 2030?', 'Biggest reduction lever?'].map(c => <button key={c} className="chip" onClick={() => ask(c)}>{c}</button>)}</div>
          <form className="chat-input" onSubmit={e => { e.preventDefault(); ask(input) }}>
            <input value={input} onChange={e => setInput(e.target.value)} placeholder="Ask about your emissions…" />
            <button className="pill-btn dark" type="submit"><Icon n="send" s={16} /></button>
          </form>
        </div>
      </div>
    </>
  )
}

export function Reports({ toast }) {
  const rows = [['BRSR Principle 6 – FY 2025-26', 'PDF', 'Draft', 'Oct 30'], ['GHG Inventory (Scope 1-3) – Q2', 'Excel', 'Approved', 'Oct 12'], ['BRSR Principle 6 – FY 2024-25', 'PDF', 'Filed', 'Jun 02'], ['Facility comparison – Sep', 'PDF', 'Approved', 'Oct 03']]
  const k = { Draft: 'warn', Approved: 'good', Filed: 'info' }
  return (
    <>
      <PageHead title="Reports" sub="BRSR-style disclosures and GHG inventories">
        <button className="pill-btn dark" onClick={() => toast('Generating BRSR PDF…')}><Icon n="file" s={16} /> Generate BRSR PDF</button>
      </PageHead>
      <div className="grid g-main">
        <div className="panel">
          <div className="panel-head"><h3>Report builder</h3></div>
          <div className="form-grid">
            <div className="field"><label>Report type</label><select><option>BRSR Principle 6</option><option>GHG inventory</option><option>Facility comparison</option></select></div>
            <div className="field"><label>Period</label><select><option>FY 2025-26</option><option>FY 2024-25</option></select></div>
            <div className="field"><label>Format</label><select><option>PDF</option><option>Excel</option></select></div>
          </div>
          <div className="preview"><div><div className="small-note">Includes</div><b style={{ fontSize: 15 }}>Scope 1, 2, 3 · intensity · targets · methodology</b></div><button className="pill-btn green" onClick={() => toast('Report queued')}>Generate</button></div>
        </div>
        <div className="kpi green" style={{ minHeight: 0 }}>
          <div className="k-top"><span>Audit readiness</span><div className="k-icon"><Icon n="check" /></div></div>
          <div className="k-val" style={{ marginTop: 30 }}>96<small>%</small></div><div className="k-sub">records with factor + source trail</div>
        </div>
      </div>
      <div className="panel">
        <div className="panel-head"><h3>Generated reports</h3></div>
        <table>
          <thead><tr><th>Report</th><th>Format</th><th>Status</th><th>Date</th><th></th></tr></thead>
          <tbody>{rows.map(r => (
            <tr className="hov" key={r[0]}><td><b style={{ fontWeight: 500 }}>{r[0]}</b></td><td>{r[1]}</td><td><Tag kind={k[r[2]]}>{r[2]}</Tag></td><td>{r[3]}</td><td className="r"><button className="pill-btn sm" onClick={() => toast('Download started')}>Download</button></td></tr>))}</tbody>
        </table>
      </div>
    </>
  )
}
