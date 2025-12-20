package com.dsl.ratelimiter.strategy;

public interface RateLimiterStrategy {
    /**
     * @param key Unique identifier (e.g., "ip:127.0.0.1")
     * @param limit Max requests
     * @param periodSeconds Window size
     * @return true if allowed, false if blocked
     */
    boolean isAllowed(String key, long limit, long periodSeconds);
}