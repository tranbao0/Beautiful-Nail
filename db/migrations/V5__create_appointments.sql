CREATE TABLE IF NOT EXISTS appointments (
    apmt_id     INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER NOT NULL,
    stylist_id  INTEGER NOT NULL,
    start_time  TEXT NOT NULL,
    end_time    TEXT NOT NULL,
    status      TEXT NOT NULL CHECK(status IN ('booked', 'cancelled', 'completed')),
    notes       TEXT,
    total_price REAL,
    created_at  TEXT NOT NULL,
    FOREIGN KEY (user_id)    REFERENCES users(user_id),
    FOREIGN KEY (stylist_id) REFERENCES stylists(stylist_id)
);
