package com.beautifulnail.scheduler.model;

public class Stylist {
    private Long stylistId;
    private String firstName;
    private String mInit;
    private String lastName;
    private String email;
    private String phone;
    private String specialty;

    public Stylist() {}

    public Long getStylistId() { return stylistId; }
    public void setStylistId(Long stylistId) { this.stylistId = stylistId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getMInit() { return mInit; }
    public void setMInit(String mInit) { this.mInit = mInit; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }
}
