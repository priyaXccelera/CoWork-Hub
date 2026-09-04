# CoWork Hub

A Spring Boot backend for coworking-space booking and membership management. It supports members, membership plans, spaces, bookings, waitlists, invoices, API keys, reports, reviews, and in-app notifications.

## Tech Stack

- Java 21 and Spring Boot 3.3.x
- Gradle (wrapper included)
- PostgreSQL 16+
- Flyway migrations
- springdoc-openapi / Swagger UI

## Prerequisites

- Java 21
- Docker Desktop (recommended for the bundled PostgreSQL setup), or a locally running PostgreSQL 16+ instance
- No system Gradle installation is required; use `./gradlew`

> **IST timezone note:** On machines configured for India Standard Time, start Java with `-Duser.timezone=Asia/Kolkata` to keep local date/time handling consistent. The provided command below includes it.

## Local Setup

1. Start PostgreSQL using Docker Compose:

   ```bash
   docker compose up -d
   docker compose ps
   ```

   Wait until the `postgres` service reports `healthy`. Compose provisions the local database using the same defaults as the application: database `gen_4d326c90fe72`, user `myuser`, password `mypassword`, port `5432`. Its named volume (`cowork_hub_postgres_data`) preserves data across restarts.

   To use an existing local PostgreSQL instance instead, create a database/user with matching credentials or set the datasource variables described in [Configuration](#configuration).

2. Start the application:

   ```bash
   chmod +x ./gradlew
   ./gradlew bootRun -Duser.timezone=Asia/Kolkata
   ```

   The API listens at `http://localhost:26986`. Swagger UI is at `http://localhost:26986/docs`; the OpenAPI document is at `http://localhost:26986/api-docs`.

3. Stop the database when finished (data is retained):

   ```bash
   docker compose down
   ```

## Database and Migrations

Schema changes are versioned in `src/main/resources/db/migration`. Flyway runs migrations automatically during application startup; Hibernate only validates the resulting schema (`ddl-auto=validate`). Seed data is applied after migrations and is idempotent.

There is intentionally no `flywayMigrate` Gradle task: the project uses Flyway through Spring Boot, not the Flyway Gradle plugin. Start the application to run migrations.

## Configuration

Datasource settings can be overridden through standard Spring environment variables. Defaults allow the application to connect to the bundled Docker Compose database or an identically configured local PostgreSQL instance.

| Environment variable | Default |
| --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/gen_4d326c90fe72` |
| `SPRING_DATASOURCE_USERNAME` | `myuser` |
| `SPRING_DATASOURCE_PASSWORD` | `mypassword` |
| `SERVER_PORT` | `26986` |
| `ADMIN_API_KEY` | local development placeholder |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173` |

For example, to use another PostgreSQL database:

```bash
export SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/cowork_hub'
export SPRING_DATASOURCE_USERNAME='cowork_user'
export SPRING_DATASOURCE_PASSWORD='change-me'
./gradlew bootRun -Duser.timezone=Asia/Kolkata
```

Set a strong `ADMIN_API_KEY` outside local development. It protects the API-key management endpoints that use the `X-Admin-Key` header.

## Authentication

Business endpoints require an `X-API-Key` request header. The seeded development database includes working API-key records for the sample admin and members; the ready-to-import Postman collection is preconfigured to use them. API-key management endpoints instead require `X-Admin-Key` and can mint new raw keys; raw key values are only returned at creation time.

Swagger UI has an **Authorize** button. Enter an API key in the `X-API-Key` security scheme before calling authenticated endpoints.

## API Overview

All business paths use the `/api/v1` prefix.

- API keys: `POST`, `GET`, `DELETE /api-keys`
- Users: `POST`, `GET`, `GET/{id}`, `PUT/{id}`, `DELETE/{id}`
- Membership plans: `POST`, `GET`, `GET/{id}`, `PUT/{id}`, `DELETE/{id}`
- Spaces: `POST`, `GET`, `GET/{id}`, `PUT/{id}`, `DELETE/{id}`
- Bookings: `POST`, `GET`, `GET/{id}`, `POST/{id}/cancel`
- Waitlist: `POST`, `GET`
- Invoices: `POST /invoices/generate`, `GET /invoices`, `GET /invoices/{id}`
- Reports: `GET /reports/space-utilization`, `/reports/revenue`, `/reports/top-members`
- Reviews: `POST`, `GET`, `GET/{id}`, `PUT/{id}`, `DELETE/{id}`
- Notifications: `GET /notifications`, `GET /notifications/unread-count`, `PATCH /notifications/{id}/read`, `PATCH /notifications/read-all`

Pagination uses `offset` and `limit` (default 20, maximum 100). Review ratings are 1–5; reviews require an owned completed booking and are soft-deleted. Notifications are personal in-app records: they are generated for booking confirmations/cancellations, waitlist promotions, and invoice generation, and can only be read or marked as read by their owner.

## Postman Collection

Import `postman/CoWork-Hub.postman_collection.json`. It covers every API operation and contains saved example responses, including review validation/authorization edge cases.
