#!/usr/bin/env bash
#
# Seed demo data through the real API, exactly as a client would: get a token from
# Keycloak, then open accounts, deposit, and transfer. Requires the stack to be up
# (`make up`). Idempotent-ish: re-running creates more accounts for the demo user.
#
# Usage:  ./infra/seed.sh
# Env:    BASE_URL (default http://localhost:8080), KC_URL (default http://localhost:8088),
#         USERNAME (default alice), PASSWORD (default password)
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
KC_URL="${KC_URL:-http://localhost:8088}"
REALM="${REALM:-ledger-bank}"
CLIENT_ID="${CLIENT_ID:-ledger-bank-web}"
USERNAME="${USERNAME:-alice}"
PASSWORD="${PASSWORD:-password}"

json_field() { python3 -c "import sys,json; print(json.load(sys.stdin)['$1'])"; }
uuid() { python3 -c "import uuid; print(uuid.uuid4())"; }

echo "→ Requesting access token for '$USERNAME' from Keycloak…"
TOKEN=$(curl -fsS -X POST \
  "$KC_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=$CLIENT_ID" -d "username=$USERNAME" -d "password=$PASSWORD" \
  -d "grant_type=password" | json_field access_token)

auth=(-H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json")

echo "→ Opening a checking and a savings account…"
CHECKING=$(curl -fsS "${auth[@]}" -X POST "$BASE_URL/api/accounts" \
  -d '{"type":"CHECKING","currency":"USD"}' | json_field id)
SAVINGS=$(curl -fsS "${auth[@]}" -X POST "$BASE_URL/api/accounts" \
  -d '{"type":"SAVINGS","currency":"USD"}' | json_field id)
echo "   checking=$CHECKING"
echo "   savings =$SAVINGS"

echo "→ Depositing \$1,000.00 into checking…"
curl -fsS "${auth[@]}" -H "Idempotency-Key: $(uuid)" -X POST \
  "$BASE_URL/api/accounts/$CHECKING/deposits" \
  -d '{"amount":"1000.00","currency":"USD","description":"opening deposit"}' >/dev/null

echo "→ Transferring \$250.00 checking → savings…"
curl -fsS "${auth[@]}" -H "Idempotency-Key: $(uuid)" -X POST \
  "$BASE_URL/api/transfers" \
  -d "{\"fromAccountId\":\"$CHECKING\",\"toAccountId\":\"$SAVINGS\",\"amount\":\"250.00\",\"currency\":\"USD\",\"description\":\"to savings\"}" >/dev/null

echo "→ Accounts for '$USERNAME':"
curl -fsS "${auth[@]}" "$BASE_URL/api/accounts" | python3 -m json.tool

echo "✓ Seed complete."
