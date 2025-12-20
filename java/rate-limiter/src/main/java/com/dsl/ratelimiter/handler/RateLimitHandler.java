package com.dsl.ratelimiter.handler;

import com.dsl.ratelimiter.strategy.RateLimiterStrategy;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

@ChannelHandler.Sharable
public class RateLimitHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final RateLimiterStrategy strategy;
    private final long capacity;
    private final long rate;

    public RateLimitHandler(RateLimiterStrategy strategy) {
        this.strategy = strategy;
        this.capacity = 10;
        this.rate = 1;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) {
        String userId = "anonymous";
        if (req.headers().contains("X-User-ID")) {
            userId = req.headers().get("X-User-ID");
        }

        String key = "rate_limit:" + userId;

        boolean allowed = strategy.isAllowed(key, capacity, rate);

        if (allowed) {
            sendResponse(ctx, HttpResponseStatus.OK, "Request Allowed for " + userId);
        } else {
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