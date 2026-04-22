package com.beautifulnail.scheduler.notification.dto;

/**
 * Structured response returned by the external Notification Service after
 * processing a {@link NotificationRequest}.
 *
 * Coarse-grained: one response carries delivery status, a tracking ID the
 * caller can log for auditing, a human-readable message, and a timestamp —
 * everything the Appointment Scheduler needs without issuing a follow-up
 * status-check call.
 */
public class NotificationResponse {

    /** DELIVERED | QUEUED | FAILED */
    private String status;

    /** Short unique ID assigned by the notification service for traceability. */
    private String notificationId;

    /** Human-readable result message. */
    private String message;

    /** ISO-8601 timestamp of when the service processed the request. */
    private String processedAt;

    public NotificationResponse() {}

    public NotificationResponse(String status, String notificationId,
                                 String message, String processedAt) {
        this.status         = status;
        this.notificationId = notificationId;
        this.message        = message;
        this.processedAt    = processedAt;
    }

    public String getStatus()                       { return status; }
    public void   setStatus(String status)          { this.status = status; }
    public String getNotificationId()               { return notificationId; }
    public void   setNotificationId(String id)      { this.notificationId = id; }
    public String getMessage()                      { return message; }
    public void   setMessage(String message)        { this.message = message; }
    public String getProcessedAt()                  { return processedAt; }
    public void   setProcessedAt(String processedAt){ this.processedAt = processedAt; }
}
