package com.beautifulnail.scheduler.repository;

import com.beautifulnail.scheduler.model.Stylist;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class StylistRepository {

    private final JdbcTemplate jdbc;

    public StylistRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Stylist> rowMapper = (rs, rowNum) -> {
        Stylist s = new Stylist();
        s.setStylistId(rs.getLong("stylist_id"));
        s.setFirstName(rs.getString("first_name"));
        s.setMInit(rs.getString("m_init"));
        s.setLastName(rs.getString("last_name"));
        s.setEmail(rs.getString("email"));
        s.setPhone(rs.getString("phone"));
        s.setSpecialty(rs.getString("specialty"));
        return s;
    };

    public List<Stylist> findAll() {
        return jdbc.query("SELECT * FROM stylists", rowMapper);
    }

    public Optional<Stylist> findById(Long id) {
        List<Stylist> results = jdbc.query("SELECT * FROM stylists WHERE stylist_id = ?", rowMapper, id);
        return results.stream().findFirst();
    }

    public int save(Stylist stylist) {
        return jdbc.update(
            "INSERT INTO stylists (first_name, m_init, last_name, email, phone, specialty) VALUES (?, ?, ?, ?, ?, ?)",
            stylist.getFirstName(), stylist.getMInit(), stylist.getLastName(),
            stylist.getEmail(), stylist.getPhone(), stylist.getSpecialty()
        );
    }

    public int update(Stylist stylist) {
        return jdbc.update(
            "UPDATE stylists SET first_name=?, m_init=?, last_name=?, email=?, phone=?, specialty=? WHERE stylist_id=?",
            stylist.getFirstName(), stylist.getMInit(), stylist.getLastName(),
            stylist.getEmail(), stylist.getPhone(), stylist.getSpecialty(), stylist.getStylistId()
        );
    }

    public int deleteById(Long id) {
        return jdbc.update("DELETE FROM stylists WHERE stylist_id = ?", id);
    }
}
