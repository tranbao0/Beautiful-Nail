-- V8: Support guest bookings
-- Recreates appointments without NOT NULL on user_id and adds guest contact columns.
-- SQLite requires a table rebuild to drop a NOT NULL constraint.

PRAGMA foreign_keys = OFF;

CREATE TABLE appointments_new (
    apmt_id     INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER,                          -- NULL for guest bookings
    stylist_id  INTEGER NOT NULL,
    start_time  TEXT NOT NULL,
    end_time    TEXT NOT NULL,
    status      TEXT NOT NULL CHECK(status IN ('booked', 'cancelled', 'completed')),
    notes       TEXT,
    total_price REAL,
    created_at  TEXT NOT NULL,
    guest_name  TEXT,                             -- populated only for guests
    guest_email TEXT,
    guest_phone TEXT,
    FOREIGN KEY (stylist_id) REFERENCES stylists(stylist_id)
);

INSERT INTO appointments_new
    SELECT apmt_id, user_id, stylist_id, start_time, end_time,
           status, notes, total_price, created_at, NULL, NULL, NULL
    FROM appointments;

DROP TABLE appointments;
ALTER TABLE appointments_new RENAME TO appointments;

PRAGMA foreign_keys = ON;
