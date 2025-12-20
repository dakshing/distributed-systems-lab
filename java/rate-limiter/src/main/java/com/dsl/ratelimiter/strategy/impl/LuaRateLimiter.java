package com.dsl.ratelimiter.strategy.impl;

import com.dsl.ratelimiter.strategy.RateLimiterStrategy;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class LuaRateLimiter implements RateLimiterStrategy {
    private static final Logger logger = LoggerFactory.getLogger(LuaRateLimiter.class);

    private final RedisCommands<String, String> redis;
    private final String scriptSha;
    private final boolean needsMillis;

    public LuaRateLimiter(RedisCommands<String, String> redis, String scriptSha, boolean needsMillis) {
        this.redis = redis;
        this.scriptSha = scriptSha;
        this.needsMillis = needsMillis;
    }

    @Override
    public boolean isAllowed(String key, long limit, long period) {
        long now = needsMillis ? Instant.now().toEpochMilli() : Instant.now().getEpochSecond();

        try {
            // Execute Lua Script
            Object result = redis.evalsha(scriptSha, ScriptOutputType.INTEGER,
                    new String[]{key},
                    String.valueOf(limit),
                    String.valueOf(period),
                    String.valueOf(now)
            );

            if (result instanceof Long) {
                return ((Long) result) == 1L;
            }
            return false;

        } catch (Exception e) {
            // Circuit Breaker - Fail Open
            // If Redis is down/slow, we allow traffic to preserve user experience.
            logger.error("Redis rate limiter failed for key: {}. Defaulting to Allow.", key, e);
            return true;
        }
    }
}