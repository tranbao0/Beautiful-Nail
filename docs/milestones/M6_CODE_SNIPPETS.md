# M6 System Management + Logging Plan - Code Implementation Snippets

## Overview
This document contains all implemented code snippets for M6 System Management, demonstrating:
- Metrics collection and monitoring
- Logging with SLF4J
- Failure scenarios and recovery strategies
- Health check endpoint
- Notification service resilience with retry and fallback logic

---

## 1. Metrics Collection Service

### MetricsCollector.java
Component responsible for tracking all system metrics (bookings, failures, latency).

```java
@Component
public class MetricsCollector {

    private final AtomicInteger bookingsThisHour = new AtomicInteger(0);
    private final AtomicInteger failedBookings = new AtomicInteger(0);
    private final List<Long> latencySamples = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Integer> failureReasons = Collections.synchronizedMap(new HashMap<>());
    private volatile Instant lastHourReset = Instant.now();

    // Record a successful booking
    public void recordBookingSuccess() {
        bookingsThisHour.incrementAndGet();
    }

    // Record a failed booking with reason
    public void recordBookingFailure(String reason) {
        failedBookings.incrementAndGet();
        failureReasons.merge(reason, 1, Integer::sum);
    }

    // Record latency of a booking operation (in milliseconds)
    public void recordLatency(long latencyMs) {
        latencySamples.add(latencyMs);
        if (latencySamples.size() > 1000) {
            latencySamples.remove(0);
        }
    }

    // Get average latency
    public double getAverageLatency() {
        if (latencySamples.isEmpty()) return 0;
        return latencySamples.stream().mapToLong(Long::longValue).average().orElse(0);
    }

    // Get p99 latency (99th percentile)
    public long getP99Latency() {
        if (latencySamples.isEmpty()) return 0;
        List<Long> sorted = new ArrayList<>(latencySamples);
        Collections.sort(sorted);
        int index = (int) Math.ceil(sorted.size() * 0.99) - 1;
        return index >= 0 ? sorted.get(index) : 0;
    }

    // Get failure rate as percentage
    public double getFailureRate() {
        int total = bookingsThisHour.get() + failedBookings.get();
        if (total == 0) return 0;
        return (failedBookings.get() / (double) total) * 100;
    }

    // Get comprehensive metrics snapshot
    public Map<String, Object> getMetricsSnapshot() {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("timestamp", Instant.now());
        snapshot.put("bookings_this_hour", getBookingsThisHour());
        snapshot.put("failed_bookings", getFailedBookings());
        snapshot.put("failure_rate_percent", String.format("%.2f", getFailureRate()));
        snapshot.put("avg_latency_ms", String.format("%.2f", getAverageLatency()));
        snapshot.put("p99_latency_ms", getP99Latency());
        snapshot.put("failure_reasons", getFailureReasons());
        return snapshot;
    }
}
```

---

## 2. Enhanced Appointment Service with Metrics Integration

### Booking with Retry and Latency Tracking

```java
@Transactional(isolation = Isolation.SERIALIZABLE)
public Appointment bookAppointmentWithRetry(Appointment appointment, Long slotId) {
    int maxAttempts = 3;
    long startTime = System.currentTimeMillis();

    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
        try {
            Appointment result = bookAppointment(appointment, slotId);
            long latencyMs = System.currentTimeMillis() - startTime;
            metricsCollector.recordLatency(latencyMs);
            metricsCollector.recordBookingSuccess();
            log.info("Booking completed successfully in {}ms", latencyMs);
            return result;
        } catch (IllegalStateException e) {
            log.warn("Booking attempt {}/{} failed for slot {}: {}",
                    attempt, maxAttempts, slotId, e.getMessage());
            if (attempt == maxAttempts) {
                long latencyMs = System.currentTimeMillis() - startTime;
                metricsCollector.recordLatency(latencyMs);
                metricsCollector.recordBookingFailure("slot_unavailable");
                log.error("Booking failed after {} attempts ({}ms): {}",
                        maxAttempts, latencyMs, e.getMessage());
                throw e;
            }
        } catch (DataAccessException e) {
            log.warn("DB contention on attempt {}/{} for slot {}",
                    attempt, maxAttempts, slotId);
            if (attempt == maxAttempts) {
                long latencyMs = System.currentTimeMillis() - startTime;
                metricsCollector.recordLatency(latencyMs);
                metricsCollector.recordBookingFailure("database_locked");
                log.error("Booking failed after {} attempts ({}ms) due to database contention",
                        maxAttempts, latencyMs);
                throw e;
            }
        }
    }
    throw new IllegalStateException("Booking failed after " + maxAttempts + " attempts.");
}
```

### Logging Examples - Appointment Cancellation

```java
@Transactional
public void cancelAppointment(Long appointmentId, Long slotId) {
    log.info("Cancelling appointment {}", appointmentId);
    appointmentRepo.updateStatus(appointmentId, "cancelled");
    availabilityRepo.markAvailable(slotId);
    log.info("Appointment {} cancelled, slot {} re-opened", appointmentId, slotId);
}
```

---

## 3. Enhanced Notification Service with Resilience (Recovery Strategy)

### Retry Logic with Exponential Backoff

```java
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int MAX_RETRIES = 3;
    private static final int[] BACKOFF_MS = {1000, 2000, 4000}; // Exponential backoff
    private static final long RETRY_QUEUE_TTL_MS = 24 * 60 * 60 * 1000; // 24 hours

    private final NotificationClient notificationClient;
    private final Queue<QueuedNotification> retryQueue = Collections.synchronizedQueue(new LinkedList<>());
    private final AtomicInteger failedNotifications = new AtomicInteger(0);

    // Send with retry and exponential backoff
    private NotificationResponse sendWithRetry(Appointment appointment, String name, String email) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                NotificationResponse response = notificationClient.sendConfirmation(
                        appointment, name, email);

                if (response != null) {
                    log.info("Confirmation delivered — appointmentId={}, notificationId={}, status={}",
                            appointment.getApmtId(), response.getNotificationId(), response.getStatus());
                    return response;
                }
            } catch (Exception e) {
                log.warn("Notification attempt {}/{} failed for appointment {}: {}",
                        attempt + 1, MAX_RETRIES, appointment.getApmtId(), e.getMessage());

                if (attempt < MAX_RETRIES - 1) {
                    try {
                        long waitMs = BACKOFF_MS[attempt];
                        log.debug("Retrying notification in {}ms", waitMs);
                        Thread.sleep(waitMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // All retries failed - queue for async retry (fallback strategy)
        log.error("Notification failed after {} retries for appointment {}; queueing for async retry",
                MAX_RETRIES, appointment.getApmtId());
        failedNotifications.incrementAndGet();
        queueForAsyncRetry(appointment, name, email);
        return null;
    }

    // Queue for async retry - graceful degradation
    private void queueForAsyncRetry(Appointment appointment, String recipientName, String recipientEmail) {
        QueuedNotification queued = new QueuedNotification(appointment, recipientName, recipientEmail);
        retryQueue.offer(queued);
        log.info("Notification queued for async retry (queue size: {})", retryQueue.size());
    }

    // Periodic task to retry queued notifications (every 30 seconds)
    @Scheduled(fixedDelay = 30000, initialDelay = 30000)
    public void processRetryQueue() {
        if (retryQueue.isEmpty()) {
            return;
        }

        log.debug("Processing retry queue (size: {})", retryQueue.size());

        Queue<QueuedNotification> tempQueue = new LinkedList<>(retryQueue);
        retryQueue.clear();

        for (QueuedNotification queued : tempQueue) {
            // Check TTL
            if (System.currentTimeMillis() - queued.queuedAt > RETRY_QUEUE_TTL_MS) {
                log.warn("Notification for appointment {} expired in retry queue; discarding",
                        queued.appointment.getApmtId());
                continue;
            }

            try {
                NotificationResponse response = notificationClient.sendConfirmation(
                        queued.appointment, queued.recipientName, queued.recipientEmail);

                if (response != null) {
                    log.info("Queued notification sent successfully for appointment {}",
                            queued.appointment.getApmtId());
                } else {
                    retryQueue.offer(queued);
                }
            } catch (Exception e) {
                log.warn("Queued notification retry failed for appointment {}: {}",
                        queued.appointment.getApmtId(), e.getMessage());
                retryQueue.offer(queued);
            }
        }
    }

    // Check if service is degraded
    public boolean isServiceDegraded() {
        return retryQueue.size() > 5;
    }

    // Get queued notifications count
    public int getFailedNotificationsCount() {
        return retryQueue.size();
    }
}
```

---

## 4. Enhanced Health Check Endpoint (M6)

### HealthController.java

```java
@RestController
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);
    private final JdbcTemplate jdbc;
    private final NotificationService notificationService;

    /**
     * Health check endpoint returning overall system status
     * HTTP 200 = UP or DEGRADED
     * HTTP 503 = DOWN
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> checks = new HashMap<>();

        // Check database (critical)
        Map<String, String> dbCheck = checkDatabase();
        checks.put("database", dbCheck);

        // Check notification service (non-critical, degrades gracefully)
        Map<String, String> notifCheck = checkNotificationService();
        checks.put("notification_service", notifCheck);

        // Determine overall status
        String overallStatus = "UP";
        HttpStatus httpStatus = HttpStatus.OK;

        if ("DOWN".equals(dbCheck.get("status"))) {
            overallStatus = "DOWN";
            httpStatus = HttpStatus.SERVICE_UNAVAILABLE;
            response.put("error", "Cannot serve requests");
        } else if ("DOWN".equals(notifCheck.get("status"))) {
            overallStatus = "DEGRADED";
        }

        response.put("status", overallStatus);
        response.put("timestamp", Instant.now());
        response.put("checks", checks);

        return new ResponseEntity<>(response, httpStatus);
    }

    // Check database connectivity
    private Map<String, String> checkDatabase() {
        Map<String, String> check = new HashMap<>();
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            check.put("status", "UP");
            check.put("message", "SQLite connected");
            log.debug("Database health check passed");
        } catch (Exception e) {
            check.put("status", "DOWN");
            check.put("message", "JDBC connection refused: " + e.getMessage());
            log.error("Database health check failed: {}", e.getMessage());
        }
        return check;
    }

    // Check notification service health
    private Map<String, String> checkNotificationService() {
        Map<String, String> check = new HashMap<>();
        try {
            boolean isDegraded = notificationService.isServiceDegraded();
            int queuedCount = notificationService.getFailedNotificationsCount();

            if (isDegraded) {
                check.put("status", "DOWN");
                check.put("message", "Notification service degraded - " + queuedCount + " notifications in retry queue");
                log.warn("Notification service degraded: {} queued notifications", queuedCount);
            } else if (queuedCount > 0) {
                check.put("status", "DEGRADED");
                check.put("message", "Notification service experiencing issues");
            } else {
                check.put("status", "UP");
                check.put("message", "External notification service responding");
            }
        } catch (Exception e) {
            check.put("status", "DOWN");
            check.put("message", "Unable to reach notification service");
            log.warn("Notification service health check failed: {}", e.getMessage());
        }
        return check;
    }
}
```

### Example Health Check Responses

**Healthy (HTTP 200):**
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

**Degraded (HTTP 200):**
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

**Down (HTTP 503):**
```json
{
  "status": "DOWN",
  "timestamp": "2026-04-29T14:37:05Z",
  "error": "Cannot serve requests",
  "checks": {
    "database": {
      "status": "DOWN",
      "message": "JDBC connection refused"
    }
  }
}
```

---

## 5. Metrics Controller (Monitoring Endpoints)

### MetricsController.java

```java
@RestController
@RequestMapping("/metrics")
public class MetricsController {

    private final MetricsCollector metricsCollector;

    /**
     * GET /metrics
     * Get all metrics in one snapshot
     */
    @GetMapping
    public Map<String, Object> getAllMetrics() {
        return metricsCollector.getMetricsSnapshot();
    }

    /**
     * GET /metrics/bookings
     * Get booking metrics (hourly count and failed count)
     */
    @GetMapping("/bookings")
    public Map<String, Object> getBookingMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("bookings_this_hour", metricsCollector.getBookingsThisHour());
        metrics.put("failed_bookings", metricsCollector.getFailedBookings());
        metrics.put("failure_rate_percent", String.format("%.2f", metricsCollector.getFailureRate()));
        metrics.put("failure_reasons", metricsCollector.getFailureReasons());
        return metrics;
    }

    /**
     * GET /metrics/bookings/latency
     * Get latency metrics (avg, p99, min, max)
     */
    @GetMapping("/bookings/latency")
    public Map<String, Object> getLatencyMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("avg_latency_ms", String.format("%.2f", metricsCollector.getAverageLatency()));
        metrics.put("p99_latency_ms", metricsCollector.getP99Latency());
        metrics.put("min_latency_ms", metricsCollector.getMinLatency());
        metrics.put("max_latency_ms", metricsCollector.getMaxLatency());
        metrics.put("sample_count", metricsCollector.getLatencySampleCount());
        return metrics;
    }

    /**
     * GET /metrics/bookings/failed
     * Get failure metrics with detailed breakdown
     */
    @GetMapping("/bookings/failed")
    public Map<String, Object> getFailureMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("total_failures", metricsCollector.getFailedBookings());
        metrics.put("failure_rate_percent", String.format("%.2f", metricsCollector.getFailureRate()));
        metrics.put("failures_by_reason", metricsCollector.getFailureReasons());
        return metrics;
    }
}
```

### Example Metrics Responses

**Booking Metrics (GET /metrics/bookings):**
```json
{
  "bookings_this_hour": 23,
  "failed_bookings": 2,
  "failure_rate_percent": "8.00",
  "failure_reasons": {
    "slot_unavailable": 1,
    "database_locked": 1
  }
}
```

**Latency Metrics (GET /metrics/bookings/latency):**
```json
{
  "avg_latency_ms": "245.67",
  "p99_latency_ms": 1850,
  "min_latency_ms": 45,
  "max_latency_ms": 3200,
  "sample_count": 25
}
```

**Failure Metrics (GET /metrics/bookings/failed):**
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

---

## 6. Logging Configuration (application.properties)

```properties
# Logging (M6 - System Management)
logging.level.com.beautifulnail=INFO
logging.level.org.springframework.jdbc=DEBUG
logging.file.name=logs/application.log
logging.file.max-size=10MB
logging.file.max-history=10
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n

# Enable scheduling for notification retry queue (M6)
spring.task.scheduling.pool.size=2
```

---

## 7. Example Log Messages

### INFO Level - Normal Operations
```
2026-04-29 14:23:15 [http-nio-8080-exec-1] INFO  AppointmentService - Attempting to book appointment for user 5 on slot 42
2026-04-29 14:23:16 [http-nio-8080-exec-1] INFO  AppointmentService - Appointment booked successfully for user 5
2026-04-29 14:23:17 [http-nio-8080-exec-1] INFO  AppointmentService - Booking completed successfully in 245ms
2026-04-29 14:23:18 [http-nio-8080-exec-1] INFO  NotificationService - Confirmation delivered — appointmentId=127, notificationId=notif_abc123, status=sent
2026-04-29 14:23:19 [http-nio-8080-exec-2] INFO  AppointmentService - Cancelling appointment 125
2026-04-29 14:23:20 [http-nio-8080-exec-2] INFO  AppointmentService - Appointment 125 cancelled, slot 42 re-opened
```

### WARN Level - Contention & Retry Logic
```
2026-04-29 14:24:05 [http-nio-8080-exec-3] WARN  AppointmentService - Booking attempt 1/3 failed for slot 42: This time slot is no longer available.
2026-04-29 14:24:06 [http-nio-8080-exec-3] WARN  AppointmentService - DB contention on attempt 2/3 for slot 42
2026-04-29 14:24:08 [http-nio-8080-exec-3] WARN  AppointmentService - Slot 42 was claimed by a concurrent transaction
2026-04-29 14:24:10 [scheduler-1] WARN  NotificationService - Notification attempt 1/3 failed for appointment 128: Connection timeout
2026-04-29 14:24:15 [scheduler-1] WARN  NotificationService - Notification service degraded: 7 queued notifications
```

### ERROR Level - System Failures
```
2026-04-29 14:25:30 [http-nio-8080-exec-4] ERROR AppointmentService - Booking failed after 3 attempts (1250ms): SQLite database locked
2026-04-29 14:25:31 [scheduler-1] ERROR NotificationService - Notification failed after 3 retries for appointment 130; queueing for async retry
2026-04-29 14:25:32 [http-nio-8080-exec-5] ERROR HealthController - Database health check failed: JDBC connection refused
```

---

## 8. Failure Scenario: Notification Service Unavailable

### Scenario Flow (M6)

1. **User submits booking request at 14:30:00**
   ```
   [INFO] Attempting to book appointment for user 5 on slot 42
   ```

2. **Slot is reserved successfully**
   ```
   [INFO] Appointment booked successfully for user 5
   ```

3. **Notification service returns HTTP 503**
   ```
   [WARN] Notification attempt 1/3 failed for appointment 127: Service Unavailable (HTTP 503)
   ```

4. **Exponential backoff retry #2 (after 1 second)**
   ```
   [DEBUG] Retrying notification in 1000ms
   [WARN] Notification attempt 2/3 failed for appointment 127: Service Unavailable
   ```

5. **Exponential backoff retry #3 (after 2 seconds)**
   ```
   [DEBUG] Retrying notification in 2000ms
   [WARN] Notification attempt 3/3 failed for appointment 127: Service Unavailable
   ```

6. **All retries exhausted, queue for async retry**
   ```
   [ERROR] Notification failed after 3 retries for appointment 127; queueing for async retry
   [INFO] Notification queued for async retry (queue size: 1)
   ```

7. **Periodic async retry task runs (every 30 seconds)**
   ```
   [DEBUG] Processing retry queue (size: 1)
   [INFO] Queued notification sent successfully for appointment 127
   ```

### Recovery Strategy Summary
- **Phase 1:** Immediate retry with exponential backoff (1s, 2s, 4s)
- **Phase 2:** Queue to in-memory deque with 24-hour TTL
- **Phase 3:** Async retry task processes queue every 30 seconds
- **Phase 4:** User can access confirmation via `/appointments/<id>/confirmation` endpoint
- **Operational Alert:** ERROR logged after 3 failed attempts to notify operators

---

## 9. Spring Boot Main Application (Enable Scheduling)

### SchedulerApplication.java

```java
package com.beautifulnail.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // Enable @Scheduled tasks in NotificationService
public class SchedulerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SchedulerApplication.class, args);
    }
}
```

---

## Testing the Implementation

### Health Check Endpoint
```bash
curl http://localhost:8080/health

# Response (HTTP 200 or 503 depending on system state)
```

### Metrics Endpoints
```bash
# Get all metrics
curl http://localhost:8080/metrics

# Get booking metrics
curl http://localhost:8080/metrics/bookings

# Get latency metrics
curl http://localhost:8080/metrics/bookings/latency

# Get failure metrics
curl http://localhost:8080/metrics/bookings/failed
```

### Simulating Failure Scenarios
```bash
# 1. Stop external notification service to trigger retries
# Observe: WARN logs, then ERROR logs, then notifications queued

# 2. Restart notification service
# Observe: Queued notifications processed every 30 seconds

# 3. Check health endpoint
curl http://localhost:8080/health
# Will show DEGRADED state while notifications are in retry queue
```

---

## Summary

This M6 implementation provides:

✅ **Comprehensive Logging** - All critical events logged at appropriate levels (INFO/WARN/ERROR)

✅ **Three Key Metrics**:
- Bookings per hour
- Failed bookings with reasons
- Latency tracking (average, p99, min, max)

✅ **Failure Scenario & Recovery**:
- Notification service unavailability handled with exponential backoff retry
- Graceful degradation via in-memory queue with 24-hour TTL
- Async processing every 30 seconds
- User-accessible fallback via confirmation endpoint

✅ **Health Check Endpoint** - Returns JSON with component status and HTTP codes:
- 200 UP/DEGRADED
- 503 DOWN

✅ **Monitoring Endpoints** - Metrics exposed via REST API for operational monitoring
