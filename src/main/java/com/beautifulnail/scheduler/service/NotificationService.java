package com.beautifulnail.scheduler.service;

import com.beautifulnail.scheduler.model.Appointment;
import com.beautifulnail.scheduler.notification.NotificationClient;
import com.beautifulnail.scheduler.notification.dto.NotificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Application-layer facade for outgoing notifications with retry and fallback logic.
 * (M6 System Management)
 *
 * Responsibility: assemble the recipient context (name, email) that the
 * {@link NotificationClient} needs, delegate the HTTP call, and implement
 * resilience patterns (retry with exponential backoff, fallback queue).
 * In production the recipient details would be fetched from UserRepository using
 * {@code appointment.getUserId()}.  For milestone purposes they are stubbed.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int MAX_RETRIES = 3;
    private static final int[] BACKOFF_MS = {1000, 2000, 4000};
    private static final long RETRY_QUEUE_TTL_MS = 24 * 60 * 60 * 1000; // 24 hours

    private final NotificationClient notificationClient;
    private final Queue<QueuedNotification> retryQueue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger failedNotifications = new AtomicInteger(0);

    public NotificationService(NotificationClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    /**
     * Send a booking confirmation with retry and fallback logic.
     * Implements exponential backoff and graceful degradation. (M6)
     *
     * @return the {@link NotificationResponse} from the external service,
     *         or {@code null} if all retries failed (queued for async retry)
     */
    public NotificationResponse sendConfirmation(Appointment appointment) {
        log.info("Sending confirmation for appointment {}", appointment.getApmtId());

        String recipientName  = "Valued Customer";
        String recipientEmail = "customer@example.com";

        return sendWithRetry(appointment, recipientName, recipientEmail);
    }

    /**
     * Send notification with exponential backoff retry logic.
     * Gracefully degrades to async queue if all retries fail.
     */
    private NotificationResponse sendWithRetry(Appointment appointment, String name, String email) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                NotificationResponse response = notificationClient.sendConfirmation(
                        appointment, name, email);

                if (response != null) {
                    log.info("Confirmation delivered — appointmentId={}, notificationId={}, status={}",
                            appointment.getApmtId(), response.getNotificationId(), response.getStatus());
                    return response;
                } else {
                    log.warn("Notification service did not respond for appointment {}",
                            appointment.getApmtId());
                }

            } catch (Exception e) {
                log.warn("Notification attempt {}/{} failed for appointment {}: {}",
                        attempt + 1, MAX_RETRIES, appointment.getApmtId(), e.getMessage());

                if (attempt < MAX_RETRIES - 1) {
                    try {
                        long waitMs = BACKOFF_MS[attempt];
                        log.debug("Retrying notification in {}ms", waitMs);
                        Thread.sleep(waitMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // All retries failed - queue for async retry
        log.error("Notification failed after {} retries for appointment {}; queueing for async retry",
                MAX_RETRIES, appointment.getApmtId());
        failedNotifications.incrementAndGet();
        queueForAsyncRetry(appointment, name, email);
        return null;
    }

    /**
     * Queue a failed notification for async retry (graceful degradation).
     */
    private void queueForAsyncRetry(Appointment appointment, String recipientName, String recipientEmail) {
        QueuedNotification queued = new QueuedNotification(appointment, recipientName, recipientEmail);
        retryQueue.offer(queued);
        log.info("Notification queued for async retry (queue size: {})", retryQueue.size());
    }

    /**
     * Periodic task to retry queued notifications (runs every 30 seconds).
     * Part of fallback recovery strategy. (M6)
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 30000)
    public void processRetryQueue() {
        if (retryQueue.isEmpty()) {
            return;
        }

        log.debug("Processing retry queue (size: {})", retryQueue.size());
        int processed = 0;

        Queue<QueuedNotification> tempQueue = new LinkedList<>(retryQueue);
        retryQueue.clear();

        for (QueuedNotification queued : tempQueue) {
            // Check if notification has expired (24 hour TTL)
            if (System.currentTimeMillis() - queued.queuedAt > RETRY_QUEUE_TTL_MS) {
                log.warn("Notification for appointment {} expired in retry queue; discarding",
                        queued.appointment.getApmtId());
                continue;
            }

            try {
                NotificationResponse response = notificationClient.sendConfirmation(
                        queued.appointment, queued.recipientName, queued.recipientEmail);

                if (response != null) {
                    log.info("Queued notification sent successfully for appointment {}",
                            queued.appointment.getApmtId());
                    processed++;
                } else {
                    log.debug("Queued notification still failing; will retry later for appointment {}",
                            queued.appointment.getApmtId());
                    retryQueue.offer(queued);
                }
            } catch (Exception e) {
                log.warn("Queued notification retry failed for appointment {}: {}",
                        queued.appointment.getApmtId(), e.getMessage());
                retryQueue.offer(queued);
            }
        }

        if (processed > 0) {
            log.info("Processed {} notifications from retry queue", processed);
        }
    }

    /**
     * Get count of failed notifications awaiting retry.
     * Used for monitoring/health checks. (M6)
     */
    public int getFailedNotificationsCount() {
        return retryQueue.size();
    }

    /**
     * Get total failed notification attempts.
     * Used for metrics collection. (M6)
     */
    public int getTotalFailedAttempts() {
        return failedNotifications.get();
    }

    /**
     * Check if notification service is experiencing issues.
     * Returns true if retry queue is growing. (M6)
     */
    public boolean isServiceDegraded() {
        return retryQueue.size() > 5;
    }

    /**
     * Internal class to track queued notifications for async retry.
     */
    private static class QueuedNotification {
        final Appointment appointment;
        final String recipientName;
        final String recipientEmail;
        final long queuedAt;

        QueuedNotification(Appointment appointment, String recipientName, String recipientEmail) {
            this.appointment = appointment;
            this.recipientName = recipientName;
            this.recipientEmail = recipientEmail;
            this.queuedAt = System.currentTimeMillis();
        }
    }
}
