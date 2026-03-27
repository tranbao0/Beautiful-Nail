package com.beautifulnail.scheduler.service;

import com.beautifulnail.scheduler.model.Appointment;
import com.beautifulnail.scheduler.repository.AppointmentRepository;
import com.beautifulnail.scheduler.repository.AvailabilityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.Optional;

@Service
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointmentRepo;
    private final AvailabilityRepository availabilityRepo;
    private final NotificationService notificationService;

    public AppointmentService(AppointmentRepository appointmentRepo,
                               AvailabilityRepository availabilityRepo,
                               NotificationService notificationService) {
        this.appointmentRepo = appointmentRepo;
        this.availabilityRepo = availabilityRepo;
        this.notificationService = notificationService;
    }

    public List<Appointment> getAllAppointments() {
        return appointmentRepo.findAll();
    }

    public List<Appointment> getAppointmentsByUser(Long userId) {
        return appointmentRepo.findByUserId(userId);
    }

    public Optional<Appointment> getAppointmentById(Long id) {
        return appointmentRepo.findById(id);
    }

    /**
     * Retry wrapper for bookAppointment. Retries up to 3 times on slot contention
     * (SQLite busy or slot taken by a concurrent transaction). (M4)
     */
    public Appointment bookAppointmentWithRetry(Appointment appointment, Long slotId) {
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return bookAppointment(appointment, slotId);
            } catch (IllegalStateException e) {
                log.warn("Booking attempt {}/{} failed for slot {}: {}", attempt, maxAttempts, slotId, e.getMessage());
                if (attempt == maxAttempts) throw e;
            } catch (DataAccessException e) {
                log.warn("DB contention on attempt {}/{} for slot {}", attempt, maxAttempts, slotId);
                if (attempt == maxAttempts) throw e;
            }
        }
        throw new IllegalStateException("Booking failed after " + maxAttempts + " attempts.");
    }

    /**
     * Books an appointment with SERIALIZABLE isolation to prevent double-booking.
     * Checks slot availability, marks slot unavailable atomically, saves appointment, then notifies. (M4)
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Appointment bookAppointment(Appointment appointment, Long slotId) {
        log.info("Attempting to book appointment for user {} on slot {}", appointment.getUserId(), slotId);

        var slot = availabilityRepo.findById(slotId)
            .orElseThrow(() -> new IllegalArgumentException("Slot not found: " + slotId));

        if (!slot.isAvailable()) {
            log.warn("Slot {} is no longer available", slotId);
            throw new IllegalStateException("This time slot is no longer available.");
        }

        appointment.setStartTime(slot.getStartTime());
        appointment.setEndTime(slot.getEndTime());
        appointment.setStatus("booked");

        // Optimistic check: affected rows = 0 means another transaction already claimed this slot
        int updated = availabilityRepo.markUnavailable(slotId);
        if (updated == 0) {
            log.warn("Slot {} was claimed by a concurrent transaction", slotId);
            throw new IllegalStateException("This time slot is no longer available.");
        }

        appointmentRepo.save(appointment);

        log.info("Appointment booked successfully for user {}", appointment.getUserId());
        notificationService.sendConfirmation(appointment);

        return appointment;
    }

    @Transactional
    public void cancelAppointment(Long appointmentId, Long slotId) {
        log.info("Cancelling appointment {}", appointmentId);
        appointmentRepo.updateStatus(appointmentId, "cancelled");
        availabilityRepo.markAvailable(slotId);
        log.info("Appointment {} cancelled, slot {} re-opened", appointmentId, slotId);
    }
}
