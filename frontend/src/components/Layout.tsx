import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { Award, Building2, LayoutGrid, Leaf, LogOut, Sparkles, Target, Upload } from 'lucide-react'
import { useAuth } from '../auth/AuthContext'
import { USE_MOCKS } from '../api/client'

const NAV = [
  { to: '/', label: 'Dashboard', icon: LayoutGrid },
  { to: '/activities', label: 'Activity data', icon: Upload },
  { to: '/facilities', label: 'Facilities', icon: Building2 },
  { to: '/targets', label: 'Targets', icon: Target },
  { to: '/incentives', label: 'Incentives', icon: Award },
  { to: '/insights', label: 'AI insights', icon: Sparkles },
]

function initials(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map(part => part[0].toUpperCase())
    .join('')
}

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  if (!user) return null

  return (
    <div className="stage">
      <div className="shell">
        <aside className="sidebar">
          <div className="logo">
            <span className="logo-mark">
              <Leaf size={18} color="#8fd48f" strokeWidth={1.8} />
            </span>
            CarbonTrack
          </div>
          <div className="company-switch" title={user.companyName}>
            <span className="avatar-sq">{user.companyName[0]?.toUpperCase()}</span>
            <span>{user.companyName}</span>
          </div>
          <nav className="nav">
            {NAV.map(({ to, label, icon: Icon }) => (
              <NavLink key={to} to={to} end={to === '/'} className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
                <Icon size={18} strokeWidth={1.8} />
                {label}
              </NavLink>
            ))}
          </nav>
          <div className="role-chip">
            <b>Signed in as</b>
            {user.email}
            <div>
              <span className={`tag ${user.role === 'ADMIN' ? 'good' : 'info'}`}>{user.role === 'ADMIN' ? 'Admin' : 'Analyst'}</span>
            </div>
            {USE_MOCKS && <div className="mock-note">Mock mode: local sample data</div>}
          </div>
        </aside>

        <main className="main">
          <div className="topbar">
            <div className="spacer" />
            <button className="pill-btn" onClick={() => navigate('/insights')}>
              <Sparkles size={16} strokeWidth={1.8} /> Ask CarbonTrack
            </button>
            <div className="who">
              <b>{user.name}</b>
              <span>{user.companyName}</span>
            </div>
            <div className="avatar">{initials(user.name)}</div>
            <button className="icon-btn" aria-label="Log out" title="Log out" onClick={logout}>
              <LogOut size={18} strokeWidth={1.8} />
            </button>
          </div>
          <Outlet />
        </main>
      </div>
    </div>
  )
}
