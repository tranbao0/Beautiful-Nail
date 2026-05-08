CREATE TABLE IF NOT EXISTS appointment_services (
    apmt_id    INTEGER NOT NULL,
    service_id INTEGER NOT NULL,
    PRIMARY KEY (apmt_id, service_id),
    FOREIGN KEY (apmt_id)    REFERENCES appointments(apmt_id),
    FOREIGN KEY (service_id) REFERENCES services(service_id)
);
