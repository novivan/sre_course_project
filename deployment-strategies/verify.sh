#!/usr/bin/env bash
set -euo pipefail

HERE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
LOCAL_PORT="${LOCAL_PORT:-8888}"
BASE_URL="http://127.0.0.1:${LOCAL_PORT}"

section() {
  printf '\n===== %s =====\n' "$1"
}

apply_route() {
  kubectl apply -f "$1" >/dev/null
  for _ in $(seq 1 40); do
    if curl -sf "${BASE_URL}/api/v1/version" >/dev/null 2>&1; then
      sleep 1
      return
    fi
    sleep 0.25
  done
  echo "Маршрут не начал отвечать: $1" >&2
  return 1
}

version() {
  local header="${1:-}"
  local response
  if [[ -n "${header}" ]]; then
    response="$(curl -sf -H "${header}" "${BASE_URL}/api/v1/version")"
  else
    response="$(curl -sf "${BASE_URL}/api/v1/version")"
  fi
  printf '%s\n' "${response}" | sed -n 's/.*"version":"\([^"]*\)".*/\1/p'
}

count_versions() {
  local count="$1"
  local header="${2:-}"
  local request_number
  for request_number in $(seq 1 "${count}"); do
    version "${header}"
  done | sort | uniq -c
}

PROXY_SERVICE="$(kubectl get service -n envoy-gateway-system \
  -l gateway.envoyproxy.io/owning-gateway-name=auction-gateway \
  -o jsonpath='{.items[0].metadata.name}')"
kubectl port-forward -n envoy-gateway-system \
  "service/${PROXY_SERVICE}" "${LOCAL_PORT}:80" >/tmp/auction-strategies-port-forward.log 2>&1 &
PORT_FORWARD_PID=$!
trap 'kill "${PORT_FORWARD_PID}" 2>/dev/null || true' EXIT INT TERM

for _ in $(seq 1 40); do
  curl -sf "${BASE_URL}/api/v1/version" >/dev/null 2>&1 && break
  sleep 0.25
done

section "Pods and probes"
kubectl get pods -n auction \
  -l app.kubernetes.io/component=api \
  -L app.kubernetes.io/version

section "Rolling v1 -> v2"
kubectl apply -f "${HERE}/strategies/rolling/02-v1.yaml" >/dev/null
apply_route "${HERE}/strategies/rolling/01-route.yaml"
kubectl rollout status deployment/auction-api -n auction --timeout=300s >/dev/null
echo "До обновления:"
count_versions 6

ROLLING_LOG="$(mktemp)"
(
  for _ in $(seq 1 60); do
    version >>"${ROLLING_LOG}" || true
    sleep 0.25
  done
) &
ROLLING_TRAFFIC_PID=$!
sleep 1
kubectl apply -f "${HERE}/strategies/rolling/03-v2.yaml" >/dev/null
wait "${ROLLING_TRAFFIC_PID}"
kubectl rollout status deployment/auction-api -n auction --timeout=300s >/dev/null
echo "Во время обновления:"
sort "${ROLLING_LOG}" | uniq -c
echo "После обновления:"
count_versions 6

section "Blue/Green and rollback"
apply_route "${HERE}/strategies/blue-green/01-green.yaml"
echo "Green:"
count_versions 8
apply_route "${HERE}/strategies/blue-green/02-blue.yaml"
echo "Blue:"
count_versions 8
apply_route "${HERE}/strategies/blue-green/01-green.yaml"
echo "Rollback to green:"
count_versions 8

section "Canary 90/10 -> 50/50 -> 0/100"
apply_route "${HERE}/strategies/canary/01-90-10.yaml"
echo "90/10, 100 requests:"
count_versions 100
apply_route "${HERE}/strategies/canary/02-50-50.yaml"
echo "50/50, 100 requests:"
count_versions 100
apply_route "${HERE}/strategies/canary/03-0-100.yaml"
echo "0/100, 30 requests:"
count_versions 30

section "A/B by x-user-group header"
apply_route "${HERE}/strategies/ab/httproute.yaml"
echo "Default audience:"
count_versions 5
echo "Beta audience:"
count_versions 5 "x-user-group: beta"

section "Shadow traffic"
kubectl rollout restart deployment/auction-api-v2 -n auction >/dev/null
kubectl rollout status deployment/auction-api-v2 -n auction --timeout=300s >/dev/null
apply_route "${HERE}/strategies/shadow/httproute.yaml"
echo "Client responses (must be v1):"
count_versions 12
sleep 2
echo "Requests mirrored to v2 logs:"
kubectl logs -n auction \
  -l app.kubernetes.io/track=versioned,app.kubernetes.io/version=v2.0.0 \
  -c api --since=2m --tail=100 --prefix=true --max-log-requests=10 \
  | grep 'Deployment info requested' | tail -20 || true

section "HTTPRoute status"
kubectl get httproute/auction-route -n auction -o wide

echo
echo "All deployment strategies verified."
