package com.beautifulnail.scheduler.service;

import com.beautifulnail.scheduler.model.Appointment;
import com.beautifulnail.scheduler.notification.NotificationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationClient notificationClient;

    public NotificationService(NotificationClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    public void sendConfirmation(Appointment appointment) {
        log.info("Sending confirmation for appointment {}", appointment.getApmtId());
        notificationClient.sendConfirmation(appointment);
    }
}
