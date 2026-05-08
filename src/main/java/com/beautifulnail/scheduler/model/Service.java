package com.beautifulnail.scheduler.model;

public class Service {
    private Long serviceId;
    private String name;
    private double priceMin;
    private double priceMax;
    private int estDuration; // minutes

    public Service() {}

    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getPriceMin() { return priceMin; }
    public void setPriceMin(double priceMin) { this.priceMin = priceMin; }
    public double getPriceMax() { return priceMax; }
    public void setPriceMax(double priceMax) { this.priceMax = priceMax; }
    public int getEstDuration() { return estDuration; }
    public void setEstDuration(int estDuration) { this.estDuration = estDuration; }
}
