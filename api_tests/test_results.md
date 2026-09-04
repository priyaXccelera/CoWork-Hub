# CoWork Hub — API Test Results (Iteration 0 / Final)

Base URL: `http://localhost:20330`
All tests executed via inline `curl` commands against the running server.
No FAILED/SKIPPED endpoints were found — 0 fix iterations were required.

| # | Method | Endpoint | Test | Status Code | Result |
|---|---|---|---|---|---|
| 1 | POST | /api/v1/api-keys | Mint admin key with X-Admin-Key | 201 | PASSED |
| 2 | GET | /api/v1/api-keys | List keys with X-Admin-Key | 200 | PASSED |
| 3 | DELETE | /api/v1/api-keys/{id} | Revoke key with X-Admin-Key | 204 | PASSED |
| 4 | GET | /api/v1/api-keys | Missing X-Admin-Key (expect 401) | 401 | PASSED |
| 5 | GET | /api/v1/api-keys | Wrong X-Admin-Key (expect 401) | 401 | PASSED |
| 6 | GET | /api/v1/spaces | No X-API-Key (expect 401/403) | 403 | PASSED |
| 7 | GET | /api/v1/spaces | List spaces with valid key | 200 | PASSED |
| 8 | POST | /api/v1/spaces | Create space (Admin) | 201 | PASSED |
| 9 | GET | /api/v1/spaces/{id} | Get single space | 200 | PASSED |
| 10 | GET | /api/v1/spaces?type=DESK | Filter by type | 200 | PASSED |
| 11 | PUT | /api/v1/spaces/{id} | Update space | 200 | PASSED |
| 12 | DELETE | /api/v1/spaces/{id} | Soft delete space | 204 | PASSED |
| 13 | GET | /api/v1/spaces/{id} | Get after delete (expect 404) | 404 | PASSED |
| 14 | POST | /api/v1/spaces | Missing required fields (expect 400) | 400 | PASSED |
| 15 | GET | /api/v1/membership-plans | List plans | 200 | PASSED |
| 16 | POST | /api/v1/membership-plans | Create plan (Admin) | 201 | PASSED |
| 17 | GET | /api/v1/membership-plans/{id} | Get single plan | 200 | PASSED |
| 18 | DELETE | /api/v1/membership-plans/{id} | Delete plan | 204 | PASSED |
| 19 | GET | /api/v1/membership-plans/{id} | Get after delete (expect 404) | 404 | PASSED |
| 20 | GET | /api/v1/users | List users (Admin) | 200 | PASSED |
| 21 | POST | /api/v1/users | Create user + auto-mint linked API key | 201 | PASSED |
| 22 | GET | /api/v1/users/{id} | Get single user | 200 | PASSED |
| 23 | PUT | /api/v1/users/{id} | Update user | 200 | PASSED |
| 24 | DELETE | /api/v1/users/{id} | Soft delete user | 204 | PASSED |
| 25 | GET | /api/v1/users/{id} | Get after delete (expect 404) | 404 | PASSED |
| 26 | POST | /api/v1/users | Invalid email (expect 400) | 400 | PASSED |
| 27 | POST | /api/v1/bookings | Member books space (no plan, full hourly rate) | 201 | PASSED (cost = hours × hourlyRate verified) |
| 28 | POST | /api/v1/bookings | endTime before startTime (expect 4xx) | 409 | PASSED |
| 29 | POST | /api/v1/bookings | Member supplies foreign userId (ignored, forced to own id) | 201 | PASSED |
| 30 | POST | /api/v1/bookings | Overlapping booking on same space → auto-WAITLISTED + Waitlist row created | 201 | PASSED |
| 31 | POST | /api/v1/bookings/{id}/cancel | Free cancellation (>2h before start), credit refunded | 200 | PASSED |
| 32 | (auto) | — | Waitlist auto-promotion after cancellation | 200 | PASSED (booking moved WAITLISTED→CONFIRMED) |
| 33 | GET | /api/v1/waitlist | List waitlist entries (Admin) | 200 | PASSED |
| 34 | GET | /api/v1/bookings/{id} | Get single booking | 200 | PASSED |
| 35 | GET | /api/v1/bookings?status=CONFIRMED | Filter by status | 200 | PASSED |
| 36 | GET | /api/v1/bookings?offset=0&limit=2 | Offset/limit pagination | 200 | PASSED |
| 37 | POST | /api/v1/bookings | Overage billing (plan credit hours exhausted, 5h × $5 overage) | 201 | PASSED (cost = 25.00, creditHoursUsed = 10.0 verified) |
| 38 | POST | /api/v1/bookings/{id}/cancel | Late cancellation (<2h before start) → 25% fee | 200 | PASSED (costCharged = 9.17 × 0.25 = 2.29 verified) |
| 39 | POST | /api/v1/invoices/generate | Generate monthly invoice (Admin) | 201 | PASSED (totalAmount/overage verified) |
| 40 | POST | /api/v1/invoices/generate | Member calls admin-only endpoint (expect 403) | 403 | PASSED |
| 41 | GET | /api/v1/invoices | List invoices | 200 | PASSED |
| 42 | GET | /api/v1/invoices/{id} | Get single invoice | 200 | PASSED |
| 43 | GET | /api/v1/reports/space-utilization | Weekly utilization % per space (Admin) | 200 | PASSED |
| 44 | GET | /api/v1/reports/revenue?month=YYYY-MM | Monthly revenue (Admin) | 200 | PASSED |
| 45 | GET | /api/v1/reports/top-members?limit=5 | Top active members (Admin) | 200 | PASSED |
| 46 | GET | /api/v1/reports/revenue | Member calls admin-only report (expect 403) | 403 | PASSED |
| 47 | (auth) | any | Revoked key reused (expect 401/403) | 403 | PASSED |
| 48 | GET | /docs | Swagger UI (redirects to /swagger-ui/index.html) | 302→200 | PASSED |
| 49 | GET | /api-docs | OpenAPI JSON | 200 | PASSED |
| 50 | GET | /actuator/health | Health check | 200 | PASSED |

## Notes
- All IDs created during testing (test spaces, plans, users, bookings, API keys)
  were deleted/cancelled/revoked before generating the final report; seed data
  (users 1-5, plans 1-3, spaces 1-6, bookings 1-5) was left untouched.
- Business rules verified against actual response bodies, not just status codes:
  cost calculation (credit-hour deduction + overage), free vs. late cancellation
  fee (25%), and waitlist auto-promotion on cancellation.
- 0 iterations of the fix loop were required — baseline run passed all endpoints.
