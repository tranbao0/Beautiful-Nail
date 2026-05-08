package com.beautifulnail.scheduler.controller;

import com.beautifulnail.scheduler.model.Appointment;
import com.beautifulnail.scheduler.service.AppointmentService;
import com.beautifulnail.scheduler.service.AvailabilityService;
import com.beautifulnail.scheduler.service.ServiceCatalogService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AvailabilityService availabilityService;
    private final ServiceCatalogService serviceCatalogService;

    public AppointmentController(AppointmentService appointmentService,
                                  AvailabilityService availabilityService,
                                  ServiceCatalogService serviceCatalogService) {
        this.appointmentService = appointmentService;
        this.availabilityService = availabilityService;
        this.serviceCatalogService = serviceCatalogService;
    }

    // GET /appointments → list user's appointments (requires login)
    @GetMapping
    public String listAppointments(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";
        model.addAttribute("appointments", appointmentService.getAppointmentsByUser(userId));
        return "appointments/list";
    }

    // GET /appointments/book?slotId=... → booking form (guests allowed)
    @GetMapping("/book")
    public String bookingForm(@RequestParam Long slotId, HttpSession session, Model model) {
        var slot = availabilityService.getAllSlots()
            .stream().filter(s -> s.getSlotId().equals(slotId)).findFirst().orElseThrow();
        model.addAttribute("slot", slot);
        model.addAttribute("services", serviceCatalogService.getServicesByStylistId(slot.getStylistId()));
        model.addAttribute("isLoggedIn", session.getAttribute("userId") != null);
        return "appointments/book";
    }

    // POST /appointments/book → submit booking (guests allowed)
    @PostMapping("/book")
    public String submitBooking(
            @RequestParam Long slotId,
            @RequestParam(required = false) List<Long> serviceId,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String notes,
            HttpSession session) {

        Appointment apmt = new Appointment();
        apmt.setNotes(notes);

        Long userId = (Long) session.getAttribute("userId");
        if (userId != null) {
            // Logged-in user: bind to account
            apmt.setUserId(userId);
        } else {
            // Guest: store contact details directly on the appointment
            apmt.setGuestName(((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim());
            apmt.setGuestEmail(email);
            apmt.setGuestPhone(phone);
        }

        List<Long> serviceIds = serviceId != null ? serviceId : Collections.emptyList();
        appointmentService.bookAppointmentWithRetry(apmt, slotId, serviceIds);
        return "redirect:/appointments/confirmation";
    }

    // GET /appointments/confirmation
    @GetMapping("/confirmation")
    public String confirmation(HttpSession session, Model model) {
        model.addAttribute("isGuest", session.getAttribute("userId") == null);
        return "appointments/confirmation";
    }

    // POST /appointments/{id}/cancel (requires login)
    @PostMapping("/{id}/cancel")
    public String cancelAppointment(@PathVariable Long id,
                                    @RequestParam Long slotId,
                                    HttpSession session) {
        if (session.getAttribute("userId") == null) return "redirect:/login";
        appointmentService.cancelAppointment(id, slotId);
        return "redirect:/appointments";
    }
}
