# CarbonTrack: Frontend Brief (Saneh)

**For the AI assistant reading this:** you are helping **Saneh**, who owns the frontend of a 3-person project that must be built and deployed in 2 to 3 days. Read the appendix "SHARED CONTRACT" first. It defines the API, enums, and rules, and you must follow it exactly. Do not invent endpoints or rename fields. Write working code, keep it simple, and prefer finishing P0 over polishing.

## Team

| Person | Owns |
|---|---|
| **Saneh (you)** | `frontend/`: all UI, charts, API client, mock mode |
| Guransh | Backend core: auth, facilities, activities, calculation, dashboard, targets, incentives |
| Manit | Deployment, CI/CD, seed data, AI endpoints, integration checks |

## Your stack

React 18, Vite, TypeScript, Tailwind CSS, React Router, Axios, Recharts. Add `react-hot-toast` for toasts and `lucide-react` for icons. Do not add a heavy UI kit or a state library. React context plus hooks is enough.

## Setup

```bash
npm create vite@latest frontend -- --template react-ts
cd frontend
npm i react-router-dom axios recharts react-hot-toast lucide-react
npm i -D tailwindcss @tailwindcss/vite
```

Add a dev proxy in `vite.config.ts` so `/api` goes to `http://localhost:8080`. All API calls use the relative base `import.meta.env.VITE_API_BASE_URL ?? '/api'`. Never hardcode a host.

## Folder structure

```
frontend/src/
├─ api/          client.ts (axios + interceptors), one file per resource, types.ts
├─ mocks/        mock data and a mock adapter, used when VITE_USE_MOCKS=true
├─ auth/         AuthContext.tsx, ProtectedRoute.tsx
├─ components/   Layout, Sidebar, Topbar, KpiCard, DataTable, Modal, EmptyState, Spinner, YearSelect
├─ pages/        one file per page
├─ utils/        format.ts (formatCo2e, formatNumber, monthName, categoryLabel)
└─ App.tsx
```

## Rules that affect every page

1. **Types:** `api/types.ts` mirrors section A8 exactly (`User`, `Facility`, `Activity`, `Target`, and so on).
2. **Auth:** store the token in `localStorage`. An axios request interceptor adds `Authorization: Bearer <token>`. A response interceptor clears the token and redirects to `/login` on 401.
3. **Units:** the API returns kg. Show tonnes when the value is 1000 kg or more: `formatCo2e(1234567)` gives `1,234.6 t CO2e`. Below that, show kg.
4. **Year selector:** load `GET /api/dashboard/years`. Default to the last full calendar year if present, else the latest year.
5. **Roles:** hide create, edit, and delete controls for facilities and targets when `user.role === 'ANALYST'`. Analysts can still add activity records.
6. **States:** every page handles loading (spinner or skeleton), empty (message plus a call to action), and error (toast with the `message` field from the error body).
7. **Labels:** show categories as readable text (`BUSINESS_TRAVEL_AIR` becomes "Business travel (air)") and show the unit next to quantity inputs. The unit comes from the category table in A6.
8. **Mock mode:** when `VITE_USE_MOCKS=true`, the API layer returns local mock data shaped exactly like A8. This lets you build on Day 1 before the backend exists. Switching to the real API must need only the env change.

## Pages

| Priority | Route | Page | Endpoints | Content |
|---|---|---|---|---|
| P0 | `/login` | Login | `POST /auth/login` | Email, password, a "Use demo account" button that fills the demo login from A9 |
| P0 | `/register` | Register | `POST /auth/register` | Company name, industry, name, email, password |
| P0 | `/` | Dashboard | `/dashboard/years`, `/summary`, `/monthly`, `/by-category`, `/by-facility` | Year select. 4 KPI cards (total, Scope 1, Scope 2, Scope 3) with change vs previous year. Stacked bar or area chart of monthly emissions by scope. Donut by category. Horizontal bar by facility. |
| P0 | `/activities` | Activities | `GET/POST/DELETE /activities`, `GET /facilities` | Filters (facility, category, year). Paged table. "Add record" modal: facility, category, quantity with unit, year, month. Show calculated CO2e and the factor used in the table. |
| P0 | `/facilities` | Facilities | `/facilities` CRUD | Card grid or table, add and edit modal, delete confirm |
| P0 | `/targets` | Targets | `GET/POST/DELETE /targets` | Card per target: progress bar from `achievedPct`, status badge, baseline vs current vs target in tonnes. Add target modal. |
| P0 | `/incentives` | Incentives | `/incentives/status`, `/incentives/rules` | Current tier badge, reduction percent, credit points, benefit text, progress to `nextTier`, table of all tiers. Show a visible "Simulated scheme" label. |
| P0 | `/insights` | AI Insights | `/ai/forecast`, `/ai/anomalies`, `POST /ai/assistant` | Line chart: history as a solid line, forecast as a dashed line with a shaded band from `lowerKg` to `upperKg`. Anomalies table with severity badge. Assistant chat box with 3 suggested questions. Show whether the answer came from `LLM` or `RULES`. |
| P1 | `/activities` | CSV upload | `POST /activities/upload` | Upload button, result summary, row errors, downloadable sample CSV |
| P1 | `/factors` | Emission factors | `GET /factors` | Read-only table with source and year |
| P1 | `/actions` | Reduction actions | `/actions` | Simple list and add form |

## Design direction

Clean enterprise dashboard. Left sidebar, top bar with company name, user, and logout. White cards on a light grey background, one green accent, a red and amber pair for status. Scope colours must stay the same in every chart. It must work on a laptop screen first, then be usable on a phone (sidebar collapses).

## Day plan

**Day 1**
1. Scaffold, Tailwind, router, layout with sidebar, protected routes.
2. API client, types, AuthContext, mock mode with data for every P0 endpoint.
3. Login and Register pages.
4. Dashboard page on mock data with all four charts.
5. Give Manit a working `npm run build` so he can deploy it. Target: the login page is on the live link by end of Day 1.

**Day 2**
1. Facilities, Activities, Targets, Incentives pages on mocks.
2. Midday: turn mocks off and connect to Guransh's real endpoints. Fix shape mismatches together, using A8 as the judge.
3. AI Insights page, connected to Manit's endpoints.

**Day 3**
1. P1 items if time allows.
2. Empty states, error toasts, responsive fixes, favicon, page titles.
3. Run the full demo flow on the live link with the demo account. Take screenshots for the README.

## Handoffs

- **To Manit:** the build command is `npm run build` and the output folder is `frontend/dist`. Tell him about any new env variable.
- **From Guransh:** Swagger UI at `/swagger-ui.html` to test real responses.
- **If the API differs from A8:** report it. Do not silently adapt the frontend. The contract decides who changes.

## Done means

- Every P0 page works on the live link with the demo account.
- No hardcoded URLs, no console errors, no secrets in the repo.
- Refreshing any route works (Manit configures the Nginx fallback to `index.html`).
- Logging out and logging in as the analyst hides the admin controls.

---

# APPENDIX: SHARED CONTRACT

This appendix is identical in all three team files (Saneh, Guransh, Manit). It is the single source of truth. Do not change field names, routes, or enums on your own. If a change is needed, tell the team, then update all three files.

## A1. Product

**CarbonTrack** is a carbon emission tracking platform for Indian companies. A company logs monthly activity data per facility (electricity, fuel, travel, freight, waste). The system converts it to CO2e using emission factors, shows dashboards, tracks reduction targets, awards a simulated green incentive tier, and adds AI features (forecast, anomaly detection, assistant).

It follows the GHG Protocol: Scope 1 (direct fuel use), Scope 2 (purchased electricity), Scope 3 (travel, freight, waste).

The incentive scheme is **simulated** for the project. It is not a real government scheme, and the UI must label it "simulated".

## A2. Scope for this build (2 to 3 days)

**P0, must ship**
- Register, login, JWT auth, each company sees only its own data
- Facilities CRUD
- Activity records: add, list, filter, delete, with CO2e calculated on save
- Dashboard: KPI cards, monthly trend by scope, split by category, split by facility
- Targets with progress and status
- Incentive tier status
- AI: forecast, anomalies, assistant
- Seeded demo company with 3+ years of data
- Deployed on one public link, with CI/CD

**P1, if time allows**
- CSV upload of activity records
- Emission factors page (read only)
- Reduction actions log
- HTTPS with a domain

**P2, stretch**
- PDF report, simulated IoT meter feed

**Out of scope:** Kafka, microservices, audit history (Envers), multi-role admin screens, Testcontainers, third-party emission APIs.

## A3. Stack

| Part | Choice |
|---|---|
| Backend | Java 21, Spring Boot 3, Maven, Spring Web, Spring Security (JWT, stateless), Spring Data JPA, Validation, Flyway, Lombok, springdoc-openapi |
| Database | PostgreSQL 16 |
| AI | Apache Commons Math (regression), plain Java statistics, one LLM call over HTTP |
| Frontend | React 18, Vite, TypeScript, Tailwind CSS, React Router, Axios, Recharts |
| Deploy | Docker, Docker Compose, Nginx, AWS EC2, GitHub Actions, GHCR |

Ports: backend 8080, frontend dev 5173, Postgres 5432.

## A4. Repo layout and ownership

One monorepo.

```
carbontrack/
├─ backend/                         Spring Boot app
│  └─ src/main/java/com/carbontrack/
│     ├─ auth/ company/ facility/   Guransh
│     ├─ factor/ activity/          Guransh
│     ├─ calculation/ dashboard/    Guransh
│     ├─ target/ incentive/         Guransh
│     ├─ common/                    Guransh (config, security, errors)
│     ├─ ai/                        Manit
│     └─ seed/                      Manit
│  └─ src/main/resources/db/migration/
│     ├─ V1__schema.sql             Guransh
│     └─ V2__seed_reference.sql     Manit (factors + incentive rules)
├─ frontend/                        Saneh
├─ deploy/                          Manit (nginx.conf, docker-compose.prod.yml)
├─ docker-compose.yml               Manit (local dev: Postgres)
├─ .github/workflows/               Manit
└─ docs/                            these three files
```

## A5. Data model

All ids are `BIGINT` identity. Column names are snake_case. JSON is camelCase.

| Table | Columns |
|---|---|
| `companies` | id, name, industry, baseline_year, created_at |
| `users` | id, company_id, name, email (unique), password_hash, role, created_at |
| `facilities` | id, company_id, name, city, state, type, created_at |
| `emission_factors` | id, category, scope, unit, value_kg_co2e_per_unit NUMERIC(14,6), source, region, year, UNIQUE(category, region, year) |
| `activity_records` | id, company_id, facility_id, category, scope, quantity NUMERIC(16,3), unit, year, month, factor_id, co2e_kg NUMERIC(18,3), source, created_by, created_at |
| `targets` | id, company_id, name, baseline_year, target_year, reduction_pct NUMERIC(5,2), created_at |
| `incentive_rules` | id, tier, name, min_reduction_pct NUMERIC(5,2), credit_points, benefit |
| `reduction_actions` (P1) | id, company_id, facility_id, title, category, expected_saving_kg, start_date, status |

## A6. Enums

Enums are stored as strings.

- `Role`: `ADMIN`, `ANALYST`
- `Scope`: `SCOPE_1`, `SCOPE_2`, `SCOPE_3`
- `FacilityType`: `OFFICE`, `PLANT`, `WAREHOUSE`, `DATA_CENTER`
- `RecordSource`: `MANUAL`, `CSV`, `SEED`
- `TargetStatus`: `ACHIEVED`, `ON_TRACK`, `AT_RISK`
- `Tier`: `NONE`, `BRONZE`, `SILVER`, `GOLD`
- `Category`, with a fixed unit and scope each:

| Category | Unit | Scope |
|---|---|---|
| `ELECTRICITY` | `kWh` | SCOPE_2 |
| `DIESEL` | `litre` | SCOPE_1 |
| `PETROL` | `litre` | SCOPE_1 |
| `NATURAL_GAS` | `m3` | SCOPE_1 |
| `LPG` | `kg` | SCOPE_1 |
| `BUSINESS_TRAVEL_AIR` | `passenger_km` | SCOPE_3 |
| `BUSINESS_TRAVEL_ROAD` | `km` | SCOPE_3 |
| `FREIGHT_ROAD` | `tonne_km` | SCOPE_3 |
| `WASTE_LANDFILL` | `tonne` | SCOPE_3 |

The client never sends unit or scope. The backend derives them from the category.

## A7. Domain rules

1. **Calculation:** `co2eKg = quantity × factor.valueKgCo2ePerUnit`. All emissions are stored and returned in **kg**. The frontend converts to tonnes for display.
2. **Factor selection:** for a category and activity year, pick the factor with the greatest `year <= activityYear`. Prefer region `IN`, else `GLOBAL`. If none is at or before that year, use the earliest available.
3. **Tenancy:** every query is filtered by the `companyId` taken from the JWT. Never accept `companyId` from the client.
4. **Roles:** `ADMIN` can do everything. `ANALYST` can read everything and create activity records. Only `ADMIN` can change facilities and targets, or delete records.
5. **Reporting year:** endpoints with a `year` parameter default to the last full calendar year (current year minus 1).
6. **Target maths:**
   - `baselineKg` = company total in `baselineYear`
   - `targetKg` = `baselineKg × (1 − reductionPct / 100)`
   - `currentKg` = company total in the reporting year
   - `achievedPct` = `(baselineKg − currentKg) / (baselineKg − targetKg) × 100`, clamped to 0 to 100
   - `ACHIEVED` if `currentKg <= targetKg`. `ON_TRACK` if the actual reduction is at least the straight-line expected reduction for that year. Otherwise `AT_RISK`.
7. **Incentive maths:** `reductionPct = (baselineKg − currentKg) / baselineKg × 100`, using `company.baselineYear`. The tier is the highest rule whose `minReductionPct` is met, else `NONE`.

## A8. API contract

Base path `/api`. JSON, camelCase. Auth header: `Authorization: Bearer <jwt>`. Everything needs auth except `/api/auth/register`, `/api/auth/login`, `/api/health`.

**Error body (all errors):**
```json
{ "timestamp": "2026-11-11T10:00:00Z", "status": 400, "error": "Bad Request", "message": "quantity must be positive", "path": "/api/activities" }
```
Use 400 for validation, 401 for missing or bad token, 403 for role, 404 for not found or another company's data, 409 for duplicate email.

**Paged list shape:** `{ "content": [], "page": 0, "size": 20, "totalElements": 0, "totalPages": 0 }`. `page` is zero-based.

### Health
| Method | Route | Response |
|---|---|---|
| GET | `/api/health` | `{ "status": "UP" }` |

### Auth
| Method | Route | Body | Response |
|---|---|---|---|
| POST | `/api/auth/register` | `{ companyName, industry, name, email, password }` | `AuthResponse` (creates company + ADMIN user) |
| POST | `/api/auth/login` | `{ email, password }` | `AuthResponse` |
| GET | `/api/auth/me` | none | `User` |

`AuthResponse`: `{ token, user }`
`User`: `{ id, name, email, role, companyId, companyName, baselineYear }`

### Facilities
| Method | Route | Body | Response |
|---|---|---|---|
| GET | `/api/facilities` | none | `Facility[]` |
| POST | `/api/facilities` | `{ name, city, state, type }` | `Facility` |
| PUT | `/api/facilities/{id}` | `{ name, city, state, type }` | `Facility` |
| DELETE | `/api/facilities/{id}` | none | 204 |

`Facility`: `{ id, name, city, state, type }`

### Emission factors
| Method | Route | Response |
|---|---|---|
| GET | `/api/factors?year=` | `Factor[]` (year optional) |

`Factor`: `{ id, category, scope, unit, valueKgCo2ePerUnit, source, region, year }`

### Activity records
| Method | Route | Body | Response |
|---|---|---|---|
| GET | `/api/activities?facilityId=&category=&year=&page=&size=` | none | paged `Activity` (all filters optional, newest period first) |
| POST | `/api/activities` | `{ facilityId, category, quantity, year, month }` | `Activity` |
| PUT | `/api/activities/{id}` | same as POST | `Activity` (recalculated) |
| DELETE | `/api/activities/{id}` | none | 204 |
| POST | `/api/activities/upload` (P1) | multipart `file` (CSV) | `{ imported, failed, errors: [{ row, message }] }` |

`Activity`: `{ id, facilityId, facilityName, category, scope, quantity, unit, year, month, co2eKg, factorId, factorValue, factorSource, source, createdAt }`

CSV header: `facilityName,category,quantity,year,month`

### Dashboard
| Method | Route | Response |
|---|---|---|
| GET | `/api/dashboard/years` | `[2023, 2024, 2025, 2026]` (years that have data, ascending) |
| GET | `/api/dashboard/summary?year=` | `{ year, totalCo2eKg, scope1Kg, scope2Kg, scope3Kg, changeVsPrevYearPct, topCategory, topFacility }` |
| GET | `/api/dashboard/monthly?year=&facilityId=` | `[{ year, month, scope1Kg, scope2Kg, scope3Kg, totalKg }]` (12 rows, zero-filled) |
| GET | `/api/dashboard/by-category?year=` | `[{ category, scope, co2eKg, pct }]` |
| GET | `/api/dashboard/by-facility?year=` | `[{ facilityId, facilityName, co2eKg, pct }]` |

`changeVsPrevYearPct` is null when there is no previous year.

### Targets
| Method | Route | Body | Response |
|---|---|---|---|
| GET | `/api/targets` | none | `Target[]` |
| POST | `/api/targets` | `{ name, baselineYear, targetYear, reductionPct }` | `Target` |
| DELETE | `/api/targets/{id}` | none | 204 |

`Target`: `{ id, name, baselineYear, targetYear, reductionPct, baselineKg, targetKg, currentYear, currentKg, achievedPct, status }`

### Incentives
| Method | Route | Response |
|---|---|---|
| GET | `/api/incentives/rules` | `[{ id, tier, name, minReductionPct, creditPoints, benefit }]` |
| GET | `/api/incentives/status?year=` | `{ year, baselineYear, baselineKg, currentKg, reductionPct, tier, creditPoints, benefit, nextTier }` |

`nextTier`: `{ tier, minReductionPct, gapPct }` or null when already at the top tier.

### AI
| Method | Route | Body | Response |
|---|---|---|---|
| GET | `/api/ai/forecast?months=6` | none | `{ method, history: [{ year, month, totalKg }], forecast: [{ year, month, totalKg, lowerKg, upperKg }] }` |
| GET | `/api/ai/anomalies?year=` | none | `[{ activityId, facilityId, facilityName, category, year, month, quantity, unit, expectedQuantity, zScore, severity }]` |
| POST | `/api/ai/assistant` | `{ question }` | `{ answer, source }` |

`severity` is `MEDIUM` or `HIGH`. Assistant `source` is `LLM` or `RULES`. `answer` is plain text or simple markdown.

### Reduction actions (P1)
| Method | Route | Body | Response |
|---|---|---|---|
| GET | `/api/actions` | none | `Action[]` |
| POST | `/api/actions` | `{ title, facilityId, category, expectedSavingKg, startDate, status }` | `Action` |

`status` is `PLANNED`, `IN_PROGRESS`, or `DONE`.

## A9. Demo data

- Demo company: **GreenSteel Industries Ltd** (fictional), industry `Manufacturing`, `baselineYear` 2023.
- Facilities: Pune Plant (PLANT), Ludhiana Plant (PLANT), Gurugram HQ (OFFICE), Chennai Warehouse (WAREHOUSE).
- Data: every month from Jan 2023 to Oct 2026, for every sensible facility and category pair.
- Shape: summer peak in electricity, about 12% total reduction from 2023 to 2025, three injected spikes in 2025 for anomaly detection.
- Demo target: "20% by 2027" from baseline 2023. Expected result: `ON_TRACK`, incentive tier `SILVER`.
- Logins: `demo@carbontrack.in` / `Demo@1234` (ADMIN), `analyst@carbontrack.in` / `Demo@1234` (ANALYST).
- Incentive rules: BRONZE at 5% (100 points), SILVER at 10% (250 points), GOLD at 20% (500 points).

## A10. Environment variables

| Variable | Used by | Example |
|---|---|---|
| `DB_URL` | backend | `jdbc:postgresql://db:5432/carbontrack` |
| `DB_USERNAME`, `DB_PASSWORD` | backend, db | `carbontrack` / secret |
| `JWT_SECRET` | backend | 64+ random characters |
| `JWT_EXPIRY_MINUTES` | backend | `1440` |
| `APP_SEED_ENABLED` | backend | `true` |
| `CORS_ALLOWED_ORIGINS` | backend (dev only) | `http://localhost:5173` |
| `LLM_BASE_URL`, `LLM_API_KEY`, `LLM_MODEL` | backend | any OpenAI-compatible chat endpoint; blank means rule-based answers |
| `VITE_API_BASE_URL` | frontend | `/api` |
| `VITE_USE_MOCKS` | frontend | `true` in early development |

In production the frontend and backend share one origin. Nginx serves the React build and proxies `/api` to the backend, so CORS is not needed there. Never commit secrets.

## A11. Git workflow

- `main` is always deployable. Every push to `main` deploys.
- Branches: `fe/<feature>`, `be/<feature>`, `ops/<feature>`. Open small pull requests and merge several times a day.
- Stay inside the folders you own. If you need a change elsewhere, ask the owner.
- CI must pass before merge.

## A12. Timeline and checkpoints

| When | Checkpoint |
|---|---|
| Day 1, first 2 hours | Repo created, contract read by all three, skeletons pushed |
| Day 1, end | Live link works end to end: login page loads, login succeeds against the deployed backend. V1 schema, auth, and facilities are merged. |
| Day 2, midday | Activities, calculation, and dashboard endpoints are live. Frontend switches from mocks to the real API. Seed data is live. |
| Day 2, end | All P0 features work on the live link. |
| Day 3, midday | Feature freeze. Only bug fixes after this. |
| Day 3, end | Demo script rehearsed on the live link. README, screenshots, and Swagger link are ready. |

If you only have 2 days, drop all P1 items and move the freeze to Day 2 evening.
