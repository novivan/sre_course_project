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
