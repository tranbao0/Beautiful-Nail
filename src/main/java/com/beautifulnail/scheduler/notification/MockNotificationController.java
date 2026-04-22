package com.beautifulnail.scheduler.notification;

import com.beautifulnail.scheduler.notification.dto.NotificationRequest;
import com.beautifulnail.scheduler.notification.dto.NotificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Simulates an external Notification Service (M5 — Distribution Boundary).
 *
 * In a real deployment this controller would live in a completely separate
 * process or third-party provider (SendGrid, Twilio, etc.).  It is co-hosted
 * at :8080 here solely for milestone demonstration purposes.
 *
 * Coarse-grained interface:
 *   One POST endpoint accepts a fully self-contained {@link NotificationRequest}
 *   and returns a {@link NotificationResponse} — no pre-registration handshake,
 *   no multi-step workflow, no polling required by the caller.
 *
 * Endpoint:  POST /api/external/notifications
 */
@RestController
@RequestMapping("/api/external/notifications")
public class MockNotificationController {

    private static final Logger log = LoggerFactory.getLogger(MockNotificationController.class);

    /**
     * Receive and process a notification request.
     *
     * <pre>
     * POST /api/external/notifications
     * Content-Type: application/json
     *
     * {
     *   "type"    : "BOOKING_CONFIRMATION",
     *   "channel" : "EMAIL",
     *   "recipient": { "name": "Jane Doe", "email": "jane@example.com", "phone": null },
     *   "appointment": {
     *       "appointmentId": 7,
     *       "startTime": "2026-04-25T10:00:00",
     *       "endTime"  : "2026-04-25T11:00:00",
     *       "totalPrice": 75.00,
     *       "notes": "Gel manicure"
     *   }
     * }
     * </pre>
     *
     * Returns a {@link NotificationResponse} with a generated tracking ID.
     */
    @PostMapping
    public ResponseEntity<NotificationResponse> receiveNotification(
            @RequestBody NotificationRequest request) {

        String apptId    = request.getAppointment() != null
                           ? String.valueOf(request.getAppointment().getAppointmentId()) : "N/A";
        String recipient = request.getRecipient() != null
                           ? request.getRecipient().getEmail() : "unknown";

        log.info("[MockNotify] {} via {} — appt={} → to={}",
                request.getType(), request.getChannel(), apptId, recipient);

        // Simulate generating a tracking ID and delivering the notification
        String notifId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("[MockNotify] Notification {} — simulated delivery OK to {}", notifId, recipient);

        NotificationResponse response = new NotificationResponse(
                "DELIVERED",
                notifId,
                "Confirmation sent via " + request.getChannel() + " to " + recipient,
                LocalDateTime.now().toString()
        );

        return ResponseEntity.ok(response);
    }
}
