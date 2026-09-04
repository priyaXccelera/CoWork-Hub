-- Adds the originalCost column used to permanently record what a booking was worth at creation
-- time (see Booking#originalCost), and adds proper foreign-key constraints on relationships that
-- were previously stored as plain, unconstrained id columns.

ALTER TABLE bookings ADD COLUMN IF NOT EXISTS original_cost NUMERIC(38,2) NOT NULL DEFAULT 0;

-- Best-effort backfill for historical rows created before this column existed: fall back to the
-- cash amount actually charged, since we have no better record of the original valuation.
UPDATE bookings SET original_cost = cost_charged WHERE original_cost = 0;

-- Foreign keys are added defensively: each one is attempted independently so that unexpected
-- orphan data in one table cannot prevent the others (or the rest of the application) from
-- starting up. Any skipped constraint is reported via a NOTICE in the server/migration log.
DO $$
BEGIN
  BEGIN
    ALTER TABLE bookings ADD CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users(id);
  EXCEPTION WHEN duplicate_object THEN NULL;
  WHEN others THEN RAISE NOTICE 'Skipping fk_bookings_user: %', SQLERRM;
  END;

  BEGIN
    ALTER TABLE bookings ADD CONSTRAINT fk_bookings_space FOREIGN KEY (space_id) REFERENCES spaces(id);
  EXCEPTION WHEN duplicate_object THEN NULL;
  WHEN others THEN RAISE NOTICE 'Skipping fk_bookings_space: %', SQLERRM;
  END;

  BEGIN
    ALTER TABLE waitlists ADD CONSTRAINT fk_waitlists_user FOREIGN KEY (user_id) REFERENCES users(id);
  EXCEPTION WHEN duplicate_object THEN NULL;
  WHEN others THEN RAISE NOTICE 'Skipping fk_waitlists_user: %', SQLERRM;
  END;

  BEGIN
    ALTER TABLE waitlists ADD CONSTRAINT fk_waitlists_space FOREIGN KEY (space_id) REFERENCES spaces(id);
  EXCEPTION WHEN duplicate_object THEN NULL;
  WHEN others THEN RAISE NOTICE 'Skipping fk_waitlists_space: %', SQLERRM;
  END;

  BEGIN
    ALTER TABLE waitlists ADD CONSTRAINT fk_waitlists_booking FOREIGN KEY (booking_id) REFERENCES bookings(id);
  EXCEPTION WHEN duplicate_object THEN NULL;
  WHEN others THEN RAISE NOTICE 'Skipping fk_waitlists_booking: %', SQLERRM;
  END;

  BEGIN
    ALTER TABLE invoices ADD CONSTRAINT fk_invoices_user FOREIGN KEY (user_id) REFERENCES users(id);
  EXCEPTION WHEN duplicate_object THEN NULL;
  WHEN others THEN RAISE NOTICE 'Skipping fk_invoices_user: %', SQLERRM;
  END;

  BEGIN
    ALTER TABLE api_keys ADD CONSTRAINT fk_api_keys_user FOREIGN KEY (user_id) REFERENCES users(id);
  EXCEPTION WHEN duplicate_object THEN NULL;
  WHEN others THEN RAISE NOTICE 'Skipping fk_api_keys_user: %', SQLERRM;
  END;
END $$;

CREATE INDEX IF NOT EXISTS idx_bookings_space_id ON bookings (space_id);
CREATE INDEX IF NOT EXISTS idx_bookings_user_id ON bookings (user_id);
CREATE INDEX IF NOT EXISTS idx_waitlists_space_id_status ON waitlists (space_id, status);
CREATE INDEX IF NOT EXISTS idx_invoices_user_id ON invoices (user_id);
CREATE INDEX IF NOT EXISTS idx_api_keys_user_id ON api_keys (user_id);
