package com.beautifulnail.scheduler.repository;

import com.beautifulnail.scheduler.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<User> rowMapper = (rs, rowNum) -> {
        User u = new User();
        u.setUserId(rs.getLong("user_id"));
        u.setFirstName(rs.getString("first_name"));
        u.setMInit(rs.getString("m_init"));
        u.setLastName(rs.getString("last_name"));
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));
        u.setRole(rs.getString("role"));
        u.setPasswordHash(rs.getString("password_hash"));
        return u;
    };

    public List<User> findAll() {
        return jdbc.query("SELECT * FROM users", rowMapper);
    }

    public Optional<User> findById(Long id) {
        List<User> results = jdbc.query("SELECT * FROM users WHERE user_id = ?", rowMapper, id);
        return results.stream().findFirst();
    }

    public Optional<User> findByEmail(String email) {
        List<User> results = jdbc.query("SELECT * FROM users WHERE email = ?", rowMapper, email);
        return results.stream().findFirst();
    }

    public int save(User user) {
        return jdbc.update(
            "INSERT INTO users (first_name, m_init, last_name, email, phone, role, password_hash) VALUES (?, ?, ?, ?, ?, ?, ?)",
            user.getFirstName(), user.getMInit(), user.getLastName(),
            user.getEmail(), user.getPhone(), user.getRole(), user.getPasswordHash()
        );
    }

    public int deleteById(Long id) {
        return jdbc.update("DELETE FROM users WHERE user_id = ?", id);
    }
}
