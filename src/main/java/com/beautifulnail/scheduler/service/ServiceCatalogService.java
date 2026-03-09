package com.beautifulnail.scheduler.service;

import com.beautifulnail.scheduler.model.Service;
import com.beautifulnail.scheduler.repository.ServiceRepository;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Service
public class ServiceCatalogService {

    private final ServiceRepository serviceRepo;

    public ServiceCatalogService(ServiceRepository serviceRepo) {
        this.serviceRepo = serviceRepo;
    }

    public List<Service> getAllServices() {
        return serviceRepo.findAll();
    }

    public Optional<Service> getServiceById(Long id) {
        return serviceRepo.findById(id);
    }

    public void addService(Service service) {
        serviceRepo.save(service);
    }

    public void updateService(Service service) {
        serviceRepo.update(service);
    }

    public void deleteService(Long id) {
        serviceRepo.deleteById(id);
    }
}
