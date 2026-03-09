package com.beautifulnail.scheduler.service;

import com.beautifulnail.scheduler.model.Appointment;
import com.beautifulnail.scheduler.repository.AppointmentRepository;
import com.beautifulnail.scheduler.repository.AvailabilityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

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
     * Books an appointment with SERIALIZABLE isolation to prevent double-booking.
     * Checks slot availability, marks slot unavailable, saves appointment, then notifies. (M4)
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

        availabilityRepo.markUnavailable(slotId);
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
