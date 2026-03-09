#!/bin/bash
# Run all migrations then seed data against the SQLite DB
DB="./db/beautiful_nail.db"

echo "Running migrations..."
for f in ./db/migrations/V*.sql; do
  echo "  Applying $f"
  sqlite3 "$DB" < "$f"
done

echo "Seeding data..."
sqlite3 "$DB" < ./db/seeds/seed_data.sql

echo "Done. DB is at $DB"
