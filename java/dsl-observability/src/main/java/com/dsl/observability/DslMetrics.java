package com.dsl.observability;

import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

public class DslMetrics {

    private static volatile PrometheusMeterRegistry registry;

    private DslMetrics() {
        // Prevent instantiation
    }

    public static PrometheusMeterRegistry getInstance() {
        if (registry == null) {
            synchronized (DslMetrics.class) {
                if (registry == null) {
                    registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
                }
            }
        }
        return registry;
    }
}