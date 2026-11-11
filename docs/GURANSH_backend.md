# CarbonTrack: Backend Brief (Guransh)

**For the AI assistant reading this:** you are helping **Guransh**, who owns the backend core of a 3-person project that must be built and deployed in 2 to 3 days. Read the appendix "SHARED CONTRACT" first. It defines the API, data model, enums, and rules, and you must follow it exactly. Do not rename routes or fields. Write working, conventional Spring Boot code. Prefer finishing P0 over adding layers.

## Team

| Person | Owns |
|---|---|
| Saneh | `frontend/`: all UI, built against the contract in A8 |
| **Guransh (you)** | Backend core: project skeleton, schema, security, auth, facilities, factors, activities, calculation, dashboard, targets, incentives, API docs, frontend integration |
| Manit | Deployment, CI/CD, `V2__seed_reference.sql`, the `seed/` and `ai/` packages |

Two people depend on you early. Saneh needs real endpoints by Day 2 midday. Manit needs a bootable app on Day 1 to deploy, and needs the entities, repositories, and `EmissionCalculator` to build the seeder and AI package.

## Setup

Generate from start.spring.io: Maven, Java 21, Spring Boot 3, group `com.carbontrack`, artifact `backend`. Dependencies: Web, Security, Data JPA, Validation, PostgreSQL Driver, Flyway, Lombok. Add by hand: `jjwt` (api, impl, jackson), `springdoc-openapi-starter-webmvc-ui`, `commons-csv` (P1), `flyway-database-postgresql`.

`application.yml` reads every setting from the env variables in A10, with local defaults that match the root `docker-compose.yml` Postgres. Set `spring.jpa.hibernate.ddl-auto=validate`. Flyway owns the schema.

## Package layout (yours)

```
com.carbontrack
├─ common/       SecurityConfig, JwtService, JwtAuthFilter, CurrentUser, GlobalExceptionHandler,
│                ApiError, OpenApiConfig, enums (Role, Scope, Category, ...)
├─ auth/         AuthController, AuthService, dto
├─ company/      Company, CompanyRepository
├─ facility/     Facility, repository, service, controller, dto
├─ factor/       EmissionFactor, repository, FactorService, controller
├─ calculation/  EmissionCalculator (interface), FactorBasedEmissionCalculator, CalculationResult
├─ activity/     ActivityRecord, repository, service, controller, dto, CsvImportService (P1)
├─ dashboard/    DashboardService, DashboardController, projections
├─ target/       Target, repository, service, controller, dto
└─ incentive/    IncentiveRule, repository, IncentiveService, controller, dto
```

Use the layering controller, service, repository. Use DTO records for requests and responses. Never return entities from controllers.

## Build order

### 1. Skeleton and schema (Day 1, first)
- `GET /api/health` returns `{ "status": "UP" }`, public. Push this first so Manit can deploy it.
- `V1__schema.sql` creates every table in A5, with foreign keys and these indexes: `activity_records(company_id, year, month)`, `activity_records(facility_id)`, `users(email)` unique, and the unique key on `emission_factors`.
- Put `Category` in `common` with its unit and scope as enum fields, matching A6.

### 2. Security and auth (Day 1)
- Stateless, CSRF off, BCrypt, JWT with claims `sub` (user id), `companyId`, `role`.
- Public: `/api/auth/register`, `/api/auth/login`, `/api/health`, `/swagger-ui/**`, `/v3/api-docs/**`. Everything else needs a valid token.
- `CurrentUser` helper returns `userId`, `companyId`, `role` from the security context. Every service method takes `companyId` from it, never from the request.
- Register creates the company and an `ADMIN` user in one transaction. `baselineYear` starts null. Duplicate email returns 409.
- CORS: allow `CORS_ALLOWED_ORIGINS` when it is set (local dev only).
- `GlobalExceptionHandler` returns the error body in A8 for validation errors, not found, access denied, and bad credentials.

### 3. Facilities and factors (Day 1)
- Facilities CRUD, scoped by company. Loading another company's facility returns 404. Writes need `ADMIN`.
- `GET /api/factors?year=` is read only.

### 4. Calculation engine (Day 1 end or Day 2 start)

```java
public interface EmissionCalculator {
    CalculationResult calculate(Category category, BigDecimal quantity, int year);
}
public record CalculationResult(EmissionFactor factor, BigDecimal co2eKg) {}
```

- `FactorBasedEmissionCalculator` implements rules 1 and 2 in A7. Throw a clear 400 error if no factor exists for a category.
- Use `BigDecimal`, scale 3, `HALF_UP`.
- **Manit's seeder calls this interface. Do not change its signature without telling him.**

### 5. Activities (Day 2 morning)
- Create: validate `quantity > 0`, `month` 1 to 12, `year` 2000 to current year, facility belongs to the company. Derive unit and scope from the category, call the calculator, store `factor_id` and `co2e_kg`, set `source = MANUAL`.
- Update recalculates. Delete needs `ADMIN`.
- List: optional filters, paged, sorted by year desc, month desc, id desc. Return the paged shape in A8.
- If `company.baselineYear` is null when a record is saved, set it to the earliest year that has data.

### 6. Dashboard (Day 2 morning)
- Do the aggregation in JPQL or native SQL with `SUM` and `GROUP BY`. Do not load all records into memory.
- `monthly` always returns 12 rows, zero-filled.
- `pct` is the share of the year total, one decimal place.
- Expose a reusable service method `monthlyTotals(companyId)` that returns all months with data in order. Manit's forecast uses it.

### 7. Targets and incentives (Day 2 afternoon)
- Implement rules 5, 6, and 7 in A7 exactly. Compute target fields on read. Do not store them.
- If the baseline total is zero, return zeros and `AT_RISK` for targets, and `NONE` for the tier. Never divide by zero.
- `nextTier.gapPct` = `minReductionPct − reductionPct`, floored at 0.

### 8. P1
- CSV upload with `commons-csv`: header from A8, match facility by name within the company, collect row errors, save valid rows with `source = CSV`.
- Reduction actions CRUD.

## Quality bar

- `@Valid` on all request bodies. Method security with `@PreAuthorize("hasRole('ADMIN')")` on restricted writes.
- Unit tests with JUnit 5 and Mockito for the three pieces with real logic: `FactorBasedEmissionCalculator` (factor selection by year and region), the target status calculation, and the incentive tier calculation. Six to ten tests in total is enough.
- Swagger UI works at `/swagger-ui.html` with a bearer auth button.
- `mvn -B verify` passes. CI runs it on every pull request.

## Day plan

**Day 1:** health endpoint pushed in the first hour. Then V1 schema, security, auth, facilities, factors. End of day: login works on the live link.

**Day 2:** calculator, activities, dashboard by midday. Help Saneh switch from mocks to the real API. Targets and incentives in the afternoon.

**Day 3:** P1 items, tests, bug fixes from the integration run. Feature freeze at midday.

## Handoffs

- **To Manit, Day 1:** tell him as soon as V1 is merged (he writes V2 against it) and as soon as `EmissionCalculator` and the repositories are merged (his seeder and AI package need them).
- **To Saneh:** share the Swagger link and a note whenever an endpoint goes live. If you must deviate from A8, say so before merging.
- **From Manit:** `docker-compose.yml` at the repo root starts local Postgres. He will add `ai/` and `seed/` packages inside your module, so keep `common` stable.

## Done means

- Every P0 endpoint in A8 returns exactly the documented shape.
- A user from one company cannot read or change another company's data.
- With the seed data, the demo company shows a 2023 to 2025 reduction, target status `ON_TRACK`, and tier `SILVER`.

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
