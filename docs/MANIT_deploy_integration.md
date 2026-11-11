# CarbonTrack: Deployment and Integration Brief (Manit)

**For the AI assistant reading this:** you are helping **Manit**, who owns deployment, CI/CD, seed data, the AI endpoints, and integration checks for a 3-person project that must be built and deployed in 2 to 3 days. Read the appendix "SHARED CONTRACT" first. It defines the API, data model, enums, and rules, and you must follow it exactly. Prefer the simplest setup that gives one stable public link.

## Team

| Person | Owns |
|---|---|
| Saneh | `frontend/` (React, Vite). Build command `npm run build`, output `frontend/dist`. |
| Guransh | Backend core (Spring Boot): schema V1, security, auth, facilities, activities, calculation, dashboard, targets, incentives |
| **Manit (you)** | Docker, Nginx, EC2, GitHub Actions, `V2__seed_reference.sql`, backend packages `seed/` and `ai/`, smoke tests, the live link |

**Your first priority is the live link.** A hello-world version must be deployed on Day 1, before the features exist. Everything else comes after that.

## Part 1: Deployment

### Target architecture

One EC2 instance, one Docker Compose stack, one origin.

```
Browser ──► Nginx :80 ──► /            React static build
                     └──► /api/*       backend:8080 (Spring Boot)
                                         └──► db:5432 (Postgres, internal only)
```

Same origin means no CORS problems and no mixed-content problems.

### Files you create

| File | Purpose |
|---|---|
| `docker-compose.yml` (root) | Local dev: Postgres 16 only, port 5432, named volume |
| `backend/Dockerfile` | Multi-stage: Maven + JDK 21 build, then a JRE 21 runtime image, non-root user |
| `frontend/Dockerfile` | Multi-stage: Node build, then Nginx serving `dist` with `deploy/nginx.conf` |
| `deploy/nginx.conf` | Serve static files, `try_files $uri /index.html` for React routes, proxy `/api/`, `/swagger-ui/`, and `/v3/api-docs` to `backend:8080`, gzip on, `client_max_body_size 10m` |
| `deploy/docker-compose.prod.yml` | Services `db`, `backend`, `web`. Only `web` publishes a port (80). Healthcheck on `db`. `backend` waits for `db`. `restart: unless-stopped`. Images come from the registry. |
| `deploy/.env.example` | Every variable in A10, with fake values |
| `.github/workflows/ci.yml` | On pull request: `mvn -B verify` in `backend`, `npm ci && npm run build` in `frontend` |
| `.github/workflows/deploy.yml` | On push to `main`: build both images, push to GHCR, SSH to EC2, `docker compose pull && docker compose up -d`, then run the smoke test |
| `deploy/smoke.sh` | curl checks, see Part 4 |

### EC2 setup

1. Launch Ubuntu LTS. Use an instance with 2 GB RAM if you can. On a 1 GB instance, add a 2 GB swap file and set `JAVA_TOOL_OPTIONS=-Xmx384m` on the backend.
2. Security group: 22 from your IPs only, 80 from anywhere, 443 if you add HTTPS. Do not open 5432 or 8080.
3. Attach an Elastic IP so the link does not change.
4. Install Docker and the Compose plugin. Create `/opt/carbontrack` with `docker-compose.prod.yml` and a real `.env` (never committed).
5. Build images in GitHub Actions, not on the server. The server only pulls.
6. Log the server in to GHCR with a read-only token, or make the two packages public.

### GitHub secrets

`EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY`. Application secrets (`DB_PASSWORD`, `JWT_SECRET`, `LLM_API_KEY`) live only in the server `.env`.

### HTTPS (P1)

Point a domain or free subdomain at the Elastic IP, then add Certbot. Do this only after all P0 features are live.

## Part 2: Reference and demo data

### `V2__seed_reference.sql`

Inserts emission factors and incentive rules. Write it after Guransh merges `V1__schema.sql`, using the columns in A5.

**Emission factors.** The values below are approximate placeholders. Before the demo, replace each with the exact value from the source file, and put the source edition in the `source` column.

| Category | Unit | Approx. kg CO2e per unit | Region | Source to verify against |
|---|---|---|---|---|
| ELECTRICITY | kWh | 0.716 (2023), 0.727 (2024 onward) | IN | CEA CO2 Baseline Database, user guide Section 5 (grid average, CO2 only, published per fiscal year) |
| DIESEL | litre | 2.66 | GLOBAL | UK DESNZ conversion factors, Fuels tab |
| PETROL | litre | 2.10 | GLOBAL | UK DESNZ, Fuels tab |
| NATURAL_GAS | m3 | 2.04 | GLOBAL | UK DESNZ, Fuels tab |
| LPG | kg | 2.94 | GLOBAL | UK DESNZ, Fuels tab |
| BUSINESS_TRAVEL_AIR | passenger_km | 0.15 | GLOBAL | UK DESNZ, Business travel (air) tab. Varies a lot by haul and class. |
| BUSINESS_TRAVEL_ROAD | km | 0.17 | GLOBAL | UK DESNZ, Business travel (land) tab, average car |
| FREIGHT_ROAD | tonne_km | 0.11 | GLOBAL | UK DESNZ, Freighting goods tab, average HGV |
| WASTE_LANDFILL | tonne | 450 | GLOBAL | UK DESNZ, Waste disposal tab. Varies a lot by waste type. |

Insert one row per category for year 2023 at least, and a 2024 row for electricity. The factor selection rule in A7 covers later years.

Sources:
- CEA user guide: https://cea.nic.in/wp-content/uploads/2021/03/User_Guide_Version_20.0.pdf
- UK DESNZ: https://www.gov.uk/government/collections/government-conversion-factors-for-company-reporting

**Incentive rules:** the three tiers in A9, each with a short simulated `benefit` text.

### `seed/DemoDataSeeder`

- A `CommandLineRunner` that runs only when `app.seed.enabled=true` and the demo company does not exist. It must be safe to restart.
- Creates the company, both users (BCrypt passwords), four facilities, the demo target, and the activity records described in A9.
- For each record, call Guransh's `EmissionCalculator.calculate(category, quantity, year)` and store the factor and `co2eKg`. Set `source = SEED`. Save in batches.
- Generate quantities as `base(facility, category) × seasonality(month) × yearTrend(year) × noise`. Use a fixed random seed so every deploy produces the same data. Noise of about ±6%.
- Seasonality: electricity peaks April to July. Year trend: about 1.00, 0.94, 0.88, 0.85 for 2023 to 2026.
- Inject exactly three spikes in 2025 (for example Pune Plant electricity at 2.5 times normal in one month). Keep the 2025 total about 12% below 2023 after the spikes.
- Facility and category pairs must make sense: plants use electricity, diesel, natural gas, LPG, freight, and waste. The office uses electricity, air and road travel, and waste. The warehouse uses electricity, diesel, and freight.
- After seeding, check on the live link that the target shows `ON_TRACK` and the tier shows `SILVER`. Tune the trend numbers if not.

**P1, real-company anchoring:** take Scope 1 and Scope 2 totals from one listed company's BRSR report, put them in a small config file, and scale the generated data so yearly totals match. Present it as "mock activity data scaled to publicly disclosed totals". Never present it as the company's real activity data.

## Part 3: AI package (`ai/`)

Endpoints and shapes are in A8. Read data through Guransh's repositories and his `monthlyTotals(companyId)` service method. Always filter by the `companyId` from `CurrentUser`.

### Forecast: `GET /api/ai/forecast?months=6`
- Input: monthly company totals, complete months only.
- Model: linear trend with `SimpleRegression` from Apache Commons Math on the month index, multiplied by a seasonal index (average ratio of actual to trend for each calendar month).
- `lowerKg` and `upperKg` = forecast ± 1.96 × residual standard deviation, floored at 0.
- `method` = `LINEAR_TREND_SEASONAL`. `months` is clamped to 1 to 12.
- With fewer than 12 months of history, return 400 with a clear message.

### Anomalies: `GET /api/ai/anomalies?year=`
- Group records by facility and category. Skip groups with fewer than 6 points.
- `zScore = (quantity − mean) / stdDev`. Flag when the absolute value is 2.5 or more. `HIGH` at 3.5 or more, else `MEDIUM`.
- `expectedQuantity` = the group mean. Return only records in the requested year, sorted by absolute z-score, highest first.

### Assistant: `POST /api/ai/assistant`
- Build a short context from the company's own data: yearly totals, scope split, top 3 categories, top facility, target status, incentive tier, current anomalies.
- `LlmClient` interface with one implementation that calls an OpenAI-compatible chat completions endpoint, configured by `LLM_BASE_URL`, `LLM_API_KEY`, `LLM_MODEL`. Timeout of 15 seconds.
- System prompt: answer only from the given context, give practical reduction steps, say when the data is not enough, keep it under 150 words.
- **Fallback:** if the key is blank or the call fails, return a rule-based answer built from the same context (top category, biggest facility, gap to the next tier, any anomalies), with `source = RULES`. The demo must work without an API key.
- Limit `question` to 500 characters. Never log the API key.

## Part 4: Integration checks

`deploy/smoke.sh` takes the base URL and fails on the first error:

1. `GET /api/health` returns `UP`.
2. `POST /api/auth/login` with the demo account returns a token.
3. With the token: `/api/facilities` has 4 items, `/api/dashboard/summary?year=2025` has a total above 0, `/api/targets` is not empty, `/api/incentives/status` returns a tier, `/api/ai/forecast` returns 6 forecast rows.
4. `GET /` returns 200 and contains the app root element.
5. A request without a token to `/api/facilities` returns 401.

Run it at the end of `deploy.yml`. You also own the integration run at each checkpoint in A12: open the live link, walk through the demo flow, and report mismatches against A8 to the owner.

## Day plan

**Day 1**
1. Create the repo, branch rules, root `docker-compose.yml`, `.env.example`.
2. EC2, Elastic IP, Docker, security group.
3. Dockerfiles, Nginx config, prod compose, `ci.yml`, `deploy.yml`.
4. Deploy Guransh's health endpoint and Saneh's first build. **End of day: the live link serves the login page and `/api/health`.**
5. Write `V2__seed_reference.sql` once V1 is merged.

**Day 2**
1. `DemoDataSeeder` as soon as `EmissionCalculator` and the repositories are merged. Seed data live by midday.
2. Forecast and anomalies endpoints.
3. Assistant endpoint with the rule-based fallback first, then the LLM call.
4. Smoke script in the pipeline.

**Day 3**
1. Verify the factor values against the sources and update V2 (use a new migration `V3` if V2 is already deployed, because Flyway rejects edited migrations).
2. HTTPS if time allows.
3. README: architecture diagram, live link, demo logins, how to run locally, data sources.
4. Final integration run and demo rehearsal.

## Risks to watch

- **Flyway checksum errors:** never edit a migration that has already run on the server. Add a new one.
- **Low memory on EC2:** check `docker stats`. Add swap before the backend gets killed.
- **Disk filling up:** run `docker image prune -f` at the end of each deploy.
- **Lost data:** keep the Postgres named volume. Never run `docker compose down -v` on the server.
- **Secrets:** nothing in the repo, nothing in image layers, nothing in logs.

## Done means

- One stable public link, redeployed automatically on every merge to `main`.
- The smoke test passes after each deploy.
- A fresh database comes up with factors, incentive rules, and the demo company, with no manual steps.
- All three AI endpoints return the shapes in A8, and the assistant works with and without an API key.

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
