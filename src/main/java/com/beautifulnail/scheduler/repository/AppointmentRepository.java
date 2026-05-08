package com.beautifulnail.scheduler.repository;

import com.beautifulnail.scheduler.model.Appointment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AppointmentRepository {

    private final JdbcTemplate jdbc;

    public AppointmentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Appointment> rowMapper = (rs, rowNum) -> {
        Appointment a = new Appointment();
        a.setApmtId(rs.getLong("apmt_id"));
        // rs.getObject(col, Long.class) is unsupported by SQLite JDBC for NULL — use wasNull() instead
        long rawUserId = rs.getLong("user_id");
        a.setUserId(rs.wasNull() ? null : rawUserId);
        a.setStylistId(rs.getLong("stylist_id"));
        a.setStartTime(LocalDateTime.parse(rs.getString("start_time")));
        a.setEndTime(LocalDateTime.parse(rs.getString("end_time")));
        a.setStatus(rs.getString("status"));
        a.setNotes(rs.getString("notes"));
        a.setTotalPrice(rs.getDouble("total_price"));
        a.setCreatedAt(LocalDateTime.parse(rs.getString("created_at")));
        a.setGuestName(rs.getString("guest_name"));
        a.setGuestEmail(rs.getString("guest_email"));
        a.setGuestPhone(rs.getString("guest_phone"));
        return a;
    };

    public List<Appointment> findAll() {
        return jdbc.query("SELECT * FROM appointments", rowMapper);
    }

    public List<Appointment> findByUserId(Long userId) {
        return jdbc.query("SELECT * FROM appointments WHERE user_id = ?", rowMapper, userId);
    }

    public List<Appointment> findByStylistId(Long stylistId) {
        return jdbc.query("SELECT * FROM appointments WHERE stylist_id = ?", rowMapper, stylistId);
    }

    public Optional<Appointment> findById(Long id) {
        List<Appointment> results = jdbc.query(
            "SELECT * FROM appointments WHERE apmt_id = ?", rowMapper, id
        );
        return results.stream().findFirst();
    }

    public Long save(Appointment apmt) {
        // Use GeneratedKeyHolder so the insert ID is read from the same connection that
        // performed the INSERT.  last_insert_rowid() on a separate connection (as
        // DriverManagerDataSource opens a new connection per call) always returns 0.
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO appointments (user_id, stylist_id, start_time, end_time, status, notes, total_price, created_at, guest_name, guest_email, guest_phone) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            if (apmt.getUserId() != null) {
                ps.setLong(1, apmt.getUserId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setLong(2, apmt.getStylistId());
            ps.setString(3, apmt.getStartTime().toString());
            ps.setString(4, apmt.getEndTime().toString());
            ps.setString(5, apmt.getStatus());
            ps.setString(6, apmt.getNotes());
            ps.setDouble(7, apmt.getTotalPrice());
            ps.setString(8, LocalDateTime.now().toString());
            ps.setString(9, apmt.getGuestName());
            ps.setString(10, apmt.getGuestEmail());
            ps.setString(11, apmt.getGuestPhone());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void saveServices(Long apmtId, List<Long> serviceIds) {
        for (Long serviceId : serviceIds) {
            jdbc.update(
                "INSERT INTO appointment_services (apmt_id, service_id) VALUES (?, ?)",
                apmtId, serviceId
            );
        }
    }

    public int updateStatus(Long id, String status) {
        return jdbc.update("UPDATE appointments SET status = ? WHERE apmt_id = ?", status, id);
    }

    public int deleteById(Long id) {
        return jdbc.update("DELETE FROM appointments WHERE apmt_id = ?", id);
    }
}
