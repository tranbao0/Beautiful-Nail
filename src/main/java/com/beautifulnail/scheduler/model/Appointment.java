package com.beautifulnail.scheduler.model;

import java.time.LocalDateTime;

public class Appointment {
    private Long apmtId;
    private Long userId;
    private Long stylistId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status; // "booked", "cancelled", "completed"
    private String notes;
    private double totalPrice;
    private LocalDateTime createdAt;
    private String guestName;
    private String guestEmail;
    private String guestPhone;

    public Appointment() {}

    public Long getApmtId() { return apmtId; }
    public void setApmtId(Long apmtId) { this.apmtId = apmtId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getStylistId() { return stylistId; }
    public void setStylistId(Long stylistId) { this.stylistId = stylistId; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }
    public String getGuestEmail() { return guestEmail; }
    public void setGuestEmail(String guestEmail) { this.guestEmail = guestEmail; }
    public String getGuestPhone() { return guestPhone; }
    public void setGuestPhone(String guestPhone) { this.guestPhone = guestPhone; }
    public boolean isGuest() { return userId == null; }
}
