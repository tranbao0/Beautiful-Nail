package com.beautifulnail.scheduler.controller;

import com.beautifulnail.scheduler.model.Appointment;
import com.beautifulnail.scheduler.service.AppointmentService;
import com.beautifulnail.scheduler.service.AvailabilityService;
import com.beautifulnail.scheduler.service.ServiceCatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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

    // GET /appointments → list user's appointments
    @GetMapping
    public String listAppointments(Model model) {
        // TODO: replace hardcoded userId with session-based user
        Long userId = 1L;
        model.addAttribute("appointments", appointmentService.getAppointmentsByUser(userId));
        return "appointments/list";
    }

    // GET /appointments/book?slotId=... → booking form (wireframe: book.html)
    @GetMapping("/book")
    public String bookingForm(@RequestParam Long slotId, Model model) {
        model.addAttribute("slot", availabilityService.getAllSlots()
            .stream().filter(s -> s.getSlotId().equals(slotId)).findFirst().orElseThrow());
        model.addAttribute("services", serviceCatalogService.getAllServices());
        model.addAttribute("appointment", new Appointment());
        return "appointments/book";
    }

    // POST /appointments/book → submit booking
    @PostMapping("/book")
    public String submitBooking(@RequestParam Long slotId,
                                 @ModelAttribute Appointment appointment) {
        // TODO: set userId from session
        appointment.setUserId(1L);
        appointmentService.bookAppointment(appointment, slotId);
        return "redirect:/appointments/confirmation";
    }

    // GET /appointments/confirmation → confirmation page (wireframe: confirmation.html)
    @GetMapping("/confirmation")
    public String confirmation(Model model) {
        // TODO: pass actual booked appointment from session/flash
        return "appointments/confirmation";
    }

    // POST /appointments/{id}/cancel
    @PostMapping("/{id}/cancel")
    public String cancelAppointment(@PathVariable Long id, @RequestParam Long slotId) {
        appointmentService.cancelAppointment(id, slotId);
        return "redirect:/appointments";
    }
}
