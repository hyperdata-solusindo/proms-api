package com.hyperdata.proms.common.ratelimit.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperdata.proms.common.ratelimit.constant.RateLimitHeaders;
import com.hyperdata.proms.common.ratelimit.model.RateLimitResult;
import com.hyperdata.proms.common.ratelimit.model.RateLimitTarget;
import com.hyperdata.proms.common.ratelimit.resolver.UserRateLimitResolver;
import com.hyperdata.proms.common.ratelimit.service.RateLimitService;
import jakarta.servlet.FilterChain;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class PostAuthRateLimitingFilterTest {

    @Mock
    private UserRateLimitResolver userResolver;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private FilterChain filterChain;

    private PostAuthRateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new PostAuthRateLimitingFilter(userResolver, rateLimitService, new ObjectMapper());
    }

    @Test
    @DisplayName("Should evaluate user & tenant limits and release slots in finally block")
    void shouldEvaluateAndReleaseSlots() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/tasks");
        MockHttpServletResponse response = new MockHttpServletResponse();

        RateLimitTarget userTarget = new RateLimitTarget("rl:user:1", 60, 60, Duration.ofMinutes(1), 10);
        RateLimitTarget tenantTarget = new RateLimitTarget("rl:tenant:1", 1000, 1000, Duration.ofMinutes(1), 50);

        when(userResolver.resolve(request)).thenReturn(List.of(userTarget, tenantTarget));
        when(rateLimitService.tryAcquire(userTarget)).thenReturn(RateLimitResult.allowed(59, 1));
        when(rateLimitService.tryAcquire(tenantTarget)).thenReturn(RateLimitResult.allowed(999, 1));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimitService).releaseInFlight(userTarget);
        verify(rateLimitService).releaseInFlight(tenantTarget);
        assertThat(response.getHeader(RateLimitHeaders.X_RATELIMIT_REMAINING)).isEqualTo("999");
    }

    @Test
    @DisplayName("Should return 429 when user rate limit is exceeded")
    void shouldReturn429WhenUserLimitExceeded() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/tasks");
        MockHttpServletResponse response = new MockHttpServletResponse();

        RateLimitTarget userTarget = new RateLimitTarget("rl:user:1", 60, 60, Duration.ofMinutes(1), 10);

        when(userResolver.resolve(request)).thenReturn(List.of(userTarget));
        when(rateLimitService.tryAcquire(userTarget))
            .thenReturn(RateLimitResult.rejected(0, 45, "User limit exceeded"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader(RateLimitHeaders.RETRY_AFTER)).isEqualTo("45");
        assertThat(response.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
    }
}
