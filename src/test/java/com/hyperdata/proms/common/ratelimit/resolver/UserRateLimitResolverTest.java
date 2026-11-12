package com.hyperdata.proms.common.ratelimit.resolver;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyperdata.proms.common.ratelimit.config.RateLimitProperties;
import com.hyperdata.proms.common.ratelimit.model.RateLimitTarget;
import com.hyperdata.proms.common.security.CurrentUser;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class UserRateLimitResolverTest {

    private final RateLimitProperties properties = new RateLimitProperties(
        true,
        new RateLimitProperties.EdgeProperties(100, 100, Duration.ofSeconds(1), 20),
        new RateLimitProperties.AuthProperties(15, 15, Duration.ofMinutes(1), 5, 5, Duration.ofMinutes(1)),
        new RateLimitProperties.UserProperties(60, 60, Duration.ofMinutes(1), 10),
        new RateLimitProperties.TenantProperties(1000, 1000, Duration.ofMinutes(1), 50),
        new RateLimitProperties.PublicGeneralProperties(30, 30, Duration.ofMinutes(1)));
    private final ClientIpResolver ipResolver = new ClientIpResolver();
    private final UserRateLimitResolver userResolver = new UserRateLimitResolver(properties, ipResolver);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should resolve User and Tenant targets when user is authenticated")
    void shouldResolveUserAndTenantTargetsWhenAuthenticated() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(userId, "test@example.com", tenantId, "owner", 1L);

        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null, List.of()));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/tasks");
        List<RateLimitTarget> targets = userResolver.resolve(request);

        assertThat(targets).hasSize(2);
        assertThat(targets).anyMatch(t -> t.bucketKey().equals("rl:user:" + userId) && t.capacity() == 60);
        assertThat(targets).anyMatch(t -> t.bucketKey().equals("rl:tenant:" + tenantId) && t.capacity() == 1000);
    }

    @Test
    @DisplayName("Should resolve public IP target when request is anonymous")
    void shouldResolvePublicIpTargetWhenAnonymous() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/docs");
        request.setRemoteAddr("10.0.0.1");

        List<RateLimitTarget> targets = userResolver.resolve(request);
        assertThat(targets).hasSize(1);
        assertThat(targets.get(0).bucketKey()).isEqualTo("rl:public:ip:10.0.0.1");
        assertThat(targets.get(0).capacity()).isEqualTo(30);
    }
}
