package com.beautifulnail.scheduler.controller;

import com.beautifulnail.scheduler.service.ServiceCatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/services")
public class ServiceController {

    private final ServiceCatalogService serviceCatalogService;

    public ServiceController(ServiceCatalogService serviceCatalogService) {
        this.serviceCatalogService = serviceCatalogService;
    }

    // GET /services → list all services with pricing
    @GetMapping
    public String listServices(Model model) {
        model.addAttribute("services", serviceCatalogService.getAllServices());
        return "services/index";
    }
}
