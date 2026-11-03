package com.hyperdata.proms.common.observability.config;

import io.sentry.SentryOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SentryConfig {

    @Bean
    public SentryOptions.BeforeSendCallback sentryBeforeSendCallback() {
        return (event, hint) -> {
            if (event.getRequest() != null && event.getRequest().getHeaders() != null) {
                event.getRequest().getHeaders().remove("Authorization");
                event.getRequest().getHeaders().remove("Cookie");
                event.getRequest().getHeaders().remove("Set-Cookie");
                event.getRequest().getHeaders().remove("X-Api-Key");
            }
            return event;
        };
    }
}
