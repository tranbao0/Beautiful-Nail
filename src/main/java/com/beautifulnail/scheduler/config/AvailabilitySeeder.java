package com.beautifulnail.scheduler.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class AvailabilitySeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AvailabilitySeeder.class);

    private final JdbcTemplate jdbc;

    public AvailabilitySeeder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Re-seeding availability slots for next 30 days...");
        jdbc.update("DELETE FROM availability_slots");

        List<Object[]> rows = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int day = 0; day < 30; day++) {
            LocalDate date = today.plusDays(day);
            DayOfWeek dow = date.getDayOfWeek();

            // Jenny (1) — Chrome Powder, Gel Extensions (rare)
            // Tue, Wed, Fri  |  3 slots, mid-day oriented
            if (Set.of(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY).contains(dow)) {
                slot(rows, 1, date, 10, 30, 11, 15);
                slot(rows, 1, date, 12,  0, 12, 45);
                slot(rows, 1, date, 14, 30, 15, 15);
            }

            // Mia (2) — Full-Service generalist
            // Mon, Wed, Sat  |  3 slots, spread across the day
            if (Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY).contains(dow)) {
                slot(rows, 2, date,  9,  0,  9, 50);
                slot(rows, 2, date, 11, 30, 12, 20);
                slot(rows, 2, date, 15,  0, 15, 50);
            }

            // Sara (3) — Nail Art, Acrylics, Gel
            // Tue, Thu  |  2 slots, morning + afternoon
            if (Set.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY).contains(dow)) {
                slot(rows, 3, date, 10,  0, 11,  0);
                slot(rows, 3, date, 14,  0, 15,  0);
            }

            // Priya (4) — Manicure Specialist
            // Wed, Fri  |  3 slots, mornings only
            if (Set.of(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY).contains(dow)) {
                slot(rows, 4, date,  9,  0,  9, 45);
                slot(rows, 4, date, 10, 30, 11, 15);
                slot(rows, 4, date, 12,  0, 12, 45);
            }

            // Rosa (5) — Spa & Color Design
            // Tue, Thu, Sat  |  3 slots, afternoons only
            if (Set.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY).contains(dow)) {
                slot(rows, 5, date, 13,  0, 14,  0);
                slot(rows, 5, date, 15,  0, 16,  0);
                slot(rows, 5, date, 16, 30, 17, 30);
            }

            // Yuki (6) — 3D Nail Art (very rare, very limited)
            // Mon, Thu  |  1 slot only
            if (Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY).contains(dow)) {
                slot(rows, 6, date, 10,  0, 11, 30);
            }

            // Chloe (7) — Gel & Acrylic Specialist (densest schedule)
            // Mon, Fri, Sat  |  5 slots
            if (Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY).contains(dow)) {
                slot(rows, 7, date,  9,  0, 10,  0);
                slot(rows, 7, date, 10, 30, 11, 30);
                slot(rows, 7, date, 12,  0, 13,  0);
                slot(rows, 7, date, 14,  0, 15,  0);
                slot(rows, 7, date, 15, 30, 16, 30);
            }
        }

        jdbc.batchUpdate(
            "INSERT INTO availability_slots (stylist_id, start_time, end_time, is_available) VALUES (?, ?, ?, 1)",
            rows
        );
        log.info("Seeded {} availability slots across 30 days.", rows.size());
    }

    private void slot(List<Object[]> rows, int stylistId, LocalDate date,
                      int sh, int sm, int eh, int em) {
        rows.add(new Object[]{
            stylistId,
            date.atTime(sh, sm).toString(),
            date.atTime(eh, em).toString()
        });
    }
}
