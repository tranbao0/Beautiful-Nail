package com.beautifulnail.scheduler.controller;

import com.beautifulnail.scheduler.service.ServiceCatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final ServiceCatalogService serviceCatalogService;

    public HomeController(ServiceCatalogService serviceCatalogService) {
        this.serviceCatalogService = serviceCatalogService;
    }

    // GET / → home page (wireframe: index.html)
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("services", serviceCatalogService.getAllServices());
        return "index";
    }
}
