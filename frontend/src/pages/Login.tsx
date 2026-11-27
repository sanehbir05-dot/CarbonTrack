import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import { useAuth } from '../auth/AuthContext'
import { errorMessage } from '../api/client'
import { usePageTitle } from '../utils/usePageTitle'
import AuthShell from './AuthShell'

// Demo login from section A9 of the shared contract.
const DEMO = { email: 'demo@carbontrack.in', password: 'Demo@1234' }

export default function Login() {
  usePageTitle('Log in')
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)

  const from = (location.state as { from?: string } | null)?.from ?? '/'
  if (user) return <Navigate to={from} replace />

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    try {
      await login({ email: email.trim(), password })
      navigate(from, { replace: true })
    } catch (err) {
      toast.error(errorMessage(err, 'Could not log in'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthShell
      title="Log in"
      subtitle="Track and reduce your company's carbon emissions."
      footer={
        <>
          New here?{' '}
          <Link to="/register">
            Register your company
          </Link>
        </>
      }
    >
      <form onSubmit={submit} className="auth-form">
        <div className="field">
          <label htmlFor="email">
            Email
          </label>
          <input id="email" type="email" autoComplete="email" required value={email} onChange={e => setEmail(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor="password">
            Password
          </label>
          <input id="password" type="password" autoComplete="current-password" required value={password} onChange={e => setPassword(e.target.value)} />
        </div>
        <button className="pill-btn green block" type="submit" disabled={busy}>
          {busy ? 'Logging in…' : 'Log in'}
        </button>
        <button
          className="pill-btn soft block"
          type="button"
          onClick={() => {
            setEmail(DEMO.email)
            setPassword(DEMO.password)
          }}
        >
          Use demo account
        </button>
      </form>
    </AuthShell>
  )
}
