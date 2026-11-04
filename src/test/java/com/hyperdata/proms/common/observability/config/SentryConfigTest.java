package com.hyperdata.proms.common.observability.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.protocol.Request;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SentryConfigTest {

    @Test
    @DisplayName("Should disable Sentry when DSN is empty or null")
    void shouldDisableSentryWhenDsnEmpty() {
        SentryConfig config = new SentryConfig("", "test", "api", "api@0.0.1", 1.0);
        config.initSentry();

        assertThat(Sentry.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Should strip sensitive headers in BeforeSendCallback")
    void shouldStripSensitiveHeadersInBeforeSendCallback() {
        SentryConfig config = new SentryConfig("", "test", "api", "api@0.0.1", 1.0);
        var callback = config.sentryBeforeSendCallback();

        SentryEvent event = new SentryEvent();
        Request request = new Request();
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer sensitive-token");
        headers.put("Cookie", "session=12345");
        headers.put("Set-Cookie", "key=val");
        headers.put("X-Api-Key", "secret-key");
        headers.put("Content-Type", "application/json");
        request.setHeaders(headers);
        event.setRequest(request);

        SentryEvent processed = callback.execute(event, null);

        assertThat(processed.getRequest().getHeaders())
            .doesNotContainKeys("Authorization", "Cookie", "Set-Cookie", "X-Api-Key")
            .containsEntry("Content-Type", "application/json");
    }

    @Test
    @DisplayName("Should sync eventId with traceId in BeforeSendCallback when present")
    void shouldSyncEventIdWithTraceId() {
        SentryConfig config = new SentryConfig("", "test", "api", "api@0.0.1", 1.0);
        var callback = config.sentryBeforeSendCallback();

        SentryEvent event = new SentryEvent();
        String traceId = "5788f5124af1188759579de096cede86";
        event.setTag("trace_id", traceId);

        SentryEvent processed = callback.execute(event, null);

        assertThat(processed.getEventId().toString().replace("-", "")).isEqualTo(traceId);
    }
}
