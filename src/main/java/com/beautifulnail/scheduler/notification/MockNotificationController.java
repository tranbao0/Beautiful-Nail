package com.beautifulnail.scheduler.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Simulates an external notification service. (M5 — Distribution Boundary)
 * In production this would be a separate deployed service.
 */
@RestController
@RequestMapping("/mock-notify")
public class MockNotificationController {

    private static final Logger log = LoggerFactory.getLogger(MockNotificationController.class);

    @PostMapping
    public ResponseEntity<String> receiveNotification(@RequestBody Map<String, Object> payload) {
        log.info("[MockNotify] Received confirmation request: appointmentId={}, userId={}",
            payload.get("appointmentId"), payload.get("userId"));
        return ResponseEntity.ok("Notification received and processed.");
    }
}
