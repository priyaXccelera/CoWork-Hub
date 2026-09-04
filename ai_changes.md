COMMIT_MESSAGE: Add Review entity/endpoints with space rating aggregation, and fix pre-existing Flyway/JPA boot bug

## Features Added
- New `Review` entity (id, userId, spaceId, bookingId, rating, comment, createdAt, updatedAt, soft-delete `deleted` flag).
- `POST /api/v1/reviews` — create a review for one of the caller's own bookings. Enforces:
  rule 1 (booking must belong to the caller and have status `COMPLETED`), rule 2 (at most one
  review per booking; multiple reviews of the same space are fine via different bookings),
  rule 3 (rating 1-5 inclusive, comment optional and capped at 500 chars).
- `GET /api/v1/reviews?spaceId={id}` — paginated (offset/limit, default limit 20), sorted by
  `createdAt desc` by default.
- `GET /api/v1/reviews/{id}` — get a single (non-deleted) review.
- `PUT /api/v1/reviews/{id}` — edit own review only (rating/comment); 403 if not the owner.
- `DELETE /api/v1/reviews/{id}` — soft delete; owner or Admin only (rule 4), never hard-deleted
  (rule 5).
- `GET /api/v1/spaces` and `GET /api/v1/spaces/{id}` now include `averageRating` (rounded to 1
  decimal) and `totalReviews`, computed from non-deleted reviews only. The list endpoint uses a
  single grouped query to avoid N+1 lookups.
- `GET /api/v1/reports/space-utilization` now also includes `averageRating` per space alongside
  existing utilization metrics.
- Flyway migration `V3__reviews.sql`: creates the `reviews` table with FKs to `users`, `spaces`
  and `bookings`, a `CHECK (rating BETWEEN 1 AND 5)` constraint, and a partial unique index
  `uq_reviews_booking_id_active` on `booking_id` (only for non-deleted rows) enforcing "one review
  per booking" at the DB layer too.
- Seed data: 5 additional COMPLETED bookings (ids 500001-500005, deliberately using a high id
  range — see "Files Modified" note on `data.sql`) plus 7 sample reviews across 6 different
  spaces and 5 different users, including two reviews on the same space (space 3) from two
  different bookings to demonstrate rule 2's "multiple reviews per space, one per booking" case.
- Postman collection (`postman/CoWork-Hub.postman_collection.json`) covering Reviews CRUD and the
  edge cases: duplicate review on the same booking, review on a non-COMPLETED booking, rating out
  of range, and editing/deleting another member's review — plus the updated Spaces/Reports
  endpoints.
- Swagger/OpenAPI docs updated automatically via `@Operation`/`@Tag` annotations on the new
  `ReviewController` (springdoc generates `/api-docs` and `/docs` dynamically; verified the new
  `/api/v1/reviews` paths appear).
- README updated: architecture, business rules, API table, Postman section, seed data counts,
  and all port references (20330 → 26986).

## Files Modified
- `src/main/resources/application.properties` — `server.port` 20330 → 26986; removed
  `spring.jpa.defer-datasource-initialization=true` (root cause of a pre-existing boot bug, see
  below).
- `start.sh` — default `SERVER_PORT` 20330 → 26986.
- `src/main/java/com/example/app/config/SecurityConfig.java` — no functional change to the
  security rules, but see `ApiKeyFilter.java` note below (kept unchanged otherwise).
- `src/main/java/com/example/app/security/ApiKeyFilter.java` — constructor parameters
  `ApiKeyRepository`/`UserRepository` marked `@Lazy` to prevent Spring Boot's
  `ServletContextInitializerBeans` from eagerly instantiating JPA infrastructure (and therefore
  `entityManagerFactory`) before Flyway has run.
- `src/main/java/com/example/app/dto/SpaceDtos.java` — `SpaceResponse` gained `averageRating`
  and `totalReviews`.
- `src/main/java/com/example/app/service/SpaceService.java` — injects `ReviewRepository`;
  `list()` now does one grouped rating-stats query per page instead of N+1; `get()`/`create()`/
  `update()` compute rating stats per space.
- `src/main/java/com/example/app/dto/ReportDtos.java` — `SpaceUtilizationResponse` gained
  `averageRating`.
- `src/main/java/com/example/app/service/ReportService.java` — injects `ReviewRepository`;
  `spaceUtilization()` now also returns `averageRating` per space.
- `src/main/resources/data.sql` — added 5 additional COMPLETED bookings and 7 sample reviews.
  The additional bookings intentionally use a high, fixed id range (500001+) rather than
  continuing 6, 7, 8... because the `bookings` table already accumulates organically-created
  rows from normal `POST /api/v1/bookings` usage whose ids start low; reusing small sequential
  ids risked `ON CONFLICT DO NOTHING` silently skipping the seed rows on a non-fresh database
  (this was caught and fixed during manual testing against the shared dev database).
- `README.md` — documented the Reviews feature, new endpoints, updated API table, Postman
  collection reference, updated port references, updated seed-data counts, updated migration
  list.
- `api_tests/test_results.md` — appended "Iteration 1 — Reviews feature" section with the new
  endpoint test matrix and a note on the pre-existing boot bug found and fixed.

## Files Added
- `src/main/java/com/example/app/entity/Review.java` — the Review JPA entity.
- `src/main/java/com/example/app/repository/ReviewRepository.java` — includes a grouped
  projection query (`findRatingStatsBySpaceIds`) for the Spaces list endpoint, plus average
  rating / count queries used by the Space and Report endpoints.
- `src/main/java/com/example/app/dto/ReviewDtos.java` — `ReviewRequest`, `ReviewUpdateRequest`,
  `ReviewResponse`.
- `src/main/java/com/example/app/service/ReviewService.java` — business logic for create/list/
  get/update/soft-delete, enforcing all 5 business rules.
- `src/main/java/com/example/app/controller/ReviewController.java` — the 5 new
  `/api/v1/reviews` endpoints.
- `src/main/resources/db/migration/V3__reviews.sql` — Flyway migration creating the `reviews`
  table, foreign keys, check constraint, and partial unique index.
- `postman/CoWork-Hub.postman_collection.json` — Postman collection for Reviews (+ affected
  Spaces/Reports endpoints) including edge cases.

## Secrets Moved
None found beyond what was already externalized (`admin.api-key` was already
`${ADMIN_API_KEY:...}` in `application.properties`; no new hardcoded secrets were introduced).

## DB URLs Resolved
- `jdbc:postgresql://localhost:5432/gen_4d326c90fe72` → unchanged (pre-resolved as already
  working; only `server.port` was updated to 26986).

## Pre-existing Bug Fixed (found while verifying the server boots)
The application failed to start with:
`Circular depends-on relationship between 'flyway' and 'entityManagerFactory'`
This reproduced identically on the original `main` branch (verified before making any Review
changes), so it was not caused by this feature work, but it blocked all endpoint testing and was
fixed as part of this change:
1. `ApiKeyFilter` is a `@Component` that also implements `jakarta.servlet.Filter` (via
   `OncePerRequestFilter`), so Spring Boot's `ServletContextInitializerBeans` auto-detects and
   eagerly instantiates it (and its JPA repository dependencies) during Tomcat context startup,
   before Flyway has had a chance to run. Fixed by injecting its repositories as `@Lazy`.
2. `spring.jpa.defer-datasource-initialization=true` forces the SQL (`data.sql`) initializer to
   depend on `entityManagerFactory`, which — in this combination of Spring Boot 3.3.4 and
   Flyway 10.x — creates a genuine circular dependency between `flyway` and
   `entityManagerFactory`. Removed the property; Spring Boot already adds an automatic
   depends-on from the SQL initializer to Flyway regardless, so `data.sql` is still guaranteed to
   run after migrations.

## Compilation Result
PASSED (`./gradlew compileJava -q` and `./gradlew bootJar -q` both succeed with zero errors).
Server verified booting successfully on port 26986, Flyway migrations V1-V3 applying cleanly,
and all 20 new Reviews-related endpoint/business-rule tests passing (see
`api_tests/test_results.md`, Iteration 1).
