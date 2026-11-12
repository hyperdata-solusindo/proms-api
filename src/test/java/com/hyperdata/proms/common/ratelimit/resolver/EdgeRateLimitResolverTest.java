package com.hyperdata.proms.common.ratelimit.resolver;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyperdata.proms.common.ratelimit.config.RateLimitProperties;
import com.hyperdata.proms.common.ratelimit.model.RateLimitTarget;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class EdgeRateLimitResolverTest {

    private final RateLimitProperties properties = new RateLimitProperties(
        true,
        new RateLimitProperties.EdgeProperties(100, 100, Duration.ofSeconds(1), 20),
        new RateLimitProperties.AuthProperties(15, 15, Duration.ofMinutes(1), 5, 5, Duration.ofMinutes(1)),
        new RateLimitProperties.UserProperties(60, 60, Duration.ofMinutes(1), 10),
        new RateLimitProperties.TenantProperties(1000, 1000, Duration.ofMinutes(1), 50),
        new RateLimitProperties.PublicGeneralProperties(30, 30, Duration.ofMinutes(1)));
    private final ClientIpResolver ipResolver = new ClientIpResolver();
    private final EdgeRateLimitResolver edgeResolver = new EdgeRateLimitResolver(ipResolver, properties);

    @Test
    @DisplayName("Should resolve global edge IP target for regular requests")
    void shouldResolveGlobalEdgeIpTarget() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/projects");
        request.setRemoteAddr("192.168.1.10");

        List<RateLimitTarget> targets = edgeResolver.resolve(request);
        assertThat(targets).hasSize(1);
        assertThat(targets.get(0).bucketKey()).isEqualTo("rl:edge:ip:192.168.1.10");
        assertThat(targets.get(0).capacity()).isEqualTo(100);
        assertThat(targets.get(0).maxConcurrent()).isEqualTo(20);
    }

    @Test
    @DisplayName("Should resolve auth login IP target specifically for auth login endpoint")
    void shouldResolveAuthLoginIpTarget() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("192.168.1.10");

        List<RateLimitTarget> targets = edgeResolver.resolve(request);
        assertThat(targets).hasSize(2);
        assertThat(targets).anyMatch(t -> t.bucketKey().equals("rl:auth:ip:192.168.1.10") && t.capacity() == 15);
    }
}
