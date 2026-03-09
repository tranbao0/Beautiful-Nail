-- Users
INSERT INTO users (first_name, m_init, last_name, email, phone, role, password_hash) VALUES
('Jane',  'A', 'Doe',      'jane.doe@example.com',      '4081110001', 'customer',     'hashed_pw_1'),
('Mike',  'B', 'Smith',    'mike.smith@example.com',    '4081110002', 'customer',     'hashed_pw_2'),
('Linda', 'C', 'Reception','linda.r@beautifulnail.com', '4081110000', 'receptionist', 'hashed_pw_3');

-- Stylists
INSERT INTO stylists (first_name, m_init, last_name, email, phone, specialty) VALUES
('Jenny', 'L', 'Lee',  'jenny@beautifulnail.com', '4082220001', 'Gel, Acrylics'),
('Mia',   'T', 'Tran', 'mia@beautifulnail.com',   '4082220002', 'Manicure, Pedicure'),
('Sara',  'K', 'Kim',  'sara@beautifulnail.com',  '4082220003', 'Nail Art, Pedicure');

-- Services
INSERT INTO services (name, price_min, price_max, est_duration) VALUES
('Classic Manicure', 25.00, 35.00, 45),
('Gel Manicure',     40.00, 55.00, 60),
('Classic Pedicure', 35.00, 45.00, 50),
('Acrylic Full Set', 55.00, 75.00, 90),
('Gel Pedicure',     50.00, 65.00, 65),
('Nail Art Add-on',  15.00, 30.00, 30);

-- Availability slots (stylist_id 1 = Jenny, 2 = Mia, 3 = Sara)
INSERT INTO availability_slots (stylist_id, start_time, end_time, is_available) VALUES
(1, '2026-03-10T10:00:00', '2026-03-10T10:45:00', 1),
(1, '2026-03-10T11:00:00', '2026-03-10T11:45:00', 1),
(1, '2026-03-10T13:00:00', '2026-03-10T13:45:00', 1),
(1, '2026-03-10T14:00:00', '2026-03-10T14:45:00', 0),
(2, '2026-03-10T09:00:00', '2026-03-10T09:50:00', 1),
(2, '2026-03-10T11:00:00', '2026-03-10T11:50:00', 1),
(2, '2026-03-10T13:30:00', '2026-03-10T14:20:00', 1),
(3, '2026-03-10T09:30:00', '2026-03-10T10:00:00', 1),
(3, '2026-03-10T12:00:00', '2026-03-10T12:50:00', 1),
(3, '2026-03-10T14:00:00', '2026-03-10T14:50:00', 1);
