#!/usr/bin/env bash
# Usage: deploy/smoke.sh https://your-domain
set -euo pipefail
BASE="${1:?usage: smoke.sh <base-url>}"
BASE="${BASE%/}"

fail() { echo "FAIL: $*" >&2; exit 1; }

echo "1. health"
curl -fsS "$BASE/api/health" | grep -q '"UP"' || fail "health not UP"

echo "2. login"
TOKEN=$(curl -fsS -X POST "$BASE/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"demo@carbontrack.in","password":"Demo@1234"}' \
  | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
[ -n "$TOKEN" ] || fail "no token"
AUTH="Authorization: Bearer $TOKEN"

echo "3. authenticated endpoints"
[ "$(curl -fsS -H "$AUTH" "$BASE/api/facilities" | grep -o '"id"' | wc -l | tr -d ' ')" -eq 4 ] || fail "facilities != 4"
curl -fsS -H "$AUTH" "$BASE/api/dashboard/summary?year=2025" | grep -Eq '"totalCo2eKg":[1-9]' || fail "dashboard total is 0"
curl -fsS -H "$AUTH" "$BASE/api/targets" | grep -q '"id"' || fail "targets empty"
curl -fsS -H "$AUTH" "$BASE/api/incentives/status" | grep -q '"tier"' || fail "no tier"
[ "$(curl -fsS -H "$AUTH" "$BASE/api/ai/forecast?months=6" | grep -o '"lowerKg"' | wc -l | tr -d ' ')" -eq 6 ] || fail "forecast != 6 rows"

echo "4. frontend"
curl -fsS "$BASE/" | grep -q 'id="root"' || fail "app root missing"

echo "5. unauthenticated request rejected"
[ "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/facilities")" = "401" ] || fail "expected 401"

echo "Smoke test passed."
