# Учебный аукцион

Простое приложение для курса по SRE. API работает с пользователями, лотами,
аукционами и ставками. Worker запускает и завершает аукционы. Данные хранятся
в PostgreSQL

## Требования

- Docker
- Docker Compose
- Java 21, если нужно запускать тесты без Docker-сборки

## Запуск

Собрать образ и запустить приложение:

```bash
docker compose up --build -d
```

После запуска:

- frontend: <http://localhost:8080>
- health-check: <http://localhost:8080/actuator/health>
- PostgreSQL: `localhost:5433`

## Разработка и тесты

```bash
./gradlew clean test bootJar
```

Интеграционные тесты запускают PostgreSQL через Testcontainers

## HTTP API

| Метод | Путь | Что делает |
|---|---|---|
| `POST` | `/api/v1/users` | создать пользователя |
| `GET` | `/api/v1/users` | получить пользователей |
| `POST` | `/api/v1/lots` | создать лот |
| `GET` | `/api/v1/lots` | получить лоты |
| `GET` | `/api/v1/lots/{id}` | получить лот |
| `PUT` | `/api/v1/lots/{id}` | изменить лот |
| `DELETE` | `/api/v1/lots/{id}?sellerId=...` | удалить лот |
| `POST` | `/api/v1/auctions` | создать аукцион |
| `GET` | `/api/v1/auctions` | получить аукционы |
| `GET` | `/api/v1/auctions/{id}` | получить аукцион |
| `POST` | `/api/v1/auctions/{id}/bids` | сделать ставку |
| `GET` | `/api/v1/auctions/{id}/bids` | получить ставки |

Полный CRUD сделан для лотов. Менять и удалять лот можно только до создания
аукциона

## Конфигурация и миграции

Настройки передаются через переменные окружения. Пример есть в `.env.example`.
Flyway запускается в API, в worker он отключен

Подробности находятся в [`Отчёт.md`](Отчёт.md)
