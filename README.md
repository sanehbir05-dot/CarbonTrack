# CarbonTrack

Enterprise platform where Indian companies log activity data (electricity, fuel, travel, logistics), convert it to CO2e under the GHG Protocol (Scope 1, 2, 3), track reduction targets, check green-incentive eligibility, and get AI forecasts, anomaly flags and reduction suggestions. Reports are BRSR-style.

## Repo layout

```
frontend/   React + Vite UI prototype (mock data only)
backend/    Spring Boot 3 / Java 21 modular monolith (not started yet)
docs/       Project docs, including backend context and remaining work
```

## Status

- **Frontend:** 8-screen prototype. All numbers come from placeholders in `frontend/src/data.js`; there are no API calls yet.
- **Backend:** not started. See `docs/CarbonTrack_Backend_Context.pdf` for modules, proposed API, remaining work and build order.

## Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173.

## Data sources (for the backend)

- CEA CO2 Baseline Database v20 (India grid, Scope 2)
- UK DESNZ GHG conversion factors (fuels, travel, freight, waste)
- US EPA GHG Emission Factors Hub (backup)
- Climatiq API (live lookups, free tier 250 calls/month)
