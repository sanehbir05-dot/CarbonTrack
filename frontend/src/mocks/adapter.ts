// Axios adapter that answers every request from local mock data when VITE_USE_MOCKS=true.
// It returns the same shapes, status codes, and error body as the real API (contract A8).

import { AxiosError, type AxiosAdapter, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import type { ActivityRequest, Category, Facility, FacilityRequest, TargetRequest } from '../api/types'
import { CATEGORY_META } from '../utils/format'
import * as db from './data'

class HttpError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message)
  }
}

const STATUS_TEXT: Record<number, string> = {
  400: 'Bad Request',
  401: 'Unauthorized',
  403: 'Forbidden',
  404: 'Not Found',
  409: 'Conflict',
}

interface Request {
  method: string
  path: string
  params: Record<string, string | undefined>
  body: any
  user: db.MockUser | null
}

function currentUser(config: InternalAxiosRequestConfig): db.MockUser | null {
  const header = String(config.headers?.Authorization ?? '')
  const match = header.match(/^Bearer mock\.(\d+)$/)
  return match ? (db.users.find(u => u.id === Number(match[1])) ?? null) : null
}

function requireUser(req: Request): db.MockUser {
  if (!req.user) throw new HttpError(401, 'Missing or invalid token')
  return req.user
}

function requireAdmin(req: Request): db.MockUser {
  const user = requireUser(req)
  if (user.role !== 'ADMIN') throw new HttpError(403, 'Only an admin can do this')
  return user
}

const num = (value: string | undefined): number | undefined => (value === undefined || value === '' ? undefined : Number(value))

function validateFacility(body: Partial<FacilityRequest>): FacilityRequest {
  if (!body.name?.trim()) throw new HttpError(400, 'name is required')
  if (!body.city?.trim()) throw new HttpError(400, 'city is required')
  if (!body.state?.trim()) throw new HttpError(400, 'state is required')
  if (!body.type) throw new HttpError(400, 'type is required')
  return { name: body.name.trim(), city: body.city.trim(), state: body.state.trim(), type: body.type }
}

function validateActivity(body: Partial<ActivityRequest>): ActivityRequest {
  if (!db.facilities.some(f => f.id === Number(body.facilityId))) throw new HttpError(404, 'Facility not found')
  if (!body.category || !(body.category in CATEGORY_META)) throw new HttpError(400, 'category is not valid')
  if (!(Number(body.quantity) > 0)) throw new HttpError(400, 'quantity must be positive')
  if (!(Number(body.month) >= 1 && Number(body.month) <= 12)) throw new HttpError(400, 'month must be between 1 and 12')
  if (!Number(body.year)) throw new HttpError(400, 'year is required')
  return {
    facilityId: Number(body.facilityId),
    category: body.category,
    quantity: Number(body.quantity),
    year: Number(body.year),
    month: Number(body.month),
  }
}

function handle(req: Request): { status: number; data?: unknown } {
  const { method, path, params, body } = req
  const idMatch = path.match(/^\/(facilities|activities|targets)\/(\d+)$/)
  const id = idMatch ? Number(idMatch[2]) : 0
  const route = idMatch ? `${method} /${idMatch[1]}/:id` : `${method} ${path}`

  switch (route) {
    case 'GET /health':
      return { status: 200, data: { status: 'UP' } }

    // ----- auth -----
    case 'POST /auth/login': {
      const user = db.users.find(u => u.email.toLowerCase() === String(body?.email ?? '').toLowerCase())
      if (!user || user.password !== body?.password) throw new HttpError(401, 'Invalid email or password')
      return { status: 200, data: { token: `mock.${user.id}`, user: db.toUser(user) } }
    }
    case 'POST /auth/register': {
      for (const field of ['companyName', 'industry', 'name', 'email', 'password']) {
        if (!String(body?.[field] ?? '').trim()) throw new HttpError(400, `${field} is required`)
      }
      if (String(body.password).length < 8) throw new HttpError(400, 'password must be at least 8 characters')
      if (db.users.some(u => u.email.toLowerCase() === String(body.email).toLowerCase())) {
        throw new HttpError(409, 'An account with this email already exists')
      }
      const user = db.addUser(body)
      return { status: 200, data: { token: `mock.${user.id}`, user: db.toUser(user) } }
    }
    case 'GET /auth/me':
      return { status: 200, data: db.toUser(requireUser(req)) }

    // ----- facilities -----
    case 'GET /facilities':
      requireUser(req)
      return { status: 200, data: db.facilities }
    case 'POST /facilities': {
      requireAdmin(req)
      const facility: Facility = { id: Math.max(0, ...db.facilities.map(f => f.id)) + 1, ...validateFacility(body ?? {}) }
      db.facilities.push(facility)
      return { status: 200, data: facility }
    }
    case 'PUT /facilities/:id': {
      requireAdmin(req)
      const index = db.facilities.findIndex(f => f.id === id)
      if (index < 0) throw new HttpError(404, 'Facility not found')
      db.facilities[index] = { id, ...validateFacility(body ?? {}) }
      return { status: 200, data: db.facilities[index] }
    }
    case 'DELETE /facilities/:id': {
      requireAdmin(req)
      const index = db.facilities.findIndex(f => f.id === id)
      if (index < 0) throw new HttpError(404, 'Facility not found')
      db.facilities.splice(index, 1)
      return { status: 204 }
    }

    // ----- emission factors -----
    case 'GET /factors':
      requireUser(req)
      return { status: 200, data: db.factors }

    // ----- activities -----
    case 'GET /activities': {
      requireUser(req)
      const facilityId = num(params.facilityId)
      const year = num(params.year)
      const category = params.category as Category | undefined
      const page = num(params.page) ?? 0
      const size = num(params.size) ?? 20
      const rows = db.activities
        .filter(a => (!facilityId || a.facilityId === facilityId) && (!year || a.year === year) && (!category || a.category === category))
        .sort((a, b) => b.year - a.year || b.month - a.month || b.id - a.id)
      return {
        status: 200,
        data: {
          content: rows.slice(page * size, page * size + size),
          page,
          size,
          totalElements: rows.length,
          totalPages: Math.ceil(rows.length / size),
        },
      }
    }
    case 'POST /activities':
      requireUser(req)
      return { status: 200, data: db.addActivity(validateActivity(body ?? {})) }
    case 'PUT /activities/:id': {
      requireUser(req)
      const updated = db.replaceActivity(id, validateActivity(body ?? {}))
      if (!updated) throw new HttpError(404, 'Activity record not found')
      return { status: 200, data: updated }
    }
    case 'DELETE /activities/:id':
      requireAdmin(req)
      if (!db.removeActivity(id)) throw new HttpError(404, 'Activity record not found')
      return { status: 204 }

    // ----- dashboard -----
    case 'GET /dashboard/years':
      requireUser(req)
      return { status: 200, data: db.dashboardYears() }
    case 'GET /dashboard/summary':
      requireUser(req)
      return { status: 200, data: db.dashboardSummary(num(params.year) ?? db.REPORTING_YEAR) }
    case 'GET /dashboard/monthly':
      requireUser(req)
      return { status: 200, data: db.dashboardMonthly(num(params.year) ?? db.REPORTING_YEAR, num(params.facilityId)) }
    case 'GET /dashboard/by-category':
      requireUser(req)
      return { status: 200, data: db.dashboardByCategory(num(params.year) ?? db.REPORTING_YEAR) }
    case 'GET /dashboard/by-facility':
      requireUser(req)
      return { status: 200, data: db.dashboardByFacility(num(params.year) ?? db.REPORTING_YEAR) }

    // ----- targets -----
    case 'GET /targets':
      requireUser(req)
      return { status: 200, data: db.listTargets() }
    case 'POST /targets': {
      requireAdmin(req)
      const t = (body ?? {}) as Partial<TargetRequest>
      if (!t.name?.trim()) throw new HttpError(400, 'name is required')
      if (!(Number(t.reductionPct) > 0 && Number(t.reductionPct) <= 100)) throw new HttpError(400, 'reductionPct must be between 0 and 100')
      if (!(Number(t.targetYear) > Number(t.baselineYear))) throw new HttpError(400, 'targetYear must be after baselineYear')
      return {
        status: 200,
        data: db.addTarget({ name: t.name.trim(), baselineYear: Number(t.baselineYear), targetYear: Number(t.targetYear), reductionPct: Number(t.reductionPct) }),
      }
    }
    case 'DELETE /targets/:id':
      requireAdmin(req)
      if (!db.removeTarget(id)) throw new HttpError(404, 'Target not found')
      return { status: 204 }

    // ----- incentives -----
    case 'GET /incentives/rules':
      requireUser(req)
      return { status: 200, data: db.incentiveRules }
    case 'GET /incentives/status':
      return { status: 200, data: db.incentiveStatus(num(params.year) ?? db.REPORTING_YEAR, requireUser(req).baselineYear) }

    // ----- AI -----
    case 'GET /ai/forecast':
      requireUser(req)
      return { status: 200, data: db.forecast(Math.min(12, Math.max(1, num(params.months) ?? 6))) }
    case 'GET /ai/anomalies':
      requireUser(req)
      return { status: 200, data: db.anomalies(num(params.year)) }
    case 'POST /ai/assistant': {
      const user = requireUser(req)
      if (!String(body?.question ?? '').trim()) throw new HttpError(400, 'question is required')
      return { status: 200, data: { answer: db.assistantAnswer(String(body.question), user.baselineYear), source: 'RULES' } }
    }

    default:
      throw new HttpError(404, `No mock for ${method} ${path}`)
  }
}

export const mockAdapter: AxiosAdapter = async config => {
  await new Promise(resolve => setTimeout(resolve, 250)) // small delay so loading states are visible

  const [path, query = ''] = (config.url ?? '').split('?')
  const params: Record<string, string | undefined> = Object.fromEntries(new URLSearchParams(query))
  Object.entries(config.params ?? {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') params[key] = String(value)
  })
  let body: unknown = config.data
  if (typeof body === 'string' && body) {
    try {
      body = JSON.parse(body)
    } catch {
      body = undefined
    }
  }

  const method = (config.method ?? 'get').toUpperCase()
  const respond = (status: number, data: unknown): AxiosResponse => ({
    data,
    status,
    statusText: STATUS_TEXT[status] ?? 'OK',
    headers: {},
    config,
  })

  try {
    const result = handle({ method, path, params, body, user: currentUser(config) })
    return respond(result.status, result.data)
  } catch (e) {
    if (!(e instanceof HttpError)) throw e
    const response = respond(e.status, {
      timestamp: new Date().toISOString(),
      status: e.status,
      error: STATUS_TEXT[e.status] ?? 'Error',
      message: e.message,
      path: `/api${path}`,
    })
    throw new AxiosError(`Request failed with status code ${e.status}`, AxiosError.ERR_BAD_REQUEST, config, null, response)
  }
}
