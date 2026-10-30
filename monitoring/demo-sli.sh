#!/usr/bin/env bash
set -euo pipefail

MODE="${1:-good}"
COUNT="${2:-30}"
BASE_URL="${BASE_URL:-http://localhost:8080}"

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
  *)
    echo "Usage: $0 {good|errors|slow} [request-count]" >&2
    exit 1
    ;;
esac

echo "Sending $COUNT '$MODE' requests to $BASE_URL/api/v1/demo"
for ((request = 1; request <= COUNT; request++)); do
  status=$(curl --silent --output /dev/null --write-out '%{http_code}' \
    "$BASE_URL/api/v1/demo?$QUERY")
  echo "$request/$COUNT: HTTP $status"
done
