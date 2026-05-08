package com.beautifulnail.scheduler.notification;

import com.beautifulnail.scheduler.notification.dto.NotificationRequest;
import com.beautifulnail.scheduler.notification.dto.NotificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Simulates an external Notification Service (M5 - Distribution Boundary).
 *
 * In a real deployment this controller would live in a completely separate
 * process or third-party provider (SendGrid, Twilio, etc.). It is co-hosted
 * at :8080 here solely for milestone demonstration purposes.
 *
 * Endpoints:
 *   POST /api/external/notifications        - receive a notification
 *   GET  /api/external/notifications/log    - view received notifications (demo UI)
 */
@RestController
@RequestMapping("/api/external/notifications")
public class MockNotificationController {

    private static final Logger log = LoggerFactory.getLogger(MockNotificationController.class);
    private static final int MAX_LOG = 20;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final CopyOnWriteArrayList<LogEntry> receivedLog = new CopyOnWriteArrayList<>();

    @PostMapping
    public ResponseEntity<NotificationResponse> receiveNotification(
            @RequestBody NotificationRequest request) {

        String apptId    = request.getAppointment() != null
                           ? String.valueOf(request.getAppointment().getAppointmentId()) : "N/A";
        String recipient = request.getRecipient() != null
                           ? request.getRecipient().getEmail() : "unknown";
        String name      = request.getRecipient() != null
                           ? request.getRecipient().getName() : "unknown";

        String notifId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("[MockNotify] {} via {} - appt={} to={}", request.getType(), request.getChannel(), apptId, recipient);
        log.info("[MockNotify] Notification {} - simulated delivery OK to {}", notifId, recipient);

        receivedLog.add(0, new LogEntry(notifId, apptId, name, recipient, request.getChannel(), LocalDateTime.now()));
        if (receivedLog.size() > MAX_LOG) {
            receivedLog.remove(receivedLog.size() - 1);
        }

        NotificationResponse response = new NotificationResponse(
                "DELIVERED",
                notifId,
                "Confirmation sent via " + request.getChannel() + " to " + recipient,
                LocalDateTime.now().toString()
        );

        return ResponseEntity.ok(response);
    }

    /** Demo page — open in a browser tab to watch notifications arrive in real time. */
    @GetMapping(value = "/log", produces = MediaType.TEXT_HTML_VALUE)
    public String notificationLog() {
        StringBuilder rows = new StringBuilder();
        List<LogEntry> snapshot = new ArrayList<>(receivedLog);

        if (snapshot.isEmpty()) {
            rows.append("<tr><td colspan='5' style='text-align:center;color:#888;padding:24px;'>No notifications received yet.</td></tr>");
        } else {
            for (LogEntry e : snapshot) {
                rows.append("<tr>")
                    .append("<td>").append(e.time.format(FMT)).append("</td>")
                    .append("<td><code>").append(e.notifId).append("</code></td>")
                    .append("<td>Appt #").append(e.apptId).append("</td>")
                    .append("<td>").append(e.name).append("</td>")
                    .append("<td>").append(e.channel).append("</td>")
                    .append("</tr>");
            }
        }

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
            + "<meta http-equiv='refresh' content='3'>"
            + "<title>Notification Log</title>"
            + "<style>"
            + "body{font-family:monospace;margin:32px;background:#f5f5f5;}"
            + "h2{margin-bottom:4px;} p{color:#555;font-size:13px;margin-bottom:16px;}"
            + "table{border-collapse:collapse;width:100%;background:#fff;}"
            + "th{background:#111;color:#fff;padding:10px 14px;text-align:left;font-size:12px;letter-spacing:.05em;}"
            + "td{padding:10px 14px;border-bottom:1px solid #e0e0e0;font-size:13px;}"
            + "tr:hover td{background:#fafafa;}"
            + ".badge{background:#1a7a1a;color:#fff;padding:2px 8px;border-radius:3px;font-size:11px;}"
            + "</style></head><body>"
            + "<h2>Notification Service - Received Log</h2>"
            + "<p>Auto-refreshes every 3 seconds. Showing last " + MAX_LOG + " notifications.</p>"
            + "<table><thead><tr>"
            + "<th>Time</th><th>Notification ID</th><th>Appointment</th><th>Recipient</th><th>Channel</th>"
            + "</tr></thead><tbody>"
            + rows
            + "</tbody></table>"
            + "<p style='margin-top:12px;color:#aaa;font-size:11px;'>POST /api/external/notifications &mdash; "
            + snapshot.size() + " total received this session</p>"
            + "</body></html>";
    }

    private static class LogEntry {
        final String notifId;
        final String apptId;
        final String name;
        final String recipient;
        final String channel;
        final LocalDateTime time;

        LogEntry(String notifId, String apptId, String name, String recipient, String channel, LocalDateTime time) {
            this.notifId   = notifId;
            this.apptId    = apptId;
            this.name      = name;
            this.recipient = recipient;
            this.channel   = channel;
            this.time      = time;
        }
    }
}
