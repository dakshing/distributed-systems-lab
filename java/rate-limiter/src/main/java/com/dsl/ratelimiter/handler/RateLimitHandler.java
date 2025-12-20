package com.dsl.ratelimiter.handler;

import com.dsl.ratelimiter.strategy.RateLimiterStrategy;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.time.Duration;

@ChannelHandler.Sharable
public class RateLimitHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final RateLimiterStrategy strategy;
    private final long capacity;
    private final long rate;
    private final PrometheusMeterRegistry meterRegistry;
    private final Counter allowedCounter;
    private final Counter blockedCounter;
    private final Timer checkTimer;

    public RateLimitHandler(RateLimiterStrategy strategy, PrometheusMeterRegistry registry) {
        this.strategy = strategy;
        this.meterRegistry = registry;
        this.allowedCounter = registry.counter("ratelimiter.requests", "result", "allowed");
        this.blockedCounter = registry.counter("ratelimiter.requests", "result", "blocked");
        this.checkTimer = registry.timer("ratelimiter.check.duration");
        this.capacity = 10;
        this.rate = 1;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) {
        String uri = req.uri();

        if ("/metrics".equals(uri)) {
            sendResponse(ctx, HttpResponseStatus.OK, meterRegistry.scrape());
            return;
        }

        String userId = "anonymous";
        if (req.headers().contains("X-User-ID")) {
            userId = req.headers().get("X-User-ID");
        }

        String key = "rate_limit:" + userId;

        long start = System.nanoTime();
        boolean allowed = strategy.isAllowed(key, capacity, rate);
        checkTimer.record(Duration.ofNanos(System.nanoTime() - start));

        if (allowed) {
            allowedCounter.increment();
            sendResponse(ctx, HttpResponseStatus.OK, "Request Allowed for " + userId);
        } else {
            blockedCounter.increment();
            sendResponse(ctx, HttpResponseStatus.TOO_MANY_REQUESTS, "Rate Limit Exceeded");
        }
    }

    private void sendResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String content) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        ctx.writeAndFlush(response);
    }
}