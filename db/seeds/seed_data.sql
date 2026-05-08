-- Users  (password for all seed accounts: "password")
-- password_hash = SHA-256("password") = 5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8
INSERT INTO users (first_name, m_init, last_name, email, phone, role, password_hash) VALUES
('Jane',  'A', 'Doe',      'jane.doe@example.com',      '4081110001', 'customer',     '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8'),
('Mike',  'B', 'Smith',    'mike.smith@example.com',    '4081110002', 'customer',     '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8'),
('Linda', 'C', 'Reception','linda.r@beautifulnail.com', '4081110000', 'receptionist', '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8');

-- Stylists
-- specialty field is a short display label; actual service offerings are in stylist_services
INSERT INTO stylists (first_name, m_init, last_name, email, phone, specialty) VALUES
('Jenny', 'L', 'Lee',   'jenny@beautifulnail.com',  '4082220001', 'Chrome Powder, Gel Extensions'),
('Mia',   'T', 'Tran',  'mia@beautifulnail.com',    '4082220002', 'Full-Service Nails'),
('Sara',  'K', 'Kim',   'sara@beautifulnail.com',   '4082220003', 'Nail Art, Acrylics, Gel'),
('Priya', 'S', 'Patel', 'priya@beautifulnail.com',  '4082220004', 'Manicure Specialist'),
('Rosa',  'M', 'Nguyen','rosa@beautifulnail.com',   '4082220005', 'Spa & Color Design'),
('Yuki',  'H', 'Tanaka','yuki@beautifulnail.com',   '4082220006', '3D Nail Art'),
('Chloe', 'J', 'Park',  'chloe@beautifulnail.com',  '4082220007', 'Gel & Acrylic Specialist');

-- Services (grouped by rarity; rarity is informational — it drives stylist assignment logic)
-- Rarity 1 – Common
INSERT INTO services (name, price_min, price_max, est_duration) VALUES
('Classic Manicure',     25.00, 35.00, 45),
('Classic Pedicure',     35.00, 45.00, 50),
('French Manicure',      30.00, 40.00, 45),
('Polish Change',        10.00, 15.00, 15),
('Basic Nail Repair',     8.00, 15.00, 15);
-- Rarity 2 – Uncommon
INSERT INTO services (name, price_min, price_max, est_duration) VALUES
('Gel Manicure',         40.00, 55.00, 60),
('Gel Pedicure',         50.00, 65.00, 65),
('Acrylic Fill',         35.00, 45.00, 45);
-- Rarity 3 – Rare
INSERT INTO services (name, price_min, price_max, est_duration) VALUES
('Acrylic Full Set',     55.00, 75.00, 90),
('Nail Art Add-on',      15.00, 30.00, 30),
('Gel Extensions',       60.00, 80.00, 75),
('Ombre Nails',          45.00, 60.00, 60);
-- Rarity 4 – Very Rare
INSERT INTO services (name, price_min, price_max, est_duration) VALUES
('Spa Treatment',        65.00, 85.00, 75),
('3D Nail Art',          50.00, 80.00, 90),
('Chrome Powder Finish', 20.00, 35.00, 30);

-- Stylist → service mappings
-- Rule: fewer services = rarer specialties; more services = more common/accessible
-- Yuki  (1 service):  very rare only
-- Jenny (2 services): very rare + rare
-- Rosa  (2 services): very rare + rare
-- Sara  (3 services): rare + rare + uncommon
-- Priya (4 services): uncommon + uncommon + common + common
-- Chloe (4 services): uncommon + uncommon + common + common
-- Mia   (5 services): common × 4 + uncommon
INSERT INTO stylist_services (stylist_id, service_id) VALUES
-- Jenny: Chrome Powder Finish [very rare] + Gel Extensions [rare]
(1, (SELECT service_id FROM services WHERE name = 'Chrome Powder Finish')),
(1, (SELECT service_id FROM services WHERE name = 'Gel Extensions')),
-- Mia: Polish Change + Classic Manicure + Classic Pedicure + French Manicure [common x4] + Gel Manicure [uncommon]
(2, (SELECT service_id FROM services WHERE name = 'Polish Change')),
(2, (SELECT service_id FROM services WHERE name = 'Classic Manicure')),
(2, (SELECT service_id FROM services WHERE name = 'Classic Pedicure')),
(2, (SELECT service_id FROM services WHERE name = 'French Manicure')),
(2, (SELECT service_id FROM services WHERE name = 'Gel Manicure')),
-- Sara: Acrylic Full Set + Nail Art Add-on [rare x2] + Gel Manicure [uncommon]
(3, (SELECT service_id FROM services WHERE name = 'Acrylic Full Set')),
(3, (SELECT service_id FROM services WHERE name = 'Nail Art Add-on')),
(3, (SELECT service_id FROM services WHERE name = 'Gel Manicure')),
-- Priya: Acrylic Fill + Gel Manicure [uncommon x2] + Classic Manicure + French Manicure + Basic Nail Repair [common x3]
(4, (SELECT service_id FROM services WHERE name = 'Acrylic Fill')),
(4, (SELECT service_id FROM services WHERE name = 'Gel Manicure')),
(4, (SELECT service_id FROM services WHERE name = 'Classic Manicure')),
(4, (SELECT service_id FROM services WHERE name = 'French Manicure')),
(4, (SELECT service_id FROM services WHERE name = 'Basic Nail Repair')),
-- Rosa: Spa Treatment [very rare] + Ombre Nails [rare]
(5, (SELECT service_id FROM services WHERE name = 'Spa Treatment')),
(5, (SELECT service_id FROM services WHERE name = 'Ombre Nails')),
-- Yuki: 3D Nail Art [very rare]
(6, (SELECT service_id FROM services WHERE name = '3D Nail Art')),
-- Chloe: Gel Pedicure + Acrylic Fill [uncommon x2] + Classic Pedicure + Classic Manicure [common x2]
(7, (SELECT service_id FROM services WHERE name = 'Gel Pedicure')),
(7, (SELECT service_id FROM services WHERE name = 'Acrylic Fill')),
(7, (SELECT service_id FROM services WHERE name = 'Classic Pedicure')),
(7, (SELECT service_id FROM services WHERE name = 'Classic Manicure'));

-- Availability slots — uses date('now') so slots are always current when re-seeded
-- Jenny (1): specialized, mid-morning and early afternoon
-- Mia (2): generalist, morning and midday
-- Sara (3): nail art/acrylics, limited early and late slots
-- Priya (4): manicure specialist, mornings only
-- Rosa (5): spa/color, afternoons only
-- Yuki (6): 3D nail art, very limited slots
-- Chloe (7): gel/acrylic, dense full-day schedule
INSERT INTO availability_slots (stylist_id, start_time, end_time, is_available) VALUES
(1, date('now') || 'T10:00:00', date('now') || 'T10:45:00', 1),
(1, date('now') || 'T11:00:00', date('now') || 'T11:45:00', 1),
(1, date('now') || 'T13:00:00', date('now') || 'T13:45:00', 1),
(1, date('now', '+1 day') || 'T10:00:00', date('now', '+1 day') || 'T10:45:00', 1),
(1, date('now', '+1 day') || 'T14:00:00', date('now', '+1 day') || 'T14:45:00', 1),
(2, date('now') || 'T09:00:00', date('now') || 'T09:50:00', 1),
(2, date('now') || 'T11:00:00', date('now') || 'T11:50:00', 1),
(2, date('now') || 'T14:00:00', date('now') || 'T14:50:00', 1),
(2, date('now', '+1 day') || 'T09:30:00', date('now', '+1 day') || 'T10:20:00', 1),
(2, date('now', '+1 day') || 'T13:30:00', date('now', '+1 day') || 'T14:20:00', 1),
(3, date('now') || 'T09:30:00', date('now') || 'T10:15:00', 1),
(3, date('now') || 'T15:00:00', date('now') || 'T15:50:00', 1),
(3, date('now', '+1 day') || 'T12:00:00', date('now', '+1 day') || 'T12:50:00', 1),
(3, date('now', '+1 day') || 'T14:00:00', date('now', '+1 day') || 'T14:50:00', 1),
(4, date('now') || 'T09:00:00', date('now') || 'T09:45:00', 1),
(4, date('now') || 'T10:00:00', date('now') || 'T10:45:00', 1),
(4, date('now') || 'T11:00:00', date('now') || 'T11:45:00', 1),
(4, date('now', '+1 day') || 'T09:00:00', date('now', '+1 day') || 'T09:45:00', 1),
(4, date('now', '+1 day') || 'T10:00:00', date('now', '+1 day') || 'T10:45:00', 1),
(5, date('now') || 'T13:00:00', date('now') || 'T14:00:00', 1),
(5, date('now') || 'T14:30:00', date('now') || 'T15:30:00', 1),
(5, date('now') || 'T15:30:00', date('now') || 'T16:30:00', 1),
(5, date('now', '+1 day') || 'T13:00:00', date('now', '+1 day') || 'T14:00:00', 1),
(5, date('now', '+1 day') || 'T15:00:00', date('now', '+1 day') || 'T16:00:00', 1),
(6, date('now') || 'T11:00:00', date('now') || 'T12:00:00', 1),
(6, date('now', '+1 day') || 'T10:00:00', date('now', '+1 day') || 'T11:00:00', 1),
(6, date('now', '+1 day') || 'T14:30:00', date('now', '+1 day') || 'T15:30:00', 1),
(7, date('now') || 'T09:00:00', date('now') || 'T10:00:00', 1),
(7, date('now') || 'T10:00:00', date('now') || 'T11:00:00', 1),
(7, date('now') || 'T11:00:00', date('now') || 'T12:00:00', 1),
(7, date('now') || 'T13:00:00', date('now') || 'T14:00:00', 1),
(7, date('now') || 'T14:00:00', date('now') || 'T15:00:00', 1),
(7, date('now', '+1 day') || 'T09:00:00', date('now', '+1 day') || 'T10:00:00', 1),
(7, date('now', '+1 day') || 'T11:00:00', date('now', '+1 day') || 'T12:00:00', 1),
(7, date('now', '+1 day') || 'T14:00:00', date('now', '+1 day') || 'T15:00:00', 1);
