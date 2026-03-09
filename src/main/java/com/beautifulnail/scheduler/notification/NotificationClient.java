package com.beautifulnail.scheduler.notification;

import com.beautifulnail.scheduler.model.Appointment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP client that calls the mock external notification service. (M5)
 */
@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);
    private static final String NOTIFICATION_URL = "http://localhost:8080/mock-notify";

    private final RestTemplate restTemplate;

    public NotificationClient() {
        this.restTemplate = new RestTemplate();
    }

    public void sendConfirmation(Appointment appointment) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("appointmentId", appointment.getApmtId());
            payload.put("userId", appointment.getUserId());
            payload.put("startTime", appointment.getStartTime());
            payload.put("status", appointment.getStatus());

            restTemplate.postForEntity(NOTIFICATION_URL, payload, String.class);
            log.info("Notification sent for appointment {}", appointment.getApmtId());
        } catch (Exception e) {
            log.warn("Notification failed for appointment {} — {}", appointment.getApmtId(), e.getMessage());
        }
    }
}
