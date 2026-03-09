package com.beautifulnail.scheduler.repository;

import com.beautifulnail.scheduler.model.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ServiceRepository {

    private final JdbcTemplate jdbc;

    public ServiceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Service> rowMapper = (rs, rowNum) -> {
        Service s = new Service();
        s.setServiceId(rs.getLong("service_id"));
        s.setName(rs.getString("name"));
        s.setPriceMin(rs.getDouble("price_min"));
        s.setPriceMax(rs.getDouble("price_max"));
        s.setEstDuration(rs.getInt("est_duration"));
        return s;
    };

    public List<Service> findAll() {
        return jdbc.query("SELECT * FROM services", rowMapper);
    }

    public Optional<Service> findById(Long id) {
        List<Service> results = jdbc.query("SELECT * FROM services WHERE service_id = ?", rowMapper, id);
        return results.stream().findFirst();
    }

    public int save(Service service) {
        return jdbc.update(
            "INSERT INTO services (name, price_min, price_max, est_duration) VALUES (?, ?, ?, ?)",
            service.getName(), service.getPriceMin(), service.getPriceMax(), service.getEstDuration()
        );
    }

    public int update(Service service) {
        return jdbc.update(
            "UPDATE services SET name=?, price_min=?, price_max=?, est_duration=? WHERE service_id=?",
            service.getName(), service.getPriceMin(), service.getPriceMax(),
            service.getEstDuration(), service.getServiceId()
        );
    }

    public int deleteById(Long id) {
        return jdbc.update("DELETE FROM services WHERE service_id = ?", id);
    }
}
