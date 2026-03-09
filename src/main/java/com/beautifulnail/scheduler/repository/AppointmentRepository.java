package com.beautifulnail.scheduler.repository;

import com.beautifulnail.scheduler.model.Appointment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

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
        a.setUserId(rs.getLong("user_id"));
        a.setStylistId(rs.getLong("stylist_id"));
        a.setStartTime(LocalDateTime.parse(rs.getString("start_time")));
        a.setEndTime(LocalDateTime.parse(rs.getString("end_time")));
        a.setStatus(rs.getString("status"));
        a.setNotes(rs.getString("notes"));
        a.setTotalPrice(rs.getDouble("total_price"));
        a.setCreatedAt(LocalDateTime.parse(rs.getString("created_at")));
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

    public int save(Appointment apmt) {
        return jdbc.update(
            "INSERT INTO appointments (user_id, stylist_id, start_time, end_time, status, notes, total_price, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            apmt.getUserId(), apmt.getStylistId(),
            apmt.getStartTime().toString(), apmt.getEndTime().toString(),
            apmt.getStatus(), apmt.getNotes(), apmt.getTotalPrice(),
            LocalDateTime.now().toString()
        );
    }

    public int updateStatus(Long id, String status) {
        return jdbc.update("UPDATE appointments SET status = ? WHERE apmt_id = ?", status, id);
    }

    public int deleteById(Long id) {
        return jdbc.update("DELETE FROM appointments WHERE apmt_id = ?", id);
    }
}
