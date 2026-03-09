package com.beautifulnail.scheduler.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final JdbcTemplate jdbc;

    public HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // GET /health → system status (M6)
    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> status = new HashMap<>();
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            status.put("status", "UP");
            status.put("db", "connected");
        } catch (Exception e) {
            status.put("status", "DOWN");
            status.put("db", "unreachable");
            status.put("error", e.getMessage());
        }
        return status;
    }
}
