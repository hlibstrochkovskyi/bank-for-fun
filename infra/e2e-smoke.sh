#!/usr/bin/env bash
#
# End-to-end smoke test through the WHOLE stack, the way a browser does it:
# scripted Keycloak OIDC login against the Next.js frontend, then every money
# operation through the frontend's BFF proxy (/api/bank/*) into core-bank.
#
# Proves: OIDC login -> Auth.js session -> BFF forwards the token -> core-bank,
# plus deposit, transfer, the fraud HOLD (>= $10k), held list, and statements.
#
# Prereqs: `make up` is healthy AND the frontend dev server is running
#          (cd frontend && npm run dev).
#
# Usage:  ./infra/e2e-smoke.sh
# Env:    APP=http://localhost:3000  KC=http://localhost:8088
#         LB_USER=alice  LB_PASS=password
set -euo pipefail

APP="${APP:-http://localhost:3000}"
KC="${KC:-http://localhost:8088}"
REALM="${REALM:-ledger-bank}"
LB_USER="${LB_USER:-alice}"
LB_PASS="${LB_PASS:-password}"

JAR="$(mktemp)"
pass() { printf '  \033[32m✓\033[0m %s\n' "$1"; }
fail() { printf '  \033[31m✗ %s\033[0m\n' "$1"; exit 1; }
field() { python3 -c "import sys,json;print(json.load(sys.stdin)$1)"; }

echo "→ 1. Scripted OIDC login as '$LB_USER'"
CSRF=$(curl -s -c "$JAR" "$APP/api/auth/csrf" | field "['csrfToken']")
KCURL=$(curl -s -b "$JAR" -c "$JAR" -o /dev/null -w '%{redirect_url}' \
  -d "csrfToken=$CSRF" -d "callbackUrl=$APP/dashboard" "$APP/api/auth/signin/keycloak")
ACTION=$(curl -s -b "$JAR" -c "$JAR" "$KCURL" \
  | grep -oE 'action="[^"]*login-actions/authenticate[^"]*"' | head -1 \
  | sed 's/action="//;s/"$//;s/\&amp;/\&/g')
CB=$(curl -s -b "$JAR" -c "$JAR" -o /dev/null -w '%{redirect_url}' \
  --data-urlencode "username=$LB_USER" --data-urlencode "password=$LB_PASS" \
  --data-urlencode "credentialId=" "$ACTION")
curl -s -b "$JAR" -c "$JAR" -o /dev/null "$CB"
EMAIL=$(curl -s -b "$JAR" "$APP/api/auth/session" | field "['user']['email']")
[ -n "$EMAIL" ] && pass "logged in as $EMAIL" || fail "login failed"

# Helpers that go through the BFF (cookie auth; the token is added server-side).
bget() { curl -s -b "$JAR" "$APP/api/bank/$1"; }
bpost() { curl -s -b "$JAR" -X POST "$APP/api/bank/$1" -H 'content-type: application/json' \
  -H "idempotency-key: $(python3 -c 'import uuid;print(uuid.uuid4())')" -d "$2"; }

echo "→ 2. Open checking + savings accounts"
CHK=$(bpost "accounts" '{"type":"CHECKING","currency":"USD"}' | field "['id']")
SAV=$(bpost "accounts" '{"type":"SAVINGS","currency":"USD"}' | field "['id']")
[ -n "$CHK" ] && [ -n "$SAV" ] && pass "checking=$CHK savings=$SAV" || fail "open account failed"

echo "→ 3. Deposit \$1,000 into checking"
DEP=$(bpost "accounts/$CHK/deposits" '{"amount":"1000.00","currency":"USD"}' | field "['status']")
[ "$DEP" = "COMPLETED" ] && pass "deposit COMPLETED" || fail "deposit status=$DEP"

echo "→ 4. Transfer \$250 checking → savings (should complete)"
T1=$(bpost "transfers" "{\"fromAccountId\":\"$CHK\",\"toAccountId\":\"$SAV\",\"amount\":\"250.00\",\"currency\":\"USD\"}" | field "['status']")
[ "$T1" = "COMPLETED" ] && pass "transfer COMPLETED" || fail "transfer status=$T1"

echo "→ 5. Deposit \$20k, then transfer \$15k (should be HELD by fraud)"
bpost "accounts/$CHK/deposits" '{"amount":"20000.00","currency":"USD"}' >/dev/null
T2=$(bpost "transfers" "{\"fromAccountId\":\"$CHK\",\"toAccountId\":\"$SAV\",\"amount\":\"15000.00\",\"currency\":\"USD\"}" | field "['status']")
[ "$T2" = "HELD" ] && pass "large transfer HELD for review" || fail "expected HELD, got $T2"

echo "→ 6. Held transfers list shows it"
HELD=$(bget "held-transfers" | python3 -c "import sys,json;print(len(json.load(sys.stdin)))")
[ "$HELD" -ge 1 ] && pass "$HELD held transfer(s) visible" || fail "no held transfers"

echo "→ 7. Statement for checking (last 30 days)"
FROM=$(python3 -c "import datetime;print((datetime.date.today()-datetime.timedelta(days=30)).isoformat())")
TO=$(python3 -c "import datetime;print(datetime.date.today().isoformat())")
CLOSING=$(bget "accounts/$CHK/statement?from=$FROM&to=$TO" | field "['closingBalance']['amount']")
[ -n "$CLOSING" ] && pass "statement closing balance = \$$CLOSING" || fail "statement failed"

rm -f "$JAR"
printf '\n\033[32mE2E smoke passed.\033[0m\n'
