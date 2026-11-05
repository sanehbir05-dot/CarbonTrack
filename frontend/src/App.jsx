import React, { useState } from 'react'
import { Icon } from './components/ui.jsx'
import Dashboard from './pages/Dashboard.jsx'
import { Ingestion, Factors } from './pages/Data.jsx'
import { Targets, Incentives, Alerts } from './pages/Goals.jsx'
import { AIInsights, Reports } from './pages/Insights.jsx'
import { companies, roles } from './data.js'

const NAV = [
  ['dashboard', 'Dashboard', 'dash'], ['activity', 'Activity data', 'upload'], ['factors', 'Emission factors', 'leaf'],
  ['targets', 'Targets & actions', 'target'], ['incentives', 'Incentives', 'award'], ['reports', 'Reports', 'file'],
  ['ai', 'AI insights', 'spark'], ['alerts', 'Alerts & audit', 'bell'],
]

export default function App() {
  const [page, setPage] = useState('dashboard')
  const [company, setCompany] = useState(companies[0])
  const [role, setRole] = useState(roles[0])
  const [msg, setMsg] = useState(null)
  const toast = t => { setMsg(t); setTimeout(() => setMsg(null), 2200) }
  const P = { dashboard: <Dashboard go={setPage} />, activity: <Ingestion toast={toast} />, factors: <Factors toast={toast} />,
    targets: <Targets />, incentives: <Incentives toast={toast} />, reports: <Reports toast={toast} />, ai: <AIInsights />, alerts: <Alerts /> }

  return (
    <div className="stage">
      <div className="shell">
        <aside className="sidebar">
          <div className="logo">
            <span className="logo-mark"><Icon n="leaf" c="#8fd48f" s={18} /></span>CarbonTrack
          </div>
          <div className="company-switch">
            <span className="avatar-sq">{company[0]}</span>
            <select value={company} onChange={e => setCompany(e.target.value)}>{companies.map(c => <option key={c}>{c}</option>)}</select>
          </div>
          <nav className="nav">
            {NAV.map(([id, label, icon]) => (
              <button key={id} className={`nav-item ${page === id ? 'active' : ''}`} onClick={() => setPage(id)}>
                <Icon n={icon} s={18} />{label}{id === 'alerts' && <span className="badge">2</span>}
              </button>
            ))}
          </nav>
          <div className="role-chip">
            <b>Signed in as</b>anita.rao@{company.split(' ')[0].toLowerCase()}.in
            <select value={role} onChange={e => setRole(e.target.value)}>{roles.map(r => <option key={r}>{r}</option>)}</select>
          </div>
        </aside>
        <main className="main">
          <div className="topbar">
            <div className="search"><Icon n="search" s={16} /><input placeholder="Search facilities, records, reports…" /></div>
            <div className="spacer" />
            <button className="pill-btn" onClick={() => setPage('ai')}><Icon n="spark" s={16} /> Ask CarbonTrack</button>
            <button className="icon-btn" onClick={() => setPage('alerts')}><Icon n="bell" /><span className="dot" /></button>
            <div className="avatar">AR</div>
          </div>
          {P[page]}
        </main>
      </div>
      {msg && <div className="toast">{msg}</div>}
    </div>
  )
}
