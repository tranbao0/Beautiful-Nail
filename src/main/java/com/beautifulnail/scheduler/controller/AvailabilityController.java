package com.beautifulnail.scheduler.controller;

import com.beautifulnail.scheduler.model.Availability;
import com.beautifulnail.scheduler.model.Stylist;
import com.beautifulnail.scheduler.repository.ServiceRepository;
import com.beautifulnail.scheduler.repository.StylistRepository;
import com.beautifulnail.scheduler.service.AvailabilityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/slots")
public class AvailabilityController {

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("EEEE, MMMM d");

    private final AvailabilityService availabilityService;
    private final StylistRepository stylistRepo;
    private final ServiceRepository serviceRepo;

    public AvailabilityController(AvailabilityService availabilityService,
                                  StylistRepository stylistRepo,
                                  ServiceRepository serviceRepo) {
        this.availabilityService = availabilityService;
        this.stylistRepo = stylistRepo;
        this.serviceRepo = serviceRepo;
    }

    @GetMapping
    public String viewSlots(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long stylistId,
            @RequestParam(required = false) List<Long> serviceIds,
            Model model) {

        LocalDate selectedDate = (date != null) ? date : LocalDate.now();
        String dateStr = selectedDate.toString();
        List<Long> selected = (serviceIds != null) ? serviceIds : Collections.emptyList();

        List<Availability> slots = availabilityService.getFilteredSlots(dateStr, stylistId, selected);

        // Stylists that have at least one slot on this date after filtering
        Set<Long> stylistIdsWithSlots = slots.stream()
                .map(Availability::getStylistId)
                .collect(Collectors.toSet());
        List<Stylist> allStylists = stylistRepo.findAll();
        List<Stylist> slotStylists = allStylists.stream()
                .filter(s -> stylistIdsWithSlots.contains(s.getStylistId()))
                .collect(Collectors.toList());

        // When no slots and at least one filter is active, find the next dates that have availability
        boolean hasFilter = !selected.isEmpty() || stylistId != null;
        List<LocalDate> nextAvailableDates = Collections.emptyList();
        if (slots.isEmpty() && hasFilter) {
            nextAvailableDates = availabilityService.findNextAvailableDates(dateStr, stylistId, selected)
                    .stream()
                    .map(LocalDate::parse)
                    .collect(Collectors.toList());
        }

        model.addAttribute("slots", slots);
        model.addAttribute("slotStylists", slotStylists);
        model.addAttribute("stylists", allStylists);           // sidebar filter list
        model.addAttribute("selectedDate", dateStr);
        model.addAttribute("prevDate", selectedDate.minusDays(1).toString());
        model.addAttribute("nextDate", selectedDate.plusDays(1).toString());
        model.addAttribute("selectedStylistId", stylistId);
        model.addAttribute("selectedServiceIds", selected);
        model.addAttribute("hasFilter", hasFilter);
        model.addAttribute("nextAvailableDates", nextAvailableDates);
        model.addAttribute("services", serviceRepo.findAll());
        model.addAttribute("stylistServicesJson", buildJson(serviceRepo.getStylistServiceMap()));

        return "slots/index";
    }

    private String buildJson(Map<Long, List<Long>> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean firstEntry = true;
        for (Map.Entry<Long, List<Long>> e : map.entrySet()) {
            if (!firstEntry) sb.append(',');
            firstEntry = false;
            sb.append('"').append(e.getKey()).append('"').append(":[");
            boolean firstVal = true;
            for (Long v : e.getValue()) {
                if (!firstVal) sb.append(',');
                firstVal = false;
                sb.append(v);
            }
            sb.append(']');
        }
        sb.append('}');
        return sb.toString();
    }
}
