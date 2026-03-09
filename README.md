# Beautiful Nail – Online Appointment Scheduling System
**CMPE 172 Term Project | Spring 2026**

A Spring Boot enterprise-style backend for managing salon appointments at Beautiful Nail. Customers can browse services, view stylist availability, and book or cancel appointments. The receptionist can manage stylists, services, and schedules.

---

## Tech Stack
- **Language:** Java 17
- **Framework:** Spring Boot
- **Database:** MySQL (accessed via JDBC — no ORM)
- **Build Tool:** Maven (`pom.xml`)

---

## Repository Structure

```
beautiful-nail-scheduler/
├── pom.xml                          # Maven build config and dependencies
├── .gitignore
├── README.md
│
├── src/
│   ├── main/
│   │   ├── java/com/beautifulnail/scheduler/
│   │   │   ├── controller/          # HTTP layer — REST endpoints for customers & receptionist
│   │   │   ├── service/             # Business logic — booking rules, conflict checks, scheduling
│   │   │   ├── repository/          # JDBC data access — raw SQL queries per entity
│   │   │   ├── model/               # POJOs — User, Stylist, Service, Availability, Appointment
│   │   │   ├── config/              # Spring config — DataSource, JDBC template, transaction manager
│   │   │   └── notification/        # Mock external notification service (M5 distribution boundary)
│   │   └── resources/
│   │       ├── application.properties   # DB connection, server port, logging config
│   │       ├── templates/           # Server-rendered HTML templates (Thymeleaf)
│   │       └── static/
│   │           ├── css/             # Stylesheets
│   │           └── js/              # Client-side scripts
│   └── test/
│       └── java/com/beautifulnail/scheduler/
│           ├── controller/          # Controller-layer tests
│           ├── service/             # Service-layer unit tests (booking logic, concurrency)
│           └── repository/          # Repository-layer integration tests
│
├── db/
│   ├── migrations/                  # Ordered SQL scripts to create/alter schema (V1__, V2__, ...)
│   └── seeds/                       # SQL scripts to insert sample/test data
│
├── docs/
│   ├── milestones/                  # Submitted PDFs for M1–M6
│   └── diagrams/                    # ER diagram, distribution diagram, architecture diagrams
│
└── scripts/                         # Shell scripts for DB setup, local run, or CI helpers
```

---

## Package Responsibilities

### `controller/`
Spring `@RestController` or `@Controller` classes. One controller per user-facing domain area:
- `AppointmentController` — book, cancel, view, reschedule
- `AvailabilityController` — browse open time slots
- `ServiceController` — list services and pricing
- `ReceptionistController` — admin endpoints for managing stylists, services, and schedules
- `HealthController` — `GET /health` endpoint (M6)

### `service/`
Business logic layer. Controllers call services; services call repositories. Never touches HTTP or SQL directly.
- `AppointmentService` — booking flow, double-booking prevention, `@Transactional` methods
- `AvailabilityService` — slot lookup and status updates
- `NotificationService` — calls the mock external notification boundary (M5)

### `repository/`
JDBC-based data access. One repository per entity. Uses `JdbcTemplate` with hand-written SQL. No ORM.
- `UserRepository`, `StylistRepository`, `ServiceRepository`, `AvailabilityRepository`, `AppointmentRepository`

### `model/`
Plain Java objects (POJOs) that map to database rows:
- `User`, `Stylist`, `Service`, `Availability`, `Appointment`

### `config/`
Spring configuration classes:
- `DataSourceConfig` — MySQL `DataSource` bean
- `TransactionConfig` — `PlatformTransactionManager` setup
- `WebConfig` — MVC or CORS settings if needed

### `notification/`
Mock external service simulating a confirmation notification system (M5 — Distribution Boundary). Contains:
- `MockNotificationController` — a separate Spring controller acting as the "remote" service
- `NotificationClient` — HTTP client that calls the mock endpoint from the main app

---

## Database (`db/`)

### `db/migrations/`
Numbered SQL scripts applied in order to build the schema. Naming convention: `V1__create_users.sql`, `V2__create_stylists.sql`, etc.

Entities (from ER/Relational Schema — M2):

| Table                  | Description                                       |
|------------------------|---------------------------------------------------|
| `users`                | Customers and receptionists (role-differentiated) |
| `stylists`             | Salon staff who perform services                  |
| `services`             | Offered services with price range and duration    |
| `availability_slots`   | Time blocks a stylist is open for booking         |
| `appointments`         | Booked sessions linking user, stylist, and slots  |
| `appointment_services` | Join table for M:N appointments ↔ services        |

### `db/seeds/`
Sample data scripts for local development and testing (e.g., demo stylists, services, users).

---

## Docs (`docs/`)

### `docs/milestones/`
One PDF per milestone submission:
- `M1_Proposal.pdf`
- `M2_ER_Diagram.pdf`, `M2_Relational_Schema.pdf`
- `M3_DesignNotes.pdf`
- `M4_Concurrency.pdf`
- `M5_DistributionNotes.pdf`
- `M6_SysMGMT.pdf`
- `Final_Report.pdf`

### `docs/diagrams/`
Visual architecture and design artifacts:
- ER diagram
- Distribution boundary diagram (Client → Scheduler → Notification Service)
- System block diagram

---

## Key Architectural Notes

**Request Flow (M3):**
```
HTTP Request → Controller → Service → Repository → MySQL (via JDBC)
```

**Double-Booking Prevention (M4):**
Handled in `AppointmentService` using `@Transactional` with `SERIALIZABLE` or `REPEATABLE_READ` isolation. The booking query checks slot availability and locks the row before inserting, preventing race conditions when two users attempt to book the same slot concurrently.

**Distribution Boundary (M5):**
After a successful booking, `AppointmentService` calls `NotificationClient`, which sends an HTTP POST to `MockNotificationController`. This simulates a coarse-grained external notification service boundary.

**Health & Logging (M6):**
- `GET /health` returns system status and DB connectivity
- SLF4J logging at INFO/WARN/ERROR levels throughout the service layer

---

## Running Locally

1. Create a MySQL database: `CREATE DATABASE beautiful_nail;`
2. Update `src/main/resources/application.properties` with your DB credentials
3. Run migrations in order from `db/migrations/`
4. Optionally seed data from `db/seeds/`
5. `mvn spring-boot:run`
6. App runs at `http://localhost:8080`
