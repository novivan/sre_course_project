#!/usr/bin/env bash
set -euo pipefail

HERE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

kubectl apply -f "${HERE}/strategies/rolling/02-v1.yaml"
kubectl apply -f "${HERE}/strategies/rolling/01-route.yaml"
kubectl rollout status deployment/auction-api -n auction --timeout=300s

echo "Основной API и маршрут возвращены на v1.0.0."
