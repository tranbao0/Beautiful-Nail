package com.beautifulnail.scheduler.controller;

import com.beautifulnail.scheduler.model.Availability;
import com.beautifulnail.scheduler.model.Service;
import com.beautifulnail.scheduler.service.AppointmentService;
import com.beautifulnail.scheduler.service.AvailabilityService;
import com.beautifulnail.scheduler.service.ServiceCatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/receptionist")
public class ReceptionistController {

    private final AppointmentService appointmentService;
    private final AvailabilityService availabilityService;
    private final ServiceCatalogService serviceCatalogService;

    public ReceptionistController(AppointmentService appointmentService,
                                   AvailabilityService availabilityService,
                                   ServiceCatalogService serviceCatalogService) {
        this.appointmentService = appointmentService;
        this.availabilityService = availabilityService;
        this.serviceCatalogService = serviceCatalogService;
    }

    // GET /receptionist → dashboard
    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("appointments", appointmentService.getAllAppointments());
        model.addAttribute("services", serviceCatalogService.getAllServices());
        return "receptionist/dashboard";
    }

    // POST /receptionist/slots/add
    @PostMapping("/slots/add")
    public String addSlot(@ModelAttribute Availability slot) {
        availabilityService.addSlot(slot);
        return "redirect:/receptionist";
    }

    // POST /receptionist/services/add
    @PostMapping("/services/add")
    public String addService(@ModelAttribute Service service) {
        serviceCatalogService.addService(service);
        return "redirect:/receptionist";
    }

    // POST /receptionist/services/{id}/delete
    @PostMapping("/services/{id}/delete")
    public String deleteService(@PathVariable Long id) {
        serviceCatalogService.deleteService(id);
        return "redirect:/receptionist";
    }
}
