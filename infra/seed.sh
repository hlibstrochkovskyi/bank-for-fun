#!/usr/bin/env bash
#
# Seed demo data through the real API, exactly as a client would: get a token from
# Keycloak, then open named accounts and post realistic deposits/withdrawals/transfers.
# Requires the stack to be up (`make up`).
#
# Idempotent: if the user already has customer accounts, it does nothing (re-run safe).
# Pass FORCE=1 to seed anyway (adds another set — useful for stress, not for demos).
#
# Usage:  ./infra/seed.sh
# Env:    BASE_URL (default http://localhost:8080), KC_URL (default http://localhost:8088),
#         USERNAME (default alice), PASSWORD (default password), FORCE (default 0)
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
KC_URL="${KC_URL:-http://localhost:8088}"
REALM="${REALM:-ledger-bank}"
CLIENT_ID="${CLIENT_ID:-ledger-bank-web}"
USERNAME="${USERNAME:-alice}"
PASSWORD="${PASSWORD:-password}"
FORCE="${FORCE:-0}"
# For the (optional) history-backdating step, applied via the Postgres container.
PG_CONTAINER="${PG_CONTAINER:-ledger-bank-postgres-1}"
DB_NAME="${DB_NAME:-ledgerbank}"
DB_USERNAME="${DB_USERNAME:-ledgerbank}"

json_field() { python3 -c "import sys,json; print(json.load(sys.stdin)['$1'])"; }
count_json() { python3 -c "import sys,json; print(len(json.load(sys.stdin)))"; }
uuid() { python3 -c "import uuid; print(uuid.uuid4())"; }

echo "→ Requesting access token for '$USERNAME' from Keycloak…"
TOKEN=$(curl -fsS -X POST \
  "$KC_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=$CLIENT_ID" -d "username=$USERNAME" -d "password=$PASSWORD" \
  -d "grant_type=password" | json_field access_token)

auth=(-H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json")

EXISTING=$(curl -fsS "${auth[@]}" "$BASE_URL/api/accounts" | count_json)
if [[ "$EXISTING" -gt 0 && "$FORCE" != "1" ]]; then
  echo "✓ '$USERNAME' already has $EXISTING account(s) — nothing to do (FORCE=1 to override)."
  exit 0
fi

# --- open a realistic, named set of accounts ----------------------------------
open_account() { # type nickname
  curl -fsS "${auth[@]}" -X POST "$BASE_URL/api/accounts" \
    -d "{\"type\":\"$1\",\"currency\":\"USD\",\"nickname\":\"$2\"}" | json_field id
}
deposit() { # account amount description
  curl -fsS "${auth[@]}" -H "Idempotency-Key: $(uuid)" -X POST \
    "$BASE_URL/api/accounts/$1/deposits" \
    -d "{\"amount\":\"$2\",\"currency\":\"USD\",\"description\":\"$3\"}" >/dev/null
}
withdraw() { # account amount description
  curl -fsS "${auth[@]}" -H "Idempotency-Key: $(uuid)" -X POST \
    "$BASE_URL/api/accounts/$1/withdrawals" \
    -d "{\"amount\":\"$2\",\"currency\":\"USD\",\"description\":\"$3\"}" >/dev/null
}
transfer() { # from to amount description
  curl -fsS "${auth[@]}" -H "Idempotency-Key: $(uuid)" -X POST \
    "$BASE_URL/api/transfers" \
    -d "{\"fromAccountId\":\"$1\",\"toAccountId\":\"$2\",\"amount\":\"$3\",\"currency\":\"USD\",\"description\":\"$4\"}" >/dev/null
}

echo "→ Opening accounts…"
CHECKING=$(open_account CHECKING "Everyday Checking")
SAVINGS=$(open_account SAVINGS "Emergency Fund")
echo "   checking=$CHECKING"
echo "   savings =$SAVINGS"

# Posted in chronological order; the backdating step below spreads these across the
# last ~30 days (oldest first), so salary lands early and spending follows.
echo "→ Posting a month of activity…"
deposit  "$CHECKING"            "5200.00" "Salary — Atlas Studio"
withdraw "$CHECKING"            "1850.00" "Rent — Oak Property"
transfer "$CHECKING" "$SAVINGS"  "500.00" "Transfer to savings"
withdraw "$CHECKING"              "94.30" "Con Edison"
withdraw "$CHECKING"              "86.24" "Whole Foods Market"
transfer "$CHECKING" "$SAVINGS"  "340.00" "Round-ups"
withdraw "$CHECKING"              "11.99" "Spotify"
withdraw "$CHECKING"               "6.50" "Blue Bottle Coffee"
withdraw "$CHECKING"               "2.75" "Metro Transit"
deposit  "$CHECKING"              "39.90" "Refund — Uniqlo"

# Backdate the demo history (timestamps only) so the balance trend looks alive.
if [[ "${BACKDATE:-1}" == "1" ]] && command -v docker >/dev/null \
   && docker ps --format '{{.Names}}' | grep -q "^${PG_CONTAINER}$"; then
  echo "→ Backdating demo history across the last 30 days…"
  docker exec -i "$PG_CONTAINER" psql -U "$DB_USERNAME" -d "$DB_NAME" -q -v ON_ERROR_STOP=1 \
    < "$(dirname "$0")/seed-history.sql" >/dev/null
fi

echo "→ Accounts for '$USERNAME':"
curl -fsS "${auth[@]}" "$BASE_URL/api/accounts" | python3 -m json.tool

echo "✓ Seed complete."
