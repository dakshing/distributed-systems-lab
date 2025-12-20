package com.dsl.ratelimiter.factory;

import com.dsl.ratelimiter.strategy.impl.LuaRateLimiter;
import com.dsl.ratelimiter.strategy.RateLimiterStrategy;
import io.lettuce.core.api.sync.RedisCommands;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class RateLimiterFactory {

    public enum Type { TOKEN_BUCKET, LEAKING_BUCKET, FIXED_WINDOW, SLIDING_LOG, SLIDING_COUNTER }

    private final RedisCommands<String, String> redis;
    private final Map<Type, String> scriptShas = new HashMap<>();

    public RateLimiterFactory(RedisCommands<String, String> redis) {
        this.redis = redis;
        load(Type.TOKEN_BUCKET, "token_bucket.lua");
        load(Type.LEAKING_BUCKET, "leaking_bucket.lua");
        load(Type.FIXED_WINDOW, "fixed_window.lua");
        load(Type.SLIDING_LOG, "sliding_log.lua");
        load(Type.SLIDING_COUNTER, "sliding_counter.lua");
    }

    private void load(Type type, String file) {
        try (InputStream is = getClass().getResourceAsStream("/lua/" + file)) {
            if (is == null) throw new RuntimeException("Script not found: " + file);
            String script = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            scriptShas.put(type, redis.scriptLoad(script));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load script: " + file, e);
        }
    }

    public RateLimiterStrategy get(Type type) {
        String sha = scriptShas.get(type);
        boolean needsMillis = (type == Type.SLIDING_LOG);
        return new LuaRateLimiter(redis, sha, needsMillis);
    }
}