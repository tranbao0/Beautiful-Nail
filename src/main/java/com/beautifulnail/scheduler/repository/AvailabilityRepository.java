package com.beautifulnail.scheduler.repository;

import com.beautifulnail.scheduler.model.Availability;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AvailabilityRepository {

    private final JdbcTemplate jdbc;

    public AvailabilityRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Availability> rowMapper = (rs, rowNum) -> {
        Availability a = new Availability();
        a.setSlotId(rs.getLong("slot_id"));
        a.setStylistId(rs.getLong("stylist_id"));
        a.setStartTime(LocalDateTime.parse(rs.getString("start_time")));
        a.setEndTime(LocalDateTime.parse(rs.getString("end_time")));
        a.setAvailable(rs.getInt("is_available") == 1);
        return a;
    };

    public List<Availability> findAll() {
        return jdbc.query("SELECT * FROM availability_slots", rowMapper);
    }

    public List<Availability> findAvailableByStylist(Long stylistId) {
        return jdbc.query(
            "SELECT * FROM availability_slots WHERE stylist_id = ? AND is_available = 1",
            rowMapper, stylistId
        );
    }

    public List<Availability> findAvailableByDate(String date) {
        return jdbc.query(
            "SELECT * FROM availability_slots WHERE date(start_time) = ? AND is_available = 1",
            rowMapper, date
        );
    }

    public Optional<Availability> findById(Long slotId) {
        List<Availability> results = jdbc.query(
            "SELECT * FROM availability_slots WHERE slot_id = ?", rowMapper, slotId
        );
        return results.stream().findFirst();
    }

    public int save(Availability slot) {
        return jdbc.update(
            "INSERT INTO availability_slots (stylist_id, start_time, end_time, is_available) VALUES (?, ?, ?, ?)",
            slot.getStylistId(), slot.getStartTime().toString(),
            slot.getEndTime().toString(), slot.isAvailable() ? 1 : 0
        );
    }

    public int markUnavailable(Long slotId) {
        // AND is_available = 1 ensures atomically that only one transaction wins (optimistic check)
        return jdbc.update(
            "UPDATE availability_slots SET is_available = 0 WHERE slot_id = ? AND is_available = 1", slotId
        );
    }

    public int markAvailable(Long slotId) {
        return jdbc.update(
            "UPDATE availability_slots SET is_available = 1 WHERE slot_id = ?", slotId
        );
    }
}
