#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
MODE="${1:-all}"

trigger_business_alert() {
  "${SCRIPT_DIR}/demo-sli.sh" business-good 5
  "${SCRIPT_DIR}/demo-sli.sh" business-errors 2
}

trigger_http_alert() {
  "${SCRIPT_DIR}/demo-sli.sh" good 10
  "${SCRIPT_DIR}/demo-sli.sh" errors 10
}

case "${MODE}" in
  business)
    trigger_business_alert
    ;;
  http)
    trigger_http_alert
    ;;
  all)
    trigger_business_alert
    trigger_http_alert
    ;;
  *)
    echo "Usage: $0 {all|business|http}" >&2
    exit 1
    ;;
esac

echo "Requests sent. Rules need about 60 seconds to become firing and reach Alertmanager."
