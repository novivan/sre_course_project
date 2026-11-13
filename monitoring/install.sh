#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

helm repo add vm https://victoriametrics.github.io/helm-charts/ --force-update
helm repo update vm

kubectl create namespace monitoring --dry-run=client -o yaml | kubectl apply -f -

helm upgrade --install vm-single vm/victoria-metrics-single \
  --namespace monitoring \
  --version 0.49.0 \
  --values "${SCRIPT_DIR}/victoria-metrics-values.yaml" \
  --wait

helm upgrade --install vm-agent vm/victoria-metrics-agent \
  --namespace monitoring \
  --version 0.50.0 \
  --values "${SCRIPT_DIR}/vmagent-values.yaml" \
  --wait

kubectl apply -k "${SCRIPT_DIR}/alerts"
kubectl rollout status deployment/alert-receiver -n monitoring --timeout=180s
kubectl rollout status deployment/alertmanager -n monitoring --timeout=180s
kubectl rollout status deployment/vmalert -n monitoring --timeout=180s

kubectl apply -k "${SCRIPT_DIR}/grafana"
kubectl rollout status deployment/grafana -n monitoring --timeout=300s
