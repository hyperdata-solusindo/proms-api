package com.hyperdata.proms.common.ratelimit.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class RateLimitPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(TestConfig.class);

    @EnableConfigurationProperties(RateLimitProperties.class)
    static class TestConfig {
    }

    @Test
    @DisplayName("Should bind default values for RateLimitProperties")
    void shouldBindDefaultValues() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            RateLimitProperties properties = context.getBean(RateLimitProperties.class);
            assertThat(properties.enabled()).isTrue();
            assertThat(properties.edge().ipCapacity()).isEqualTo(100);
            assertThat(properties.edge().ipRefillDuration()).isEqualTo(Duration.ofSeconds(1));
            assertThat(properties.user().maxConcurrent()).isEqualTo(10);
            assertThat(properties.tenant().maxConcurrent()).isEqualTo(50);
        });
    }
}
