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
  AppApplication.java        Spring Boot entry point (+ @EnableJpaAuditing, @EnableScheduling)
  config/                    SecurityConfig, CorsConfig
  entity/                    JPA entities (User, MembershipPlan, Space, Booking, Waitlist, Invoice, Review, enums)
  repository/                Spring Data JPA repositories + Specifications
  service/                   Business logic (booking engine, invoicing, reporting, ...)
  scheduler/                 BookingCompletionScheduler (auto-completes past bookings)
  controller/                REST controllers (api/v1)
  dto/                       Request/response DTOs
  security/                  ApiKey entity, ApiKeyRepository, ApiKeyFilter, CurrentActor, AccessGuard,
                              RestAuthenticationEntryPoint (401), RestAccessDeniedHandler (403)
  exception/                 Custom exceptions (BusinessRuleException=400, ConflictException=409, ...)
                              + GlobalExceptionHandler
  util/                      OffsetPageRequest (offset/limit pagination helper, capped at 100)

src/main/resources/db/migration/  Versioned Flyway SQL migrations
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
- **MEMBER** — can read spaces/membership plans and manage only their own
  bookings/invoices/waitlist entries.

Authentication failures (missing/blank/invalid/revoked key) return **401 Unauthorized**;
an authenticated actor without the required role/ownership gets **403 Forbidden**.
Soft-deleting a user immediately revokes all of that user's API keys, and a key tied to a
deleted/inactive user is rejected even if the key row itself is still marked active.
Raw API keys are only ever returned once, at creation time (`POST /users` or
`POST /api-keys`); no endpoint returns a previously-issued raw key again.

## Business Rules

1. **Booking conflict detection** — booking creation locks the target `Space`
   row (`SELECT ... FOR UPDATE`) before counting overlapping `CONFIRMED`
   bookings against the space's `capacity`, preventing race conditions under
   concurrent requests. Once capacity is exhausted for the requested slot the
   request is rejected with **409 Conflict**; the caller can then explicitly
   join the waitlist via `POST /api/v1/waitlist`.
2. **Cost calculation** — if the user has a `MembershipPlan`, credit hours are
   deducted first; any remaining hours are billed at the plan's
   `overageRatePerHour`. Users without a plan are billed the space's
   `hourlyRate` for the full duration. The full market value of the booking
   (`space.hourlyRate x hours`) is always preserved as `originalCost`,
   regardless of how much was actually paid in cash vs. credit hours.
3. **Cancellation policy** — cancelling more than 2 hours before `startTime` is
   free. Cancelling within 2 hours charges 25% of the booking's `originalCost`
   (so bookings paid entirely with credit hours still incur a real fee).
   Any credit hours that had been reserved for the booking are always
   refunded on cancellation, since the slot is released either way.
4. **Waitlist auto-promotion** — cancelling a `CONFIRMED` booking attempts to
   promote every `WAITING` waitlist entry for that space, oldest first; each
   candidate is evaluated independently against current capacity, so a
   still-conflicting entry doesn't block promotion of a later entry whose
   slot is free.
5. **Monthly invoice generation** — `POST /api/v1/invoices/generate` aggregates
   a user's overage charges and cancellation fees for a given `YYYY-MM` month
   (validated; rejects deleted users, malformed/out-of-range months with 400).
6. **Space capacity** — a space can host up to `capacity` concurrent
   `CONFIRMED` bookings; this is enforced both on creation and on waitlist
   promotion, and factored into the space-utilization report.
7. **Soft delete guards** — Users, Spaces and Membership Plans cannot be
   deleted while still referenced by active/future bookings or assigned
   users respectively; such attempts return 409 Conflict. Deleting a user
   also immediately revokes all of their API keys.
8. **Auto-completion** — a background job runs every minute and transitions
   `CONFIRMED` bookings whose `endTime` has passed into `COMPLETED`.
9. **Reviews** — a member can only review a space if they have a booking for that
   space with status `COMPLETED`; at most one review is allowed per booking
   (multiple reviews of the same space are fine as long as they come from
   different completed bookings). `rating` must be between 1 and 5 (inclusive);
   `comment` is optional but capped at 500 characters. A member may edit or
   delete only their own review; an Admin may delete any review. Reviews are
   soft-deleted, never hard-deleted. `GET /spaces` and `GET /spaces/{id}` (and
   the space-utilization report) include `averageRating` (rounded to 1 decimal)
   and `totalReviews`, computed from non-deleted reviews.

## API Overview (prefix `/api/v1`)

| Method | Path | Access | Notes |
|---|---|---|---|
| POST | /api-keys | Admin (`X-Admin-Key`) | mint API key |
| GET | /api-keys | Admin (`X-Admin-Key`) | list API keys |
| DELETE | /api-keys/{id} | Admin (`X-Admin-Key`) | revoke API key |
| POST/GET/GET{id}/PUT/DELETE | /users | ADMIN | soft delete, blocked while user has active bookings |
| POST/GET/GET{id}/PUT/DELETE | /membership-plans | ADMIN | delete blocked while plan is assigned to users |
| POST/PUT/DELETE /spaces | ADMIN | soft delete, blocked while space has active bookings |
| GET /spaces, GET /spaces/{id} | any authenticated (ADMIN or MEMBER) | so members can discover spaces to book |
| POST | /bookings | ADMIN or MEMBER (own) | capacity-aware conflict detection (409) + billing |
| GET | /bookings | ADMIN (all) / MEMBER (own) | filter by `date` or `from`/`to` (`startDate`/`endDate`), `status`, `spaceType`; sort via `sort=field,asc|desc`; paginated (`limit` capped at 100) |
| GET | /bookings/{id} | ADMIN or owning MEMBER | |
| POST | /bookings/{id}/cancel | ADMIN or owning MEMBER | cancellation policy + waitlist promotion |
| POST | /waitlist | ADMIN or MEMBER (own) | explicitly join the waitlist for a full space/slot |
| GET | /waitlist | ADMIN (all) / MEMBER (own) | paginated |
| POST | /invoices/generate | ADMIN | aggregates a month's charges |
| GET | /invoices | ADMIN / MEMBER (own) | paginated |
| GET | /invoices/{id} | ADMIN or owning MEMBER | |
| GET | /reports/space-utilization | ADMIN | `%` per space per week (capacity-aware) |
| GET | /reports/revenue | ADMIN | `?month=YYYY-MM` |
| GET | /reports/top-members | ADMIN | `?limit=5` (max 100) |
| POST | /reviews | authenticated (own bookings only) | requires a COMPLETED booking; one review per booking |
| GET | /reviews?spaceId={id} | any authenticated | paginated, sorted by `createdAt desc` by default |
| GET | /reviews/{id} | any authenticated | |
| PUT | /reviews/{id} | owning member only | edit own review |
| DELETE | /reviews/{id} | owning member or ADMIN | soft delete |
| GET | /actuator/health | public | health check |
| GET | /docs | public | Swagger UI |
| GET | /api-docs | public | OpenAPI JSON |

Pagination uses `offset`/`limit` query params (default `limit=20`, maximum `limit=100`).

## Error Responses

All errors return a structured JSON body (`timestamp`, `status`, `error`, `message`, `path`,
optional `details`) with the correct HTTP status code:

- `400` — malformed/invalid input (bad JSON, wrong types, invalid enum/date, failed validation,
  business-rule violations such as `endTime` before `startTime`)
- `401` — missing, invalid, blank or revoked API key
- `403` — authenticated but lacking the required role/ownership
- `404` — resource not found / unmapped route
- `409` — conflicts with current state (double booking at capacity, cancelling an
  already-cancelled booking, deleting a membership plan/space/user still in use, duplicate email)
- `500` — unexpected server error (message is a generic, sanitized string; no SQL, stack traces
  or internal class names are ever returned to the client)

## Configuration

`src/main/resources/application.properties`:

```
server.port=26986
spring.datasource.url=jdbc:postgresql://localhost:5432/gen_4d326c90fe72
spring.datasource.username=myuser
spring.datasource.password=mypassword
spring.jpa.hibernate.ddl-auto=validate
admin.api-key=${ADMIN_API_KEY:local-dev-admin-key-change-me}
app.cors.allowed-origins=${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:5173}
```

Environment variable overrides used by `start.sh`/`start.bat`:

- `SERVER_PORT` (defaults to `26986`)
- `ADMIN_API_KEY` — **must** be set to a strong, secret value in any real
  deployment. It is not committed to source control; the value in
  `application.properties` is only a local-dev placeholder. It gates
  `POST/GET /api/v1/api-keys` and `DELETE /api/v1/api-keys/{id}`.
- `APP_CORS_ALLOWED_ORIGINS` — comma-separated list of browser origins allowed
  to call the API with credentials (CORS). Leave unset/empty to disable
  cross-origin browser access entirely; server-to-server calls are unaffected.

## Running Locally

```bash
chmod +x ./gradlew ./start.sh
export ADMIN_API_KEY="$(openssl rand -hex 32)"   # required in any non-local environment
./start.sh
```

The app starts on `http://localhost:26986`. Swagger UI: `http://localhost:26986/docs`.

### Database & Migrations

The schema is version-controlled with **Flyway** migrations under
`src/main/resources/db/migration` (`V1__baseline.sql`, `V2__original_cost_and_foreign_keys.sql`,
`V3__reviews.sql`). Hibernate is configured with `ddl-auto=validate` and never mutates the
schema itself.

- Migrations run automatically on application startup.
- On a brand-new empty database, Flyway creates the full schema from `V1` onward.
- Flyway is configured with `baseline-on-migrate=true` / `baseline-version=1`, so pointing it at
  an existing database that was previously managed by Hibernate's `ddl-auto=update` (matching the
  `V1` baseline) is safe: `V1` is treated as already applied and only `V2+` run.
- To run migrations manually without starting the app: `./gradlew flywayMigrate` (requires the
  `spring.datasource.*` properties, e.g. via `-Dflyway.url=... -Dflyway.user=... -Dflyway.password=...`
  or by relying on the values in `application.properties`).
- Seed data lives in `src/main/resources/data.sql` (3 membership plans, 6 spaces, 5 users, 10
  historical bookings and 7 sample reviews) and is applied after migrations on every startup
  (idempotent, uses `ON CONFLICT DO NOTHING`).

## Example Usage

```bash
# 1. Mint an admin API key (replace <ADMIN_KEY> with your ADMIN_API_KEY env value)
curl -X POST http://localhost:26986/api/v1/api-keys \
  -H "Content-Type: application/json" \
  -H "X-Admin-Key: <ADMIN_KEY>" \
  -d '{"name":"admin-test","role":"ADMIN"}'

# 2. Use the returned "apiKey" value as X-API-Key for all business endpoints
curl http://localhost:26986/api/v1/spaces -H "X-API-Key: <RAW_KEY>"

# 3. If a space is fully booked, POST /bookings returns 409; join the waitlist instead
curl -X POST http://localhost:26986/api/v1/waitlist \
  -H "Content-Type: application/json" -H "X-API-Key: <RAW_KEY>" \
  -d '{"spaceId":1,"requestedStart":"2025-01-01T09:00:00","requestedEnd":"2025-01-01T11:00:00"}'

# 4. Leave a review for one of your own COMPLETED bookings
curl -X POST http://localhost:26986/api/v1/reviews \
  -H "Content-Type: application/json" -H "X-API-Key: <RAW_KEY>" \
  -d '{"bookingId":1,"rating":5,"comment":"Great space!"}'

# 5. List reviews for a space (paginated, newest first by default)
curl "http://localhost:26986/api/v1/reviews?spaceId=1&offset=0&limit=20" -H "X-API-Key: <RAW_KEY>"
```

## Tests

Automated unit tests live under `src/test/java` (`./gradlew test`), covering the booking engine
(capacity-aware conflict detection, credit-hour refunds, and the late-cancellation fee based on
the booking's original value rather than just its cash cost) and pagination limits.

API endpoints were additionally exercised end-to-end with `curl` (see `/api_tests/test_results.md`
for the full pass/fail matrix) and summarized in `api_test_report.xlsx`.

## Postman Collection

A ready-to-import Postman collection is provided at `postman/CoWork-Hub.postman_collection.json`,
covering every endpoint including Reviews, and edge cases such as: duplicate review on the same
booking (409), reviewing a booking that isn't `COMPLETED` (400), rating out of the 1-5 range (400),
and editing/deleting another member's review (403).
