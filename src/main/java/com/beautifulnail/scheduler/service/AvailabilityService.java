package com.beautifulnail.scheduler.service;

import com.beautifulnail.scheduler.model.Availability;
import com.beautifulnail.scheduler.repository.AvailabilityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AvailabilityService {

    private static final Logger log = LoggerFactory.getLogger(AvailabilityService.class);

    private final AvailabilityRepository availabilityRepo;

    public AvailabilityService(AvailabilityRepository availabilityRepo) {
        this.availabilityRepo = availabilityRepo;
    }

    public List<Availability> getAllSlots() {
        return availabilityRepo.findAll();
    }

    public List<Availability> getAvailableSlotsByDate(String date) {
        log.info("Fetching available slots for date: {}", date);
        return availabilityRepo.findAvailableByDate(date);
    }

    public List<Availability> getAvailableSlotsByStylist(Long stylistId) {
        return availabilityRepo.findAvailableByStylist(stylistId);
    }

    public void addSlot(Availability slot) {
        log.info("Adding availability slot for stylist {}", slot.getStylistId());
        availabilityRepo.save(slot);
    }
}
