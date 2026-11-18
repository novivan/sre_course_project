#!/usr/bin/env bash
set -euo pipefail

ENVOY_GATEWAY_VERSION="${ENVOY_GATEWAY_VERSION:-v1.10.0}"
HERE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd -- "${HERE}/.." && pwd)"

for command in docker kubectl minikube; do
  command -v "${command}" >/dev/null || {
    echo "Не найден ${command}" >&2
    exit 1
  }
done

if ! minikube status >/dev/null 2>&1; then
  minikube start --driver=docker --cpus=4 --memory=7168
fi

eval "$(minikube docker-env)"
docker build --progress=plain -t auction:local "${PROJECT_DIR}"

kubectl apply -k "${PROJECT_DIR}/k8s"
kubectl rollout status statefulset/postgres -n auction --timeout=300s
kubectl rollout status deployment/auction-api -n auction --timeout=300s
kubectl rollout status deployment/auction-worker -n auction --timeout=300s

kubectl apply --server-side -f \
  "https://github.com/envoyproxy/gateway/releases/download/${ENVOY_GATEWAY_VERSION}/install.yaml"
kubectl wait deployment/envoy-gateway -n envoy-gateway-system \
  --for=condition=Available --timeout=300s

kubectl apply -f "${HERE}/00-gateway.yaml"
kubectl wait gateway/auction-gateway -n auction \
  --for=condition=Accepted --timeout=180s

kubectl apply -k "${HERE}/versions/v1"
kubectl apply -k "${HERE}/versions/v2"
kubectl rollout status deployment/auction-api-v1 -n auction --timeout=300s
kubectl rollout status deployment/auction-api-v2 -n auction --timeout=300s

kubectl apply -f "${HERE}/strategies/rolling/01-route.yaml"

echo
echo "Готово: приложение, две версии и Envoy Gateway запущены."
echo "Проверка всех стратегий: ${HERE}/verify.sh"
