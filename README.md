# Система управления банковскими картами

Backend-приложение на Java (Spring Boot) для управления банковскими картами.

## Возможности

### Аутентификация и авторизация
- Spring Security + JWT
- Роли: `ROLE_ADMIN`, `ROLE_USER`

### Карты
- Хранение номера карты (PAN) в БД в зашифрованном виде
- Отображение PAN только в виде маски: `**** **** **** 1234`
- Статусы: `ACTIVE`, `BLOCKED`, `EXPIRED`
- Просмотр своих карт с фильтрацией и пагинацией
- Админский поиск/управление картами
- Пользовательский запрос блокировки карты

### Переводы
- Переводы между своими картами
- Идемпотентность переводов через `idempotencyKey`
- История транзакций пользователя и по конкретной карте

## Технологии
- Java 17
- Spring Boot: Web, Validation, Security, Data JPA
- PostgreSQL
- Liquibase
- JWT (jjwt)
- Swagger UI (springdoc)

## Конфигурация

Основные настройки находятся в:
- `src/main/resources/application.yml`

### Переменные окружения

Для запуска через Docker Compose используется файл `.env`.

Шаблон:
- `.env.example` — **скопируй** в `.env` и задай свои значения.

Используемые переменные:
- `APP_PORT` — порт приложения на хосте
- `POSTGRES_PORT` — порт PostgreSQL на хосте
- `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
- `DB_URL`, `DB_USER`, `DB_PASSWORD` — параметры datasource для приложения
- `JWT_SECRET`, `JWT_EXPIRATION`
- `CARD_CRYPTO_SECRET` — секрет для шифрования PAN

## Запуск (Docker Compose)

### 1) Подготовка `.env`

```bash
cp .env.example .env
```

Далее отредактируй `.env`.

### 2) Запуск

```bash
docker compose up --build
```

После старта:
- приложение: `http://localhost:${APP_PORT}` (по умолчанию `8080`)
- swagger-ui: `http://localhost:${APP_PORT}/swagger-ui/index.html`

## Миграции и тестовые данные

Liquibase применяет миграции автоматически при старте.

В миграциях создаются роли и тестовый пользователь:
- username: `admin`
- password: `admin123`
- roles: `ROLE_ADMIN`, `ROLE_USER`

## Документация API

### Swagger UI
- `http://localhost:${APP_PORT}/swagger-ui/index.html`

### OpenAPI YAML
- `http://localhost:${APP_PORT}/docs/openapi.yaml`

## Аутентификация

1) Получить токен:
- `POST /auth/login`

2) Использовать токен в запросах:

Заголовок:
- `Authorization: Bearer <token>`

## Основные эндпоинты

### Auth
- `POST /auth/login`

### Cards
- `POST /cards/admin?userId=...` (ADMIN)
- `GET /cards/me` (USER/ADMIN)
- `GET /cards/me/{cardId}` (USER/ADMIN)
- `POST /cards/me/{cardId}/block-request` (USER)
- `GET /cards/admin/search` (ADMIN)
- `PATCH /cards/admin/{cardId}` (ADMIN)
- `DELETE /cards/admin/{cardId}` (ADMIN)

### Transactions
- `POST /transactions` (USER) — пополнение баланса карты (депозит)
- `POST /transactions/transfer` (USER/ADMIN)
- `GET /transactions/me` (USER/ADMIN)
- `GET /transactions/me/card/{cardId}` (USER/ADMIN)

### Users
- `GET /users/me` (USER/ADMIN)
- `GET /users/admin` (ADMIN)
- `GET /users/admin/search` (ADMIN)
- `GET /users/admin/role` (ADMIN)
- `GET /users/admin/{id}` (ADMIN)
- `PATCH /users/admin/{id}` (ADMIN)
- `DELETE /users/admin/{id}` (ADMIN)


