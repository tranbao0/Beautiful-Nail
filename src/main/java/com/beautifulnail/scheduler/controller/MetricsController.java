package com.beautifulnail.scheduler.controller;

import com.beautifulnail.scheduler.service.MetricsCollector;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Metrics exposition endpoint for monitoring system behavior.
 * Provides bookings, failures, and latency metrics. (M6)
 */
@RestController
@RequestMapping("/metrics")
public class MetricsController {

    private final MetricsCollector metricsCollector;

    public MetricsController(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    /**
     * Get all metrics in one snapshot
     * GET /metrics
     */
    @GetMapping
    public Map<String, Object> getAllMetrics() {
        return metricsCollector.getMetricsSnapshot();
    }

    /**
     * Get booking metrics (hourly count and failed count)
     * GET /metrics/bookings
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
     * Get latency metrics (avg, p99, min, max)
     * GET /metrics/bookings/latency
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
     * Get failure metrics with detailed breakdown
     * GET /metrics/bookings/failed
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
