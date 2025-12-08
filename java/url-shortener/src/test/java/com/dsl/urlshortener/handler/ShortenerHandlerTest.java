package com.dsl.urlshortener.handler;

import com.dsl.common.idgenerator.SnowflakeIdGenerator;
import com.dsl.urlshortener.repository.UrlRepository;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortenerHandlerTest {

    @Mock
    private SnowflakeIdGenerator idGenerator;

    @Mock
    private UrlRepository repository;

    @Mock
    private PrometheusMeterRegistry meterRegistry;

    @Mock
    private Timer shortenTimer;

    @Mock
    private Timer redirectTimer;

    private EmbeddedChannel channel;

    @BeforeEach
    void setUp() {
        when(meterRegistry.timer("http_requests", "endpoint", "shorten")).thenReturn(shortenTimer);
        when(meterRegistry.timer("http_requests", "endpoint", "redirect")).thenReturn(redirectTimer);
        ShortenerHandler handler = new ShortenerHandler(idGenerator, repository, "http://localhost:8080/", meterRegistry);
        // EmbeddedChannel to test Netty handlers
        channel = new EmbeddedChannel(handler);
    }

    @Test
    void testShortenUrlSuccess() {
        long mockId = 12345L;
        String expectedShortCode = "3d7"; // Base62 of 12345
        when(idGenerator.nextId()).thenReturn(mockId);

        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.POST,
                "/shorten",
                Unpooled.copiedBuffer("https://google.com", CharsetUtil.UTF_8)
        );
        channel.writeInbound(request);

        FullHttpResponse response = channel.readOutbound();

        assertNotNull(response);
        assertEquals(HttpResponseStatus.OK, response.status());

        String body = response.content().toString(CharsetUtil.UTF_8);
        assertEquals("http://localhost:8080/" + expectedShortCode, body);

        verify(repository).save(mockId, expectedShortCode, "https://google.com");
        verify(shortenTimer).record(any(java.time.Duration.class));
    }

    @Test
    void testShortenEmptyBodyFails() {
        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.POST,
                "/shorten",
                Unpooled.copiedBuffer("", CharsetUtil.UTF_8)
        );

        channel.writeInbound(request);
        FullHttpResponse response = channel.readOutbound();

        assertEquals(HttpResponseStatus.BAD_REQUEST, response.status());
        verifyNoInteractions(idGenerator); // Shouldn't burn an ID for invalid request
    }

    @Test
    void testRedirectNotFound() {
        // 1. SETUP: Repo returns null
        when(repository.getOriginalUrl("unknown")).thenReturn(null);

        // 2. EXECUTE
        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.GET,
                "/unknown"
        );
        channel.writeInbound(request);

        // 3. VERIFY
        FullHttpResponse response = channel.readOutbound();
        assertEquals(HttpResponseStatus.NOT_FOUND, response.status());
        verify(redirectTimer).record(any(java.time.Duration.class));
    }

    @Test
    void testMetricsEndpoint() {
        when(meterRegistry.scrape()).thenReturn("http_requests");
        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.GET,
                "/metrics"
        );
        channel.writeInbound(request);

        FullHttpResponse response = channel.readOutbound();
        assertEquals(HttpResponseStatus.OK, response.status());
        assertTrue(response.content().toString(CharsetUtil.UTF_8).contains("http_requests"));
        verifyNoInteractions(shortenTimer);
        verifyNoInteractions(redirectTimer);
    }
}
