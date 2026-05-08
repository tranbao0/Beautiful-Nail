package com.beautifulnail.scheduler.controller;

import com.beautifulnail.scheduler.model.Appointment;
import com.beautifulnail.scheduler.notification.dto.NotificationResponse;
import com.beautifulnail.scheduler.service.AppointmentService;
import com.beautifulnail.scheduler.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST endpoint that explicitly exposes the Appointment Scheduler →
 * Notification Service distribution boundary (M5).
 *
 * Endpoint:  POST /api/notify
 */
@RestController
@RequestMapping("/api")
public class NotificationApiController {

    private static final Logger log = LoggerFactory.getLogger(NotificationApiController.class);

    private final AppointmentService  appointmentService;
    private final NotificationService notificationService;

    public NotificationApiController(AppointmentService appointmentService,
                                      NotificationService notificationService) {
        this.appointmentService  = appointmentService;
        this.notificationService = notificationService;
    }

    /**
     * Trigger a confirmation notification for an existing appointment.
     *
     * <pre>
     * POST /api/notify
     * Content-Type: application/json
     * { "appointmentId": 1 }
     * </pre>
     */
    @PostMapping("/notify")
    public ResponseEntity<?> triggerNotification(@RequestBody Map<String, Long> body) {
        Long appointmentId = body.get("appointmentId");
        if (appointmentId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "appointmentId is required"));
        }

        Appointment appointment = appointmentService.getAppointmentById(appointmentId)
                .orElse(null);
        if (appointment == null) {
            return ResponseEntity.notFound().build();
        }

        log.info("Manual notification trigger for appointment {}", appointmentId);

        NotificationResponse response = notificationService.sendConfirmation(appointment);

        if (response == null) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Notification service unavailable"));
        }

        return ResponseEntity.ok(Map.of(
                "appointmentId",  appointmentId,
                "notificationId", response.getNotificationId(),
                "status",         response.getStatus(),
                "message",        response.getMessage()
        ));
    }
}
