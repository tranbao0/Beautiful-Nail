package com.beautifulnail.scheduler.repository;

import com.beautifulnail.scheduler.model.Availability;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Collections;

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

    public List<Availability> findAvailableByDateAndStylist(String date, Long stylistId) {
        return jdbc.query(
            "SELECT * FROM availability_slots WHERE date(start_time) = ? AND stylist_id = ? AND is_available = 1",
            rowMapper, date, stylistId
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

    public List<Availability> findByDateAndStylistAndServices(String date, Long stylistId, List<Long> serviceIds) {
        StringBuilder sql = new StringBuilder(
            "SELECT * FROM availability_slots WHERE is_available = 1 AND date(start_time) = ?");
        List<Object> params = new ArrayList<>();
        params.add(date);

        if (stylistId != null) {
            sql.append(" AND stylist_id = ?");
            params.add(stylistId);
        }
        if (serviceIds != null && !serviceIds.isEmpty()) {
            sql.append(" AND stylist_id IN (SELECT stylist_id FROM stylist_services WHERE service_id = ?");
            params.add(serviceIds.get(0));
            for (int i = 1; i < serviceIds.size(); i++) {
                sql.append(" INTERSECT SELECT stylist_id FROM stylist_services WHERE service_id = ?");
                params.add(serviceIds.get(i));
            }
            sql.append(")");
        }
        return jdbc.query(sql.toString(), rowMapper, params.toArray());
    }

    public List<String> findAvailableDatesAfter(String fromDate, Long stylistId, List<Long> serviceIds) {
        StringBuilder sql = new StringBuilder(
            "SELECT DISTINCT date(start_time) as d FROM availability_slots WHERE is_available = 1 AND date(start_time) > ?");
        List<Object> params = new ArrayList<>();
        params.add(fromDate);

        if (stylistId != null) {
            sql.append(" AND stylist_id = ?");
            params.add(stylistId);
        }
        if (serviceIds != null && !serviceIds.isEmpty()) {
            sql.append(" AND stylist_id IN (SELECT stylist_id FROM stylist_services WHERE service_id = ?");
            params.add(serviceIds.get(0));
            for (int i = 1; i < serviceIds.size(); i++) {
                sql.append(" INTERSECT SELECT stylist_id FROM stylist_services WHERE service_id = ?");
                params.add(serviceIds.get(i));
            }
            sql.append(")");
        }
        sql.append(" ORDER BY d");

        return jdbc.queryForList(sql.toString(), String.class, params.toArray());
    }
}
