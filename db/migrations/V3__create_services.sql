CREATE TABLE IF NOT EXISTS services (
    service_id   INTEGER PRIMARY KEY AUTOINCREMENT,
    name         TEXT NOT NULL,
    price_min    REAL NOT NULL,
    price_max    REAL NOT NULL,
    est_duration INTEGER NOT NULL
);
