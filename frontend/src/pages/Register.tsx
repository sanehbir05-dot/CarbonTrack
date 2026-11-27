import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import { useAuth } from '../auth/AuthContext'
import { errorMessage } from '../api/client'
import type { RegisterRequest } from '../api/types'
import { usePageTitle } from '../utils/usePageTitle'
import AuthShell from './AuthShell'

const INDUSTRIES = ['Manufacturing', 'IT Services', 'Logistics', 'Textiles', 'Food Processing', 'Energy', 'Other']

const EMPTY: RegisterRequest = { companyName: '', industry: INDUSTRIES[0], name: '', email: '', password: '' }

export default function Register() {
  usePageTitle('Register')
  const { user, register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState<RegisterRequest>(EMPTY)
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to="/" replace />

  const set = (field: keyof RegisterRequest) => (e: { target: { value: string } }) => setForm(f => ({ ...f, [field]: e.target.value }))

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    try {
      await register({ ...form, companyName: form.companyName.trim(), name: form.name.trim(), email: form.email.trim() })
      toast.success('Company registered')
      navigate('/', { replace: true })
    } catch (err) {
      toast.error(errorMessage(err, 'Could not register'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthShell
      title="Register your company"
      subtitle="This creates your company and an admin account."
      footer={
        <>
          Already registered?{' '}
          <Link to="/login">
            Log in
          </Link>
        </>
      }
    >
      <form onSubmit={submit} className="auth-form">
        <div className="field">
          <label htmlFor="companyName">
            Company name
          </label>
          <input id="companyName" required value={form.companyName} onChange={set('companyName')} />
        </div>
        <div className="field">
          <label htmlFor="industry">
            Industry
          </label>
          <select id="industry" value={form.industry} onChange={set('industry')}>
            {INDUSTRIES.map(i => (
              <option key={i}>{i}</option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="name">
            Your name
          </label>
          <input id="name" autoComplete="name" required value={form.name} onChange={set('name')} />
        </div>
        <div className="field">
          <label htmlFor="email">
            Email
          </label>
          <input id="email" type="email" autoComplete="email" required value={form.email} onChange={set('email')} />
        </div>
        <div className="field">
          <label htmlFor="password">
            Password
          </label>
          <input id="password" type="password" autoComplete="new-password" required minLength={8} value={form.password} onChange={set('password')} />
          <p className="small-note" style={{ marginTop: 6 }}>At least 8 characters.</p>
        </div>
        <button className="pill-btn green block" type="submit" disabled={busy}>
          {busy ? 'Creating account…' : 'Create account'}
        </button>
      </form>
    </AuthShell>
  )
}
