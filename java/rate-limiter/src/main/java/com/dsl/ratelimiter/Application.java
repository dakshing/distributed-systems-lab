package com.dsl.ratelimiter;

import com.dsl.ratelimiter.factory.RateLimiterFactory;
import com.dsl.ratelimiter.handler.RateLimitHandler;
import com.dsl.ratelimiter.server.NettyServer;
import com.dsl.ratelimiter.strategy.RateLimiterStrategy;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;

public class Application {

    public static void main(String[] args) throws Exception {
        int port = 8080;
        String redisHost = System.getenv().getOrDefault("REDIS_HOST", "localhost");
        String redisUrl = "redis://" + redisHost + ":6379";

        System.out.println("Starting Rate Limiter on port " + port);
        System.out.println("Connecting to Redis at " + redisHost + "...");

        try (RedisClient redisClient = RedisClient.create(redisUrl)) {
            StatefulRedisConnection<String, String> connection = redisClient.connect();

            RateLimiterFactory factory = new RateLimiterFactory(connection.sync());
            RateLimiterStrategy rateLimiter = factory.get(RateLimiterFactory.Type.TOKEN_BUCKET);

            System.out.println("Token Bucket Strategy Loaded.");

            NettyServer<RateLimitHandler> server = new NettyServer<>(port, new RateLimitHandler(rateLimiter));
            server.start();
        }
    }
}