package com.beautifulnail.scheduler.notification;

import com.beautifulnail.scheduler.model.Appointment;
import com.beautifulnail.scheduler.notification.dto.NotificationRequest;
import com.beautifulnail.scheduler.notification.dto.NotificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP client that crosses the distribution boundary from the Appointment
 * Scheduler to the external Notification Service.
 *
 * Coarse-grained interface justification:
 *   A single POST call carries the complete {@link NotificationRequest} —
 *   recipient identity, delivery channel, and full appointment detail — so
 *   the external service can act without issuing follow-up queries back to us.
 *   This design minimises coupling: the Notification Service never needs to
 *   know our internal data model or query our database.
 *
 * Target endpoint: POST ${notification.service.url}
 */
@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final String notificationUrl;
    private final RestTemplate restTemplate;

    public NotificationClient(
            @Value("${notification.service.url}") String notificationUrl,
            RestTemplate restTemplate) {
        this.notificationUrl = notificationUrl;
        this.restTemplate    = restTemplate;
    }

    /**
     * Build a self-contained {@link NotificationRequest} from appointment data
     * and POST it to the external notification service in one coarse-grained call.
     *
     * @param appointment    the booked appointment
     * @param recipientName  display name of the customer
     * @param recipientEmail email address of the customer
     * @return the service's {@link NotificationResponse}, or {@code null} on failure
     */
    public NotificationResponse sendConfirmation(Appointment appointment,
                                                  String recipientName,
                                                  String recipientEmail) {
        NotificationRequest.RecipientInfo recipient =
                new NotificationRequest.RecipientInfo(recipientName, recipientEmail, null);

        NotificationRequest.AppointmentInfo apptInfo = new NotificationRequest.AppointmentInfo(
                appointment.getApmtId(),
                appointment.getStartTime() != null ? appointment.getStartTime().toString() : null,
                appointment.getEndTime()   != null ? appointment.getEndTime().toString()   : null,
                appointment.getTotalPrice(),
                appointment.getNotes()
        );

        NotificationRequest request = new NotificationRequest(
                "BOOKING_CONFIRMATION", "EMAIL", recipient, apptInfo);

        try {
            ResponseEntity<NotificationResponse> response =
                    restTemplate.postForEntity(notificationUrl, request, NotificationResponse.class);

            NotificationResponse body = response.getBody();
            if (body != null) {
                log.info("Notification {} ({}) sent for appointment {}",
                        body.getNotificationId(), body.getStatus(), appointment.getApmtId());
            }
            return body;
        } catch (Exception e) {
            log.warn("Notification failed for appointment {} — {}",
                    appointment.getApmtId(), e.getMessage());
            return null;
        }
    }
}
