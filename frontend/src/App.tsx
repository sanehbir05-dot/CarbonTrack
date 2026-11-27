import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { Toaster } from 'react-hot-toast'
import { AuthProvider } from './auth/AuthContext'
import ProtectedRoute from './auth/ProtectedRoute'
import Layout from './components/Layout'
import Activities from './pages/Activities'
import Dashboard from './pages/Dashboard'
import Facilities from './pages/Facilities'
import Incentives from './pages/Incentives'
import Insights from './pages/Insights'
import Login from './pages/Login'
import NotFound from './pages/NotFound'
import Register from './pages/Register'
import Targets from './pages/Targets'

export default function App() {
  return (
    <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route element={<ProtectedRoute />}>
            <Route element={<Layout />}>
              <Route path="/" element={<Dashboard />} />
              <Route path="/activities" element={<Activities />} />
              <Route path="/facilities" element={<Facilities />} />
              <Route path="/targets" element={<Targets />} />
              <Route path="/incentives" element={<Incentives />} />
              <Route path="/insights" element={<Insights />} />
              <Route path="*" element={<NotFound />} />
            </Route>
          </Route>
        </Routes>
        <Toaster
          position="bottom-center"
          containerStyle={{ bottom: 52 }}
          toastOptions={{ style: { background: '#0d0d0d', color: '#fff', borderRadius: 99, padding: '10px 18px', fontSize: 14 } }}
        />
      </AuthProvider>
    </BrowserRouter>
  )
}
