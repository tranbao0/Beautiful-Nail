CREATE TABLE IF NOT EXISTS stylist_services (
    stylist_id INTEGER NOT NULL,
    service_id INTEGER NOT NULL,
    PRIMARY KEY (stylist_id, service_id),
    FOREIGN KEY (stylist_id) REFERENCES stylists(stylist_id),
    FOREIGN KEY (service_id) REFERENCES services(service_id)
);
