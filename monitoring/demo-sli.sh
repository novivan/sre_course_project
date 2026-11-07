#!/usr/bin/env bash
set -euo pipefail

MODE="${1:-good}"
COUNT="${2:-30}"
BASE_URL="${BASE_URL:-http://localhost:8080}"

send_requests() {
  local label="$1"
  local url="$2"

  echo "Sending $COUNT '$label' requests to $url"
  for ((request = 1; request <= COUNT; request++)); do
    status=$(curl --silent --output /dev/null --write-out '%{http_code}' "$url")
    echo "$request/$COUNT: HTTP $status"
  done
}

case "$MODE" in
  good)
    QUERY="mode=ok"
    ;;
  errors)
    QUERY="mode=error"
    ;;
  slow)
    QUERY="mode=slow&delayMs=1000"
    ;;
  business-good)
    BUSINESS_MODE="ok"
    ;;
  business-errors)
    BUSINESS_MODE="error"
    ;;
  business-rejected)
    BUSINESS_MODE="rejected"
    ;;
  *)
    echo "Usage: $0 {good|errors|slow|business-good|business-errors|business-rejected} [request-count]" >&2
    exit 1
    ;;
esac

if [[ "$MODE" == business-* ]]; then
  for operation in CREATE_LOT CREATE_AUCTION PLACE_BID; do
    send_requests \
      "$MODE/$operation" \
      "$BASE_URL/api/v1/demo/business?operation=$operation&mode=$BUSINESS_MODE"
  done
else
  send_requests "$MODE" "$BASE_URL/api/v1/demo?$QUERY"
fi
