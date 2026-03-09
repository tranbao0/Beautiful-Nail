CREATE TABLE IF NOT EXISTS availability_slots (
    slot_id      INTEGER PRIMARY KEY AUTOINCREMENT,
    stylist_id   INTEGER NOT NULL,
    start_time   TEXT NOT NULL,
    end_time     TEXT NOT NULL,
    is_available INTEGER NOT NULL DEFAULT 1,
    FOREIGN KEY (stylist_id) REFERENCES stylists(stylist_id)
);
