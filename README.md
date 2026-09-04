# CoWork Hub

A production-ready Spring Boot backend for a Coworking Space Booking & Membership
Management System: manage members, membership plans, coworking spaces, bookings
(with conflict detection, credit-hour billing, cancellation policy and waitlist
auto-promotion), and monthly invoices.

## Tech Stack

- Java 21, Spring Boot 3.3.x
- Spring Web, Spring Data JPA, Spring Security, Bean Validation
- PostgreSQL
- springdoc-openapi (Swagger UI)
- Gradle

## Architecture

Layered architecture: `Controller -> Service -> Repository -> Entity/DTO`.

```
src/main/java/com/example/app/
  AppApplication.java        Spring Boot entry point (+ @EnableJpaAuditing)
  config/                    SecurityConfig, CorsConfig
  entity/                    JPA entities (User, MembershipPlan, Space, Booking, Waitlist, Invoice, enums)
  repository/                Spring Data JPA repositories + Specifications
  service/                   Business logic (booking engine, invoicing, reporting, ...)
  controller/                REST controllers (api/v1)
  dto/                       Request/response DTOs
  security/                  ApiKey entity, ApiKeyRepository, ApiKeyFilter, CurrentActor, AccessGuard
  exception/                 Custom exceptions + GlobalExceptionHandler
  util/                      OffsetPageRequest (offset/limit pagination helper)
```

## Authentication

All business endpoints require an `X-API-Key` header. Keys are managed through a
separate, admin-gated endpoint group:

- `POST /api/v1/api-keys` — mint a new API key. Requires the `X-Admin-Key` header
  to match the `admin.api-key` value configured in `application.properties`.
  Body: `{ "name": "my-key", "role": "ADMIN" | "MEMBER", "userId": optional }`.
  Response includes the raw key **once** — store it, it cannot be retrieved again
  (only its SHA-256 hash is persisted).
- `GET /api/v1/api-keys` — list keys (admin gated).
- `DELETE /api/v1/api-keys/{id}` — revoke a key (admin gated).

Creating a `User` (`POST /api/v1/users`, admin only) automatically mints a linked
API key with the user's role and returns the raw key once in the response
`apiKey` field — this is how a MEMBER obtains credentials to manage their own
bookings.

Roles:
- **ADMIN** — full access: manage users/spaces/membership plans, see and manage
  all bookings, generate invoices, view reports.
- **MEMBER** — manage only their own bookings/invoices/waitlist entries.

## Business Rules

1. **Booking conflict detection** — booking creation locks the target `Space`
   row (`SELECT ... FOR UPDATE`) before checking for overlapping `CONFIRMED`
   bookings, preventing race conditions under concurrent requests. If a
   conflict is found, the booking is created with status `WAITLISTED` and a
   matching `Waitlist` row is added.
2. **Cost calculation** — if the user has a `MembershipPlan`, credit hours are
   deducted first; any remaining hours are billed at the plan's
   `overageRatePerHour`. Users without a plan are billed the space's
   `hourlyRate` for the full duration.
3. **Cancellation policy** — cancelling more than 2 hours before `startTime` is
   free (credit hours used are refunded). Cancelling within 2 hours charges
   25% of the original cost.
4. **Waitlist auto-promotion** — cancelling a `CONFIRMED` booking automatically
   promotes the earliest `WAITING` waitlist entry for that space (if the
   requested slot no longer conflicts), confirming its booking and billing it.
5. **Monthly invoice generation** — `POST /api/v1/invoices/generate` aggregates
   a user's overage charges (confirmed/completed bookings) and cancellation
   fees (cancelled bookings) for a given `YYYY-MM` month.

## API Overview (prefix `/api/v1`)

| Method | Path | Access | Notes |
|---|---|---|---|
| POST | /api-keys | Admin (`X-Admin-Key`) | mint API key |
| GET | /api-keys | Admin (`X-Admin-Key`) | list API keys |
| DELETE | /api-keys/{id} | Admin (`X-Admin-Key`) | revoke API key |
| POST/GET/GET{id}/PUT/DELETE | /users | ADMIN | soft delete |
| POST/GET/GET{id}/PUT/DELETE | /membership-plans | ADMIN | |
| POST/GET/GET{id}/PUT/DELETE | /spaces | ADMIN | soft delete, filter by `type` |
| POST | /bookings | ADMIN or MEMBER (own) | conflict detection + billing |
| GET | /bookings | ADMIN (all) / MEMBER (own) | filter by `date`, `status`, `spaceType`; paginated |
| GET | /bookings/{id} | ADMIN or owning MEMBER | |
| POST | /bookings/{id}/cancel | ADMIN or owning MEMBER | cancellation policy + waitlist promotion |
| GET | /waitlist | ADMIN (all) / MEMBER (own) | paginated |
| POST | /invoices/generate | ADMIN | aggregates a month's charges |
| GET | /invoices | ADMIN / MEMBER (own) | paginated |
| GET | /invoices/{id} | any authenticated | |
| GET | /reports/space-utilization | ADMIN | `%` per space per week |
| GET | /reports/revenue | ADMIN | `?month=YYYY-MM` |
| GET | /reports/top-members | ADMIN | `?limit=5` |
| GET | /actuator/health | public | health check |
| GET | /docs | public | Swagger UI |
| GET | /api-docs | public | OpenAPI JSON |

Pagination uses `offset`/`limit` query params (default `limit=20`).

## Configuration

`src/main/resources/application.properties`:

```
server.port=20330
spring.datasource.url=jdbc:postgresql://localhost:5432/gen_4d326c90fe72
spring.datasource.username=myuser
spring.datasource.password=mypassword
spring.jpa.hibernate.ddl-auto=update
admin.api-key=<randomly generated at project creation>
```

Environment variable overrides used by `start.sh`/`start.bat`:

- `SERVER_PORT` (defaults to `20330`)

## Running Locally

```bash
chmod +x ./gradlew ./start.sh
./start.sh
```

The app starts on `http://localhost:20330`. Swagger UI: `http://localhost:20330/docs`.

### Database

The schema is created/updated automatically via `spring.jpa.hibernate.ddl-auto=update`
and seeded via `src/main/resources/data.sql` (3 membership plans, 6 spaces,
5 users, 5 historical bookings) on first startup.

## Example Usage

```bash
# 1. Mint an admin API key (replace <ADMIN_KEY> with the value of admin.api-key)
curl -X POST http://localhost:20330/api/v1/api-keys \
  -H "Content-Type: application/json" \
  -H "X-Admin-Key: <ADMIN_KEY>" \
  -d '{"name":"admin-test","role":"ADMIN"}'

# 2. Use the returned "apiKey" value as X-API-Key for all business endpoints
curl http://localhost:20330/api/v1/spaces -H "X-API-Key: <RAW_KEY>"
```

## Tests

API endpoints were exercised end-to-end with `curl` (see `/api_tests/test_results.md`
for the full pass/fail matrix) and summarized in `api_test_report.xlsx`.
