package com.beautifulnail.scheduler.controller;

import com.beautifulnail.scheduler.model.Availability;
import com.beautifulnail.scheduler.model.Service;
import com.beautifulnail.scheduler.model.User;
import com.beautifulnail.scheduler.repository.StylistRepository;
import com.beautifulnail.scheduler.repository.UserRepository;
import com.beautifulnail.scheduler.service.AppointmentService;
import com.beautifulnail.scheduler.service.AvailabilityService;
import com.beautifulnail.scheduler.service.ServiceCatalogService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/receptionist")
public class ReceptionistController {

    private final AppointmentService appointmentService;
    private final AvailabilityService availabilityService;
    private final ServiceCatalogService serviceCatalogService;
    private final UserRepository userRepo;
    private final StylistRepository stylistRepo;

    public ReceptionistController(AppointmentService appointmentService,
                                   AvailabilityService availabilityService,
                                   ServiceCatalogService serviceCatalogService,
                                   UserRepository userRepo,
                                   StylistRepository stylistRepo) {
        this.appointmentService = appointmentService;
        this.availabilityService = availabilityService;
        this.serviceCatalogService = serviceCatalogService;
        this.userRepo = userRepo;
        this.stylistRepo = stylistRepo;
    }

    // GET /receptionist → dashboard
    @GetMapping
    public String dashboard(HttpSession session, Model model) {
        if (!isReceptionist(session)) return "redirect:/";
        Map<Long, String> userNames = userRepo.findAll().stream()
                .collect(Collectors.toMap(User::getUserId,
                        u -> u.getFirstName() + " " + u.getLastName()));
        model.addAttribute("appointments", appointmentService.getAllAppointments());
        model.addAttribute("services", serviceCatalogService.getAllServices());
        model.addAttribute("stylists", stylistRepo.findAll());
        model.addAttribute("userNames", userNames);
        return "receptionist/dashboard";
    }

    // POST /receptionist/appointments/{id}/cancel
    @PostMapping("/appointments/{id}/cancel")
    public String cancelAppointment(@PathVariable Long id,
                                    @RequestParam Long slotId,
                                    HttpSession session) {
        if (!isReceptionist(session)) return "redirect:/";
        appointmentService.cancelAppointment(id, slotId);
        return "redirect:/receptionist";
    }

    // POST /receptionist/slots/add
    @PostMapping("/slots/add")
    public String addSlot(@ModelAttribute Availability slot, HttpSession session) {
        if (!isReceptionist(session)) return "redirect:/";
        availabilityService.addSlot(slot);
        return "redirect:/receptionist";
    }

    // POST /receptionist/services/add
    @PostMapping("/services/add")
    public String addService(@ModelAttribute Service service, HttpSession session) {
        if (!isReceptionist(session)) return "redirect:/";
        serviceCatalogService.addService(service);
        return "redirect:/receptionist";
    }

    // POST /receptionist/services/{id}/delete
    @PostMapping("/services/{id}/delete")
    public String deleteService(@PathVariable Long id, HttpSession session) {
        if (!isReceptionist(session)) return "redirect:/";
        serviceCatalogService.deleteService(id);
        return "redirect:/receptionist";
    }

    private boolean isReceptionist(HttpSession session) {
        return "receptionist".equals(session.getAttribute("userRole"));
    }
}
