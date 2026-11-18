#!/usr/bin/env bash
set -euo pipefail

LOCAL_PORT="${LOCAL_PORT:-8888}"
RATE="${RATE:-0.25}"
COUNT="${1:-0}"
USER_GROUP="${USER_GROUP:-}"

PROXY_SERVICE="$(kubectl get service -n envoy-gateway-system \
  -l gateway.envoyproxy.io/owning-gateway-name=auction-gateway \
  -o jsonpath='{.items[0].metadata.name}')"

kubectl port-forward -n envoy-gateway-system \
  "service/${PROXY_SERVICE}" "${LOCAL_PORT}:80" >/tmp/auction-gateway-port-forward.log 2>&1 &
PORT_FORWARD_PID=$!
trap 'kill "${PORT_FORWARD_PID}" 2>/dev/null || true' EXIT INT TERM

for _ in $(seq 1 40); do
  curl -sf "http://127.0.0.1:${LOCAL_PORT}/api/v1/version" >/dev/null 2>&1 && break
  sleep 0.25
done

echo "Трафик через Envoy Gateway. Ctrl-C для остановки."
if [[ -n "${USER_GROUP}" ]]; then
  echo "Заголовок x-user-group: ${USER_GROUP}"
fi

request_number=0
while :; do
  if [[ -n "${USER_GROUP}" ]]; then
    response="$(curl -sf -H "x-user-group: ${USER_GROUP}" \
      "http://127.0.0.1:${LOCAL_PORT}/api/v1/version")"
  else
    response="$(curl -sf "http://127.0.0.1:${LOCAL_PORT}/api/v1/version")"
  fi
  printf '%s | %s\n' "$(date +%H:%M:%S)" "${response}"

  request_number=$((request_number + 1))
  if [[ "${COUNT}" -gt 0 && "${request_number}" -ge "${COUNT}" ]]; then
    break
  fi
  sleep "${RATE}"
done
