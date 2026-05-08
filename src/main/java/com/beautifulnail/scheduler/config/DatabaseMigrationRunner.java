package com.beautifulnail.scheduler.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Applies pending schema changes and data fixes on every startup.
 * All operations are idempotent — safe to run against an already-migrated DB.
 * Runs before AvailabilitySeeder (Order 1).
 */
@Component
@Order(1)
public class DatabaseMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationRunner.class);
    private static final String PROPER_HASH =
            "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8"; // SHA-256("password")

    private final JdbcTemplate jdbc;

    public DatabaseMigrationRunner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        applyGuestBookingSchema();
        fixLegacyPasswordHashes();
    }

    /**
     * V8: Makes user_id nullable and adds guest contact columns.
     * Skipped if guest_name column already exists.
     */
    private void applyGuestBookingSchema() {
        List<String> columns = jdbc.query(
                "PRAGMA table_info(appointments)",
                (rs, rowNum) -> rs.getString("name"));

        if (columns.contains("guest_name")) {
            log.debug("V8 schema already applied, skipping.");
            return;
        }

        log.info("Applying V8: adding guest booking support to appointments table...");
        jdbc.execute("PRAGMA foreign_keys = OFF");
        jdbc.execute("""
                CREATE TABLE appointments_new (
                    apmt_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id     INTEGER,
                    stylist_id  INTEGER NOT NULL,
                    start_time  TEXT NOT NULL,
                    end_time    TEXT NOT NULL,
                    status      TEXT NOT NULL CHECK(status IN ('booked','cancelled','completed')),
                    notes       TEXT,
                    total_price REAL,
                    created_at  TEXT NOT NULL,
                    guest_name  TEXT,
                    guest_email TEXT,
                    guest_phone TEXT,
                    FOREIGN KEY (stylist_id) REFERENCES stylists(stylist_id)
                )""");
        jdbc.execute("""
                INSERT INTO appointments_new
                SELECT apmt_id, user_id, stylist_id, start_time, end_time,
                       status, notes, total_price, created_at, NULL, NULL, NULL
                FROM appointments""");
        jdbc.execute("DROP TABLE appointments");
        jdbc.execute("ALTER TABLE appointments_new RENAME TO appointments");
        jdbc.execute("PRAGMA foreign_keys = ON");
        log.info("V8 applied successfully.");
    }

    /**
     * Replaces any leftover placeholder hashes (hashed_pw_*) with the proper
     * SHA-256 digest of "password" so seed accounts are always usable.
     */
    private void fixLegacyPasswordHashes() {
        int updated = jdbc.update(
                "UPDATE users SET password_hash = ? WHERE password_hash LIKE 'hashed_pw_%'",
                PROPER_HASH);
        if (updated > 0) {
            log.info("Fixed {} seed account(s) with legacy placeholder password hash.", updated);
        }
    }
}
