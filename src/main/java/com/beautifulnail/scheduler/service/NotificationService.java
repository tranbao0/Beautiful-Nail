package com.beautifulnail.scheduler.service;

import com.beautifulnail.scheduler.model.Appointment;
import com.beautifulnail.scheduler.notification.NotificationClient;
import com.beautifulnail.scheduler.notification.dto.NotificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Application-layer facade for outgoing notifications.
 *
 * Responsibility: assemble the recipient context (name, email) that the
 * {@link NotificationClient} needs and delegate the HTTP call.  In production
 * the recipient details would be fetched from UserRepository using
 * {@code appointment.getUserId()}.  For milestone purposes they are stubbed.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationClient notificationClient;

    public NotificationService(NotificationClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    /**
     * Send a booking confirmation for the given appointment.
     * Failure is non-fatal: logged as WARN, booking transaction is unaffected.
     *
     * @return the {@link NotificationResponse} from the external service,
     *         or {@code null} if the service was unreachable
     */
    public NotificationResponse sendConfirmation(Appointment appointment) {
        log.info("Sending confirmation for appointment {}", appointment.getApmtId());

        // TODO: look up real name/email via UserRepository with appointment.getUserId()
        String recipientName  = "Valued Customer";
        String recipientEmail = "customer@example.com";

        NotificationResponse response = notificationClient.sendConfirmation(
                appointment, recipientName, recipientEmail);

        if (response != null) {
            log.info("Confirmation delivered — notificationId={}, status={}",
                    response.getNotificationId(), response.getStatus());
        } else {
            log.warn("Notification service did not respond for appointment {}",
                    appointment.getApmtId());
        }

        return response;
    }
}
