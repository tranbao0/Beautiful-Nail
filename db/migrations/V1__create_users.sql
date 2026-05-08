CREATE TABLE IF NOT EXISTS users (
    user_id      INTEGER PRIMARY KEY AUTOINCREMENT,
    first_name   TEXT NOT NULL,
    m_init       TEXT,
    last_name    TEXT NOT NULL,
    email        TEXT NOT NULL UNIQUE,
    phone        TEXT,
    role         TEXT NOT NULL CHECK(role IN ('customer', 'receptionist')),
    password_hash TEXT NOT NULL
);
