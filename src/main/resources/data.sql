-- Membership plans
INSERT INTO membership_plans (id, name, monthly_price, included_credit_hours, overage_rate_per_hour, created_at, updated_at)
VALUES
    (1, 'Basic', 49.00, 10.0, 5.00, now(), now()),
    (2, 'Pro', 99.00, 25.0, 4.00, now(), now()),
    (3, 'Enterprise', 199.00, 60.0, 3.00, now(), now())
ON CONFLICT (id) DO NOTHING;

-- Spaces
INSERT INTO spaces (id, name, type, capacity, hourly_rate, is_active, deleted, created_at, updated_at)
VALUES
    (1, 'Hot Desk A1', 'DESK', 1, 5.00, true, false, now(), now()),
    (2, 'Hot Desk A2', 'DESK', 1, 5.00, true, false, now(), now()),
    (3, 'Meeting Room Falcon', 'MEETING_ROOM', 8, 20.00, true, false, now(), now()),
    (4, 'Meeting Room Eagle', 'MEETING_ROOM', 6, 15.00, true, false, now(), now()),
    (5, 'Private Cabin 1', 'PRIVATE_CABIN', 2, 12.00, true, false, now(), now()),
    (6, 'Private Cabin 2', 'PRIVATE_CABIN', 4, 18.00, true, false, now(), now())
ON CONFLICT (id) DO NOTHING;

-- Users
INSERT INTO users (id, name, email, role, api_key, membership_plan_id, credit_hours_remaining, status, deleted, created_at, updated_at)
VALUES
    (1, 'Alice Admin', 'alice.admin@coworkhub.test', 'ADMIN', 'seed-admin-key-1', 3, 60.0, 'ACTIVE', false, now(), now()),
    (2, 'Bob Member', 'bob.member@coworkhub.test', 'MEMBER', 'seed-member-key-2', 1, 10.0, 'ACTIVE', false, now(), now()),
    (3, 'Carol Member', 'carol.member@coworkhub.test', 'MEMBER', 'seed-member-key-3', 2, 25.0, 'ACTIVE', false, now(), now()),
    (4, 'Dave Member', 'dave.member@coworkhub.test', 'MEMBER', 'seed-member-key-4', 1, 10.0, 'ACTIVE', false, now(), now()),
    (5, 'Erin Member', 'erin.member@coworkhub.test', 'MEMBER', 'seed-member-key-5', NULL, 0.0, 'ACTIVE', false, now(), now())
ON CONFLICT (id) DO NOTHING;

-- Bookings (existing history)
INSERT INTO bookings (id, user_id, space_id, start_time, end_time, status, cost_charged, credit_hours_used, created_at, updated_at)
VALUES
    (1, 2, 1, now() - interval '5 days', now() - interval '5 days' + interval '2 hours', 'COMPLETED', 0.00, 2.0, now() - interval '5 days', now() - interval '5 days'),
    (2, 3, 3, now() - interval '3 days', now() - interval '3 days' + interval '3 hours', 'COMPLETED', 0.00, 3.0, now() - interval '3 days', now() - interval '3 days'),
    (3, 4, 5, now() - interval '2 days', now() - interval '2 days' + interval '1 hours', 'CANCELLED', 3.00, 0.0, now() - interval '2 days', now() - interval '2 days'),
    (4, 2, 4, now() + interval '2 days', now() + interval '2 days' + interval '2 hours', 'CONFIRMED', 0.00, 2.0, now(), now()),
    (5, 3, 6, now() + interval '4 days', now() + interval '4 days' + interval '4 hours', 'CONFIRMED', 4.00, 4.0, now(), now())
ON CONFLICT (id) DO NOTHING;

-- Additional COMPLETED bookings backing the sample reviews below, across different users/spaces.
-- Deliberately use a high, fixed id range (500001+) instead of continuing 6, 7, 8... because this
-- table already accumulates organically-created bookings (via POST /api/v1/bookings) whose
-- auto-generated ids start from wherever the identity sequence is at the time; using small
-- sequential ids here could collide with real booking rows created by normal app usage and get
-- silently skipped by ON CONFLICT DO NOTHING, leaving the review seed rows below pointing at the
-- wrong booking/user/space. The high range guarantees these seed rows are always the ones present.
INSERT INTO bookings (id, user_id, space_id, start_time, end_time, status, cost_charged, credit_hours_used, created_at, updated_at)
VALUES
    (500001, 4, 2, now() - interval '6 days', now() - interval '6 days' + interval '1 hours', 'COMPLETED', 5.00, 0.0, now() - interval '6 days', now() - interval '6 days'),
    (500002, 5, 4, now() - interval '7 days', now() - interval '7 days' + interval '2 hours', 'COMPLETED', 30.00, 0.0, now() - interval '7 days', now() - interval '7 days'),
    (500003, 2, 5, now() - interval '4 days', now() - interval '4 days' + interval '1 hours', 'COMPLETED', 12.00, 0.0, now() - interval '4 days', now() - interval '4 days'),
    (500004, 3, 6, now() - interval '8 days', now() - interval '8 days' + interval '3 hours', 'COMPLETED', 54.00, 0.0, now() - interval '8 days', now() - interval '8 days'),
    (500005, 4, 3, now() - interval '9 days', now() - interval '9 days' + interval '2 hours', 'COMPLETED', 40.00, 0.0, now() - interval '9 days', now() - interval '9 days')
ON CONFLICT (id) DO NOTHING;

-- Reviews (sample data) — each references one of the COMPLETED bookings above; space 3 has two
-- reviews from two different bookings (booking 2 and booking 500005) to demonstrate that multiple
-- reviews for the same space are allowed as long as they come from different bookings.
INSERT INTO reviews (id, user_id, space_id, booking_id, rating, comment, deleted, created_at, updated_at)
VALUES
    (1, 2, 1, 1, 5, 'Great desk, very quiet and well lit.', false, now() - interval '4 days', now() - interval '4 days'),
    (2, 3, 3, 2, 4, 'Good meeting room, could use better lighting.', false, now() - interval '2 days', now() - interval '2 days'),
    (3, 4, 2, 500001, 3, 'Decent desk but a bit noisy in the afternoon.', false, now() - interval '5 days', now() - interval '5 days'),
    (4, 5, 4, 500002, 5, 'Excellent room for client meetings, highly recommend.', false, now() - interval '6 days', now() - interval '6 days'),
    (5, 2, 5, 500003, 4, NULL, false, now() - interval '3 days', now() - interval '3 days'),
    (6, 3, 6, 500004, 2, 'Cabin was smaller than expected for the price.', false, now() - interval '7 days', now() - interval '7 days'),
    (7, 4, 3, 500005, 5, 'Second time booking this room, still great!', false, now() - interval '8 days', now() - interval '8 days')
ON CONFLICT (id) DO NOTHING;

-- Keep identity sequences in sync with the explicit ids inserted above
SELECT setval(pg_get_serial_sequence('membership_plans', 'id'), COALESCE((SELECT MAX(id) FROM membership_plans), 1));
SELECT setval(pg_get_serial_sequence('spaces', 'id'), COALESCE((SELECT MAX(id) FROM spaces), 1));
SELECT setval(pg_get_serial_sequence('users', 'id'), COALESCE((SELECT MAX(id) FROM users), 1));
-- Bookings intentionally excluded from the sequence bump: organically-created bookings (ids well
-- below 500000) should keep incrementing normally rather than jumping past the high seed range.
SELECT setval(pg_get_serial_sequence('reviews', 'id'), COALESCE((SELECT MAX(id) FROM reviews), 1));
