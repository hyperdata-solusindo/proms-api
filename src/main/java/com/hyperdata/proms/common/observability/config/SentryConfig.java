package com.hyperdata.proms.common.observability.config;

import io.sentry.Sentry;
import io.sentry.SentryOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SentryConfig {

    private final String dsn;
    private final String environment;
    private final String serverName;
    private final String release;
    private final Double tracesSampleRate;

    public SentryConfig(
        @Value("${sentry.dsn:}")
        String dsn,
        @Value("${sentry.environment:local}")
        String environment,
        @Value("${sentry.server-name:api}")
        String serverName,
        @Value("${sentry.release:api@0.0.1}")
        String release,
        @Value("${sentry.traces-sample-rate:1.0}")
        Double tracesSampleRate) {
        this.dsn = dsn;
        this.environment = environment;
        this.serverName = serverName;
        this.release = release;
        this.tracesSampleRate = tracesSampleRate;
    }

    @PostConstruct
    public void initSentry() {
        if (dsn != null && !dsn.isBlank()) {
            Sentry.init(options -> {
                options.setDsn(dsn);
                options.setEnvironment(environment);
                options.setServerName(serverName);
                options.setRelease(release);
                options.setTracesSampleRate(tracesSampleRate);
                options.setSendDefaultPii(false);
                options.setBeforeSend(sentryBeforeSendCallback());
            });
        } else {
            Sentry.init(options -> options.setEnabled(false));
        }
    }

    @Bean
    public SentryOptions.BeforeSendCallback sentryBeforeSendCallback() {
        return (event, hint) -> {
            if (event.getRequest() != null && event.getRequest().getHeaders() != null) {
                event.getRequest().getHeaders().remove("Authorization");
                event.getRequest().getHeaders().remove("Cookie");
                event.getRequest().getHeaders().remove("Set-Cookie");
                event.getRequest().getHeaders().remove("X-Api-Key");
            }

            // Sync Event ID with Trace ID so searching either traceId or eventId in GlitchTip matches directly
            String traceId = event.getTag("trace_id");
            if (traceId == null || traceId.isBlank()) {
                traceId = event.getTag("traceId");
            }
            if (traceId == null || traceId.isBlank()) {
                traceId = event.getTag("otel.trace_id");
            }
            if (traceId != null && !traceId.isBlank()) {
                try {
                    event.setEventId(new io.sentry.protocol.SentryId(traceId));
                } catch (Exception ignored) {
                    // Fallback gracefully if traceId format does not fit 32-hex
                }
            }

            return event;
        };
    }
}
