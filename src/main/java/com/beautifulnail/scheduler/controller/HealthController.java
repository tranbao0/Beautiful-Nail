package com.beautifulnail.scheduler.controller;

import com.beautifulnail.scheduler.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check endpoint for production monitoring and health checks.
 * Returns overall system status and component-level health. (M6)
 */
@RestController
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final JdbcTemplate jdbc;
    private final NotificationService notificationService;

    public HealthController(JdbcTemplate jdbc, NotificationService notificationService) {
        this.jdbc = jdbc;
        this.notificationService = notificationService;
    }

    /**
     * Health check endpoint returning overall system status and component health.
     * HTTP 200 = UP or DEGRADED
     * HTTP 503 = DOWN
     * (M6 System Management)
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> checks = new HashMap<>();

        // Check database connectivity (critical)
        Map<String, String> dbCheck = checkDatabase();
        checks.put("database", dbCheck);

        // Check notification service (non-critical, degrades gracefully)
        Map<String, String> notifCheck = checkNotificationService();
        checks.put("notification_service", notifCheck);

        // Determine overall status
        String overallStatus = "UP";
        HttpStatus httpStatus = HttpStatus.OK;

        if ("DOWN".equals(dbCheck.get("status"))) {
            // Critical failure
            overallStatus = "DOWN";
            httpStatus = HttpStatus.SERVICE_UNAVAILABLE;
            response.put("error", "Cannot serve requests");
        } else if ("DOWN".equals(notifCheck.get("status"))) {
            // Non-critical failure - degraded but operational
            overallStatus = "DEGRADED";
        }

        response.put("status", overallStatus);
        response.put("timestamp", Instant.now());
        response.put("checks", checks);

        return new ResponseEntity<>(response, httpStatus);
    }

    /**
     * Check database connectivity
     */
    private Map<String, String> checkDatabase() {
        Map<String, String> check = new HashMap<>();
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            check.put("status", "UP");
            check.put("message", "SQLite connected");
            log.debug("Database health check passed");
        } catch (Exception e) {
            check.put("status", "DOWN");
            check.put("message", "JDBC connection refused: " + e.getMessage());
            log.error("Database health check failed: {}", e.getMessage());
        }
        return check;
    }

    /**
     * Check notification service health
     */
    private Map<String, String> checkNotificationService() {
        Map<String, String> check = new HashMap<>();
        try {
            boolean isDegraded = notificationService.isServiceDegraded();
            int queuedCount = notificationService.getFailedNotificationsCount();

            if (isDegraded) {
                check.put("status", "DOWN");
                check.put("message", "Notification service degraded - " + queuedCount + " notifications in retry queue");
                log.warn("Notification service degraded: {} queued notifications", queuedCount);
            } else if (queuedCount > 0) {
                check.put("status", "DEGRADED");
                check.put("message", "Notification service experiencing issues - " + queuedCount + " notifications in retry queue");
            } else {
                check.put("status", "UP");
                check.put("message", "External notification service responding");
            }
        } catch (Exception e) {
            check.put("status", "DOWN");
            check.put("message", "Unable to reach notification service: " + e.getMessage());
            log.warn("Notification service health check failed: {}", e.getMessage());
        }
        return check;
    }
}
