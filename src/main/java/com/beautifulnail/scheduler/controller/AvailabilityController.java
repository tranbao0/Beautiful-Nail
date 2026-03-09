package com.beautifulnail.scheduler.controller;

import com.beautifulnail.scheduler.service.AvailabilityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/slots")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    // GET /slots → view available slots (wireframe: slots.html)
    @GetMapping
    public String viewSlots(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long stylistId,
            Model model) {

        String dateStr = (date != null) ? date.toString() : LocalDate.now().toString();
        model.addAttribute("slots", availabilityService.getAvailableSlotsByDate(dateStr));
        model.addAttribute("selectedDate", dateStr);
        return "slots/index";
    }
}
