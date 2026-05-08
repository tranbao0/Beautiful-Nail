# M6 Implementation Guide - System Management + Logging Plan

## Files Created/Modified

### New Files Created:
1. **MetricsCollector.java** - Core metrics collection component
   - Location: `src/main/java/com/beautifulnail/scheduler/service/MetricsCollector.java`
   - Tracks: bookings/hour, failed bookings, latency samples, failure reasons

2. **MetricsController.java** - REST endpoints for metrics exposure
   - Location: `src/main/java/com/beautifulnail/scheduler/controller/MetricsController.java`
   - Endpoints: `/metrics`, `/metrics/bookings`, `/metrics/bookings/latency`, `/metrics/bookings/failed`

### Files Modified:
1. **NotificationService.java** - Enhanced with retry and fallback logic
   - Added exponential backoff retry (1s, 2s, 4s)
   - Added async retry queue with 24-hour TTL
   - Added @Scheduled task for queue processing (every 30 seconds)
   - Added degradation detection

2. **AppointmentService.java** - Integrated metrics collection
   - Added MetricsCollector injection
   - Records latency for each booking attempt
   - Records success/failure with reasons
   - Enhanced logging with timing information

3. **HealthController.java** - Enhanced health checks
   - Added database connectivity check
   - Added notification service health check
   - Returns HTTP 200 (UP/DEGRADED) or 503 (DOWN)
   - JSON response with component status

4. **application.properties** - Logging configuration
   - File-based logging with rotation
   - Pattern format with timestamp, thread, level
   - Scheduling pool size for async tasks

5. **SchedulerApplication.java** - Main app class
   - Added @EnableScheduling annotation for async retry task

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────┐
│                    Client Requests                       │
└────────┬──────────────────────────────────────────────────┘
         │
         ├─────────────────────┬─────────────────────┐
         ▼                     ▼                     ▼
    ┌─────────┐          ┌─────────┐          ┌──────────┐
    │Booking  │          │Health   │          │Metrics   │
    │Endpoint │          │Endpoint │          │Endpoints │
    └────┬────┘          └────┬────┘          └─────┬────┘
         │                    │                     │
         ▼                    ▼                     ▼
    ┌─────────────────────────────────────────────────────┐
    │              Spring Boot Services                    │
    │  ┌──────────────────────────────────────────────┐   │
    │  │  AppointmentService                           │   │
    │  │  - Records latency                            │   │
    │  │  - Records success/failure with reasons       │   │
    │  │  - Logs at appropriate levels                 │   │
    │  └──────────────┬──────────────────────────────┘   │
    │                 │                                    │
    │  ┌──────────────┴──────────────────────────────┐   │
    │  │  MetricsCollector (thread-safe)              │   │
    │  │  - Tracks bookings/hour                       │   │
    │  │  - Tracks failures (categorized)              │   │
    │  │  - Tracks latency percentiles (p99)           │   │
    │  └──────────────┬──────────────────────────────┘   │
    │                 │                                    │
    │                 ├─────────────────────┐             │
    │                 ▼                     ▼             │
    │            ┌─────────────┐      ┌──────────────┐   │
    │            │SLF4J Logger │      │Health Checks │   │
    │            └──────┬──────┘      │- Database    │   │
    │                   │             │- Notifications   │
    │            ┌──────▼──────┐      └──────────────┘   │
    │            │Application  │                          │
    │            │Log File     │                          │
    │            └─────────────┘                          │
    │                                                      │
    │  ┌──────────────────────────────────────────────┐   │
    │  │  NotificationService (with Resilience)       │   │
    │  │  ┌──────────────────────────────────────┐    │   │
    │  │  │ Send with Retry (3x exponential)     │    │   │
    │  │  │ Attempt 1: immediate                 │    │   │
    │  │  │ Attempt 2: wait 1s, retry            │    │   │
    │  │  │ Attempt 3: wait 2s, retry            │    │   │
    │  │  │ Failed: queue for async              │    │   │
    │  │  └──────────────────────────────────────┘    │   │
    │  │                                               │   │
    │  │  ┌──────────────────────────────────────┐    │   │
    │  │  │ Async Retry Queue (24h TTL)          │    │   │
    │  │  │ - Scheduled task runs every 30s      │    │   │
    │  │  │ - Retries failed notifications       │    │   │
    │  │  │ - Removes expired items              │    │   │
    │  │  └──────────────────────────────────────┘    │   │
    │  └──────────────────────────────────────────────┘   │
    │                                                      │
    └─────────────────────────────────────────────────────┘
         │
         ▼
    ┌──────────────┐
    │SQLite DB     │
    └──────────────┘
```

---

## Logging Strategy

### Log Levels Used:

**DEBUG:**
- Detailed database operations
- Retry timing information
- Queue processing details

**INFO:**
- Successful bookings
- Successful notifications
- Successful cancellations
- Metrics snapshots (optional)

**WARN:**
- Booking retry attempts
- Database contention
- Failed notification retries
- Service degradation detection

**ERROR:**
- Final booking failures after all retries
- Notification failures requiring queue storage
- Health check failures
- Critical system errors

### Log File Configuration:
- Location: `logs/application.log`
- Rotation: 10MB per file, keep 10 files
- Format: `[timestamp] [thread] [level] [logger] - message`

---

## Metrics Collection Flow

### Booking Success Path:
```
User Request
    ↓
AppointmentService.bookAppointmentWithRetry()
    ↓ (start timer)
    ↓
AppointmentService.bookAppointment()
    ├─ Check slot available
    ├─ Mark slot unavailable
    ├─ Save appointment
    ├─ Send notification
    ↓
[SUCCESS]
    ↓
metricsCollector.recordBookingSuccess()
metricsCollector.recordLatency(latencyMs)
    ↓
Log: "Booking completed successfully in XXXms"
```

### Booking Failure Path with Retry:
```
User Request
    ↓
AppointmentService.bookAppointmentWithRetry()
    ↓ (start timer)
    ↓
[Attempt 1] → FAIL (slot taken)
    ↓ [WARN] Attempt 1/3 failed
    ↓
[Attempt 2] → FAIL (database locked)
    ↓ [WARN] Attempt 2/3 failed
    ↓
[Attempt 3] → FAIL (slot unavailable)
    ↓ [WARN] Attempt 3/3 failed
    ↓
[FAILURE]
    ↓
metricsCollector.recordBookingFailure("slot_unavailable")
metricsCollector.recordLatency(latencyMs)
    ↓
Log: "Booking failed after 3 attempts (XXXms)"
```

---

## Three Core Metrics

### 1. Bookings Per Hour
**What it measures:** Business volume and user demand
**Collection:** Incremented on successful booking
**Reset:** Automatic hourly reset
**API:** `GET /metrics/bookings`
**Alert threshold:** If hourly bookings drop below 5 (potential outage)

**Example Response:**
```json
{
  "bookings_this_hour": 23,
  "failed_bookings": 2,
  "failure_rate_percent": "8.00"
}
```

### 2. Failed Bookings (with Reasons)
**What it measures:** User experience impact and system reliability
**Collection:** Recorded with categorized reason
**Categories:** "slot_unavailable", "database_locked", others
**API:** `GET /metrics/bookings/failed`
**Alert threshold:** If failure rate > 10% in 5-minute window

**Example Response:**
```json
{
  "total_failures": 2,
  "failure_rate_percent": "8.00",
  "failures_by_reason": {
    "slot_unavailable": 1,
    "database_locked": 1
  }
}
```

### 3. Average Latency (with Percentiles)
**What it measures:** User-facing performance and bottlenecks
**Collection:** Recorded for both success and failure paths
**Metrics:** Average, p99 (99th percentile), min, max
**API:** `GET /metrics/bookings/latency`
**Alert thresholds:**
- p99 > 5s → WARNING
- p99 > 10s → ERROR (cascading failure indicator)

**Example Response:**
```json
{
  "avg_latency_ms": "245.67",
  "p99_latency_ms": 1850,
  "min_latency_ms": 45,
  "max_latency_ms": 3200,
  "sample_count": 25
}
```

---

## Failure Scenario & Recovery: Notification Service Unavailable

### Scenario Description:
External notification service becomes temporarily unavailable (HTTP 503 or timeout).
Bookings should succeed, but confirmations fail initially with graceful recovery.

### Timeline:

**T+0s:** Notification service goes down
```
[INFO] Sending confirmation for appointment 127
[WARN] Notification attempt 1/3 failed for appointment 127: Service Unavailable
```

**T+1s:** First retry with exponential backoff
```
[DEBUG] Retrying notification in 1000ms
[WARN] Notification attempt 2/3 failed for appointment 127: Service Unavailable
```

**T+3s:** Second retry
```
[DEBUG] Retrying notification in 2000ms
[WARN] Notification attempt 3/3 failed for appointment 127: Service Unavailable
```

**T+7s:** All retries exhausted, queue for async
```
[ERROR] Notification failed after 3 retries for appointment 127; queueing for async retry
[INFO] Notification queued for async retry (queue size: 1)
```

**T+30s:** Async retry task runs periodically
```
[DEBUG] Processing retry queue (size: 1)
[INFO] Queued notification sent successfully for appointment 127
```

**T+35s:** Health check reflects recovery
```
GET /health
{
  "status": "UP",  // Changed from DEGRADED to UP
  "checks": {
    "notification_service": {
      "status": "UP",
      "message": "External notification service responding"
    }
  }
}
```

### Recovery Strategies Implemented:

1. **Exponential Backoff Retry**
   - Attempts: 3
   - Backoff: 1s, 2s, 4s
   - Prevents thundering herd during outages

2. **In-Memory Queue with TTL**
   - Capacity: Unlimited (but memory-aware)
   - TTL: 24 hours
   - Automatic cleanup of expired items

3. **Async Periodic Processing**
   - Schedule: Every 30 seconds
   - Non-blocking: Doesn't block booking operations
   - Graceful: Continues retrying failed items

4. **User-Accessible Fallback**
   - Endpoint: `GET /appointments/<id>/confirmation`
   - Users can always check booking status even if email fails

5. **Operational Alerts**
   - Initial WARN after first failed retry
   - ERROR after queue grows (degradation detected)
   - Health endpoint reports DEGRADED state

---

## Health Check Endpoint Response Codes & Bodies

### Endpoint: `GET /health`

### HTTP 200 OK - UP
```json
{
  "status": "UP",
  "timestamp": "2026-04-29T14:35:22Z",
  "checks": {
    "database": {
      "status": "UP",
      "message": "SQLite connected"
    },
    "notification_service": {
      "status": "UP",
      "message": "External notification service responding"
    }
  }
}
```

### HTTP 200 OK - DEGRADED
```json
{
  "status": "DEGRADED",
  "timestamp": "2026-04-29T14:36:10Z",
  "checks": {
    "database": {
      "status": "UP",
      "message": "SQLite connected"
    },
    "notification_service": {
      "status": "DOWN",
      "message": "Notification service degraded - 7 notifications in retry queue"
    }
  }
}
```

### HTTP 503 Service Unavailable - DOWN
```json
{
  "status": "DOWN",
  "timestamp": "2026-04-29T14:37:05Z",
  "error": "Cannot serve requests",
  "checks": {
    "database": {
      "status": "DOWN",
      "message": "JDBC connection refused: Connection refused"
    }
  }
}
```

---

## Testing the Implementation

### 1. Test Normal Booking Flow
```bash
# Make a booking request
curl -X POST http://localhost:8080/appointments/book \
  -d "slotId=1&serviceId=1&notes=Test"

# Check metrics
curl http://localhost:8080/metrics/bookings
# Expected: bookings_this_hour should increment
```

### 2. Test Notification Retry Logic
```bash
# Step 1: Stop external notification service
# (The app won't fail bookings, will queue notifications)

# Step 2: Make bookings
curl -X POST http://localhost:8080/appointments/book \
  -d "slotId=1&serviceId=1"

# Step 3: Check health shows DEGRADED
curl http://localhost:8080/health
# Expected status: DEGRADED, queued notifications > 0

# Step 4: Restart notification service

# Step 5: Wait 30 seconds for async retry task

# Step 6: Check health shows UP again
curl http://localhost:8080/health
# Expected status: UP
```

### 3. Test Latency Metrics
```bash
# Make multiple bookings
for i in {1..10}; do
  curl -X POST http://localhost:8080/appointments/book \
    -d "slotId=$i&serviceId=1" &
done

# Check latency metrics
curl http://localhost:8080/metrics/bookings/latency
# Expected: avg_latency_ms, p99_latency_ms populated
```

### 4. Check Logs
```bash
# View log file
tail -f logs/application.log

# Filter for INFO messages
grep "\[INFO\]" logs/application.log

# Filter for ERROR messages
grep "\[ERROR\]" logs/application.log

# Filter for specific appointment
grep "appointment 127" logs/application.log
```

---

## Configuration Options

### application.properties

```properties
# Logging level
logging.level.com.beautifulnail=INFO        # Main app: INFO
logging.level.org.springframework.jdbc=DEBUG # Database: DEBUG

# File output
logging.file.name=logs/application.log      # File location
logging.file.max-size=10MB                  # Rotation size
logging.file.max-history=10                 # Keep 10 files

# Async task scheduling
spring.task.scheduling.pool.size=2          # Thread pool for @Scheduled
```

### Tuning Parameters (in code):

**NotificationService:**
```java
private static final int MAX_RETRIES = 3;                    // Number of retries
private static final int[] BACKOFF_MS = {1000, 2000, 4000}; // Backoff schedule
private static final long RETRY_QUEUE_TTL_MS = 24 * 60 * 60 * 1000; // 24 hours
```

**Scheduled Task:**
```java
@Scheduled(fixedDelay = 30000, initialDelay = 30000)  // Every 30 seconds
public void processRetryQueue() { ... }
```

**MetricsCollector:**
```java
if (latencySamples.size() > 1000) {  // Keep last 1000 samples
    latencySamples.remove(0);
}
```

---

## Integration with M1-M5 Milestones

- **M1 (Proposal):** Logging/monitoring is part of operational planning
- **M2 (DB Design):** Logs don't go to DB, file-based only
- **M3 (Web Interface):** Health endpoint is REST-based
- **M4 (Concurrency):** Retry logic demonstrates transaction safety
- **M5 (Distribution Boundary):** Notification service resilience shown here
- **M6 (This):** Ties everything together with observability

---

## Key Design Decisions

1. **In-Memory Queue vs Database Queue**
   - Chosen: In-memory for simplicity and speed
   - Trade-off: Lost on restart (acceptable for 24h TTL)

2. **Exponential Backoff Schedule**
   - Chosen: 1s, 2s, 4s (doubles each time)
   - Rationale: Prevents overwhelming failed service

3. **Async Task Every 30 Seconds**
   - Chosen: 30s interval
   - Rationale: Balance between latency (user notification delay) and resource efficiency

4. **p99 Percentile vs Average**
   - Both tracked: Average is misleading with outliers
   - p99 reveals user experience for 99% of requests

5. **Health Checks on Every Request**
   - Chosen: Yes (database check is fast with SQLite)
   - Alternative: Could cache for 5-10s to reduce load

6. **Failure Categorization**
   - Reasons: "slot_unavailable", "database_locked"
   - Allows targeted alerting and debugging
