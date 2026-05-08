package com.beautifulnail.scheduler.notification.dto;

/**
 * Coarse-grained request payload sent to the external Notification Service.
 *
 * Design rationale: a single, self-contained object carries everything the
 * downstream service needs — notification type, delivery channel, recipient
 * identity, and full appointment detail — in one HTTP call.
 *
 * Fine-grained alternative (rejected): separate calls to set recipient, set
 * template, set appointment, then trigger send.  That approach multiplies
 * network round-trips and couples the caller to the internal state machine
 * of the external service.  One coarse call removes that coupling and keeps
 * the distributed boundary simple.
 */
public class NotificationRequest {

    /** BOOKING_CONFIRMATION | CANCELLATION */
    private String type;

    /** EMAIL | SMS */
    private String channel;

    private RecipientInfo recipient;
    private AppointmentInfo appointment;

    public NotificationRequest() {}

    public NotificationRequest(String type, String channel,
                                RecipientInfo recipient, AppointmentInfo appointment) {
        this.type        = type;
        this.channel     = channel;
        this.recipient   = recipient;
        this.appointment = appointment;
    }

    public String getType()                      { return type; }
    public void   setType(String type)           { this.type = type; }
    public String getChannel()                   { return channel; }
    public void   setChannel(String channel)     { this.channel = channel; }
    public RecipientInfo getRecipient()          { return recipient; }
    public void   setRecipient(RecipientInfo r)  { this.recipient = r; }
    public AppointmentInfo getAppointment()      { return appointment; }
    public void   setAppointment(AppointmentInfo a) { this.appointment = a; }

    // ------------------------------------------------------------------ //
    //  Nested DTOs — kept inner to travel as one unit over the wire       //
    // ------------------------------------------------------------------ //

    public static class RecipientInfo {
        private String name;
        private String email;
        private String phone;

        public RecipientInfo() {}
        public RecipientInfo(String name, String email, String phone) {
            this.name  = name;
            this.email = email;
            this.phone = phone;
        }

        public String getName()              { return name; }
        public void   setName(String name)   { this.name = name; }
        public String getEmail()             { return email; }
        public void   setEmail(String email) { this.email = email; }
        public String getPhone()             { return phone; }
        public void   setPhone(String phone) { this.phone = phone; }
    }

    public static class AppointmentInfo {
        private Long   appointmentId;
        private String startTime;
        private String endTime;
        private double totalPrice;
        private String notes;

        public AppointmentInfo() {}
        public AppointmentInfo(Long appointmentId, String startTime, String endTime,
                                double totalPrice, String notes) {
            this.appointmentId = appointmentId;
            this.startTime     = startTime;
            this.endTime       = endTime;
            this.totalPrice    = totalPrice;
            this.notes         = notes;
        }

        public Long   getAppointmentId()                  { return appointmentId; }
        public void   setAppointmentId(Long id)           { this.appointmentId = id; }
        public String getStartTime()                      { return startTime; }
        public void   setStartTime(String startTime)      { this.startTime = startTime; }
        public String getEndTime()                        { return endTime; }
        public void   setEndTime(String endTime)          { this.endTime = endTime; }
        public double getTotalPrice()                     { return totalPrice; }
        public void   setTotalPrice(double totalPrice)    { this.totalPrice = totalPrice; }
        public String getNotes()                          { return notes; }
        public void   setNotes(String notes)              { this.notes = notes; }
    }
}
