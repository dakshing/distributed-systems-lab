package com.dsl.urlshortener.handler;

import com.dsl.common.baseencoder.Base62Encoder;
import com.dsl.common.idgenerator.SnowflakeIdGenerator;
import com.dsl.urlshortener.repository.UrlRepository;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.time.Duration;

@ChannelHandler.Sharable
public class ShortenerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final SnowflakeIdGenerator idGenerator;
    private final UrlRepository repository;
    private final String domain;
    private final PrometheusMeterRegistry meterRegistry;
    private final Timer shortenTimer;
    private final Timer redirectTimer;

    public ShortenerHandler(SnowflakeIdGenerator idGenerator, UrlRepository repository, String domain, PrometheusMeterRegistry meterRegistry) {
        this.idGenerator = idGenerator;
        this.repository = repository;
        this.domain = domain;
        this.meterRegistry = meterRegistry;
        this.shortenTimer = meterRegistry.timer("http_requests", "endpoint", "shorten");
        this.redirectTimer = meterRegistry.timer("http_requests", "endpoint", "redirect");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext handlerContext, FullHttpRequest request) {
        HttpMethod method = request.method();
        String uri = request.uri();

        if ("/metrics".equals(uri)) {
            sendResponse(handlerContext, HttpResponseStatus.OK, meterRegistry.scrape());
            return;
        }

        long startTime = System.nanoTime();

        if (HttpMethod.POST.equals(method) && "/shorten".equals(uri)) {
            handleShortenRequest(handlerContext, request);
            shortenTimer.record(Duration.ofNanos(System.nanoTime() - startTime));
        } else if (HttpMethod.GET.equals(method) && uri.length() > 1) {
            handleRedirectRequest(handlerContext, uri.substring(1)); // Remove leading '/'
            redirectTimer.record(Duration.ofNanos(System.nanoTime() - startTime));
        } else {
            sendResponse(handlerContext, HttpResponseStatus.NOT_FOUND, "Endpoint not found");
        }
    }

    private void handleShortenRequest(ChannelHandlerContext handlerContext, FullHttpRequest request) {
        String originalUrl = request.content().toString(CharsetUtil.UTF_8).trim();

        if (originalUrl.isEmpty()) {
            sendResponse(handlerContext, HttpResponseStatus.BAD_REQUEST, "Body cannot be empty");
            return;
        }

        // Core Logic: ID -> Base62 -> Store
        long id = idGenerator.nextId();
        String shortUrl = Base62Encoder.encode(id);
        repository.save(id, shortUrl, originalUrl);

        String responseBody = domain + shortUrl;
        sendResponse(handlerContext, HttpResponseStatus.OK, responseBody);
    }

    private void handleRedirectRequest(ChannelHandlerContext handlerContext, String shortUrl) {
        String originalUrl = repository.getOriginalUrl(shortUrl);

        if (originalUrl == null) {
            sendResponse(handlerContext, HttpResponseStatus.NOT_FOUND, "Short URL not found");
            return;
        }

        // 302 Redirect
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
        response.headers().set(HttpHeaderNames.LOCATION, originalUrl);

        handlerContext.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private void sendResponse(ChannelHandlerContext handlerContext, HttpResponseStatus status, String content) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        handlerContext.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}