package com.hyperdata.proms.common.ratelimit.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperdata.proms.common.ratelimit.constant.RateLimitHeaders;
import com.hyperdata.proms.common.ratelimit.model.RateLimitResult;
import com.hyperdata.proms.common.ratelimit.model.RateLimitTarget;
import com.hyperdata.proms.common.ratelimit.resolver.EdgeRateLimitResolver;
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

class PreAuthRateLimitingFilterTest {

    @Mock
    private EdgeRateLimitResolver edgeResolver;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private FilterChain filterChain;

    private PreAuthRateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new PreAuthRateLimitingFilter(edgeResolver, rateLimitService, new ObjectMapper());
    }

    @Test
    @DisplayName("Should pass request and release slot in finally block when tokens allowed")
    void shouldPassRequestWhenAllowed() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/projects");
        MockHttpServletResponse response = new MockHttpServletResponse();

        RateLimitTarget target = new RateLimitTarget("rl:edge:ip:127.0.0.1", 100, 100, Duration.ofSeconds(1), 20);
        when(edgeResolver.resolve(request)).thenReturn(List.of(target));
        when(rateLimitService.tryAcquire(target)).thenReturn(RateLimitResult.allowed(99, 1));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimitService).releaseInFlight(target);
        assertThat(response.getHeader(RateLimitHeaders.X_RATELIMIT_REMAINING)).isEqualTo("99");
    }

    @Test
    @DisplayName("Should block request and return HTTP 429 when rate limit exceeded")
    void shouldBlockWhenRateLimitExceeded() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/projects");
        MockHttpServletResponse response = new MockHttpServletResponse();

        RateLimitTarget target = new RateLimitTarget("rl:edge:ip:127.0.0.1", 100, 100, Duration.ofSeconds(1), 20);
        when(edgeResolver.resolve(request)).thenReturn(List.of(target));
        when(rateLimitService.tryAcquire(target)).thenReturn(RateLimitResult.rejected(0, 30, "Too many requests"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader(RateLimitHeaders.RETRY_AFTER)).isEqualTo("30");
        assertThat(response.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
    }
}
