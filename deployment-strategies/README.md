# Стратегии развёртывания

Каталог содержит Envoy Gateway и манифесты Rolling, Blue/Green, Canary, A/B и
Shadow для приложения аукциона.

## Запуск

```bash
./deployment-strategies/setup.sh
```

Скрипт запускает minikube, собирает `auction:local`, применяет обычные
манифесты приложения, устанавливает Envoy Gateway и поднимает v1/v2.

Проверить всё одной командой:

```bash
./deployment-strategies/verify.sh
```

Финальный вывод проверки сохранён в `logs/verification.log`.

Непрерывный трафик через Gateway:

```bash
./deployment-strategies/traffic.sh
USER_GROUP=beta ./deployment-strategies/traffic.sh
```

## Стратегии

Rolling:

```bash
kubectl apply -f deployment-strategies/strategies/rolling/02-v1.yaml
kubectl apply -f deployment-strategies/strategies/rolling/01-route.yaml
kubectl apply -f deployment-strategies/strategies/rolling/03-v2.yaml
kubectl get pods -n auction -w
```

Blue/Green:

```bash
kubectl apply -f deployment-strategies/strategies/blue-green/01-green.yaml
kubectl apply -f deployment-strategies/strategies/blue-green/02-blue.yaml
kubectl apply -f deployment-strategies/strategies/blue-green/01-green.yaml
```

Canary:

```bash
kubectl apply -f deployment-strategies/strategies/canary/01-90-10.yaml
kubectl apply -f deployment-strategies/strategies/canary/02-50-50.yaml
kubectl apply -f deployment-strategies/strategies/canary/03-0-100.yaml
```

A/B:

```bash
kubectl apply -f deployment-strategies/strategies/ab/httproute.yaml
USER_GROUP=beta ./deployment-strategies/traffic.sh 5
```

Shadow:

```bash
kubectl apply -f deployment-strategies/strategies/shadow/httproute.yaml
./deployment-strategies/traffic.sh 10
kubectl logs -n auction -l app.kubernetes.io/track=versioned,app.kubernetes.io/version=v2.0.0 --since=2m --prefix
```

Вернуть основной маршрут на v1:

```bash
./deployment-strategies/reset.sh
```
