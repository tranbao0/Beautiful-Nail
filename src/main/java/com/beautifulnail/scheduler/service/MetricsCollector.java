package com.beautifulnail.scheduler.service;

import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class MetricsCollector {

    private final AtomicInteger bookingsThisHour = new AtomicInteger(0);
    private final AtomicInteger failedBookings = new AtomicInteger(0);
    private final List<Long> latencySamples = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Integer> failureReasons = Collections.synchronizedMap(new HashMap<>());
    private volatile Instant lastHourReset = Instant.now();

    /**
     * Record a successful booking
     */
    public void recordBookingSuccess() {
        bookingsThisHour.incrementAndGet();
    }

    /**
     * Record a failed booking with reason
     */
    public void recordBookingFailure(String reason) {
        failedBookings.incrementAndGet();
        failureReasons.merge(reason, 1, Integer::sum);
    }

    /**
     * Record latency of a booking operation (in milliseconds)
     */
    public void recordLatency(long latencyMs) {
        latencySamples.add(latencyMs);
        // Keep only last 1000 samples to avoid memory bloat
        if (latencySamples.size() > 1000) {
            latencySamples.remove(0);
        }
    }

    /**
     * Get bookings in current hour window
     */
    public int getBookingsThisHour() {
        checkAndResetHour();
        return bookingsThisHour.get();
    }

    /**
     * Get total failed bookings
     */
    public int getFailedBookings() {
        return failedBookings.get();
    }

    /**
     * Get breakdown of failure reasons
     */
    public Map<String, Integer> getFailureReasons() {
        return new HashMap<>(failureReasons);
    }

    /**
     * Get average latency in milliseconds
     */
    public double getAverageLatency() {
        if (latencySamples.isEmpty()) return 0;
        return latencySamples.stream().mapToLong(Long::longValue).average().orElse(0);
    }

    /**
     * Get p99 latency (99th percentile) in milliseconds
     */
    public long getP99Latency() {
        if (latencySamples.isEmpty()) return 0;
        List<Long> sorted = new ArrayList<>(latencySamples);
        Collections.sort(sorted);
        int index = (int) Math.ceil(sorted.size() * 0.99) - 1;
        return index >= 0 ? sorted.get(index) : 0;
    }

    /**
     * Get maximum latency recorded
     */
    public long getMaxLatency() {
        if (latencySamples.isEmpty()) return 0;
        return latencySamples.stream().mapToLong(Long::longValue).max().orElse(0);
    }

    /**
     * Get minimum latency recorded
     */
    public long getMinLatency() {
        if (latencySamples.isEmpty()) return 0;
        return latencySamples.stream().mapToLong(Long::longValue).min().orElse(0);
    }

    /**
     * Get total latency samples count
     */
    public int getLatencySampleCount() {
        return latencySamples.size();
    }

    /**
     * Reset hour counter if 1 hour has passed
     */
    private void checkAndResetHour() {
        Instant now = Instant.now();
        if (Duration.between(lastHourReset, now).toMinutes() >= 60) {
            bookingsThisHour.set(0);
            lastHourReset = now;
        }
    }

    /**
     * Get failure rate as percentage
     */
    public double getFailureRate() {
        int total = bookingsThisHour.get() + failedBookings.get();
        if (total == 0) return 0;
        return (failedBookings.get() / (double) total) * 100;
    }

    /**
     * Reset all metrics (for testing or admin purposes)
     */
    public void reset() {
        bookingsThisHour.set(0);
        failedBookings.set(0);
        latencySamples.clear();
        failureReasons.clear();
        lastHourReset = Instant.now();
    }

    /**
     * Get comprehensive metrics snapshot
     */
    public Map<String, Object> getMetricsSnapshot() {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("timestamp", Instant.now());
        snapshot.put("bookings_this_hour", getBookingsThisHour());
        snapshot.put("failed_bookings", getFailedBookings());
        snapshot.put("failure_rate_percent", String.format("%.2f", getFailureRate()));
        snapshot.put("avg_latency_ms", String.format("%.2f", getAverageLatency()));
        snapshot.put("p99_latency_ms", getP99Latency());
        snapshot.put("max_latency_ms", getMaxLatency());
        snapshot.put("min_latency_ms", getMinLatency());
        snapshot.put("latency_samples", getLatencySampleCount());
        snapshot.put("failure_reasons", getFailureReasons());
        return snapshot;
    }
}
