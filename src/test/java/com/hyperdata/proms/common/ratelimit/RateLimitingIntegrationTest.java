package com.hyperdata.proms.common.ratelimit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperdata.proms.common.ratelimit.constant.RateLimitHeaders;
import com.hyperdata.proms.common.ratelimit.filter.PostAuthRateLimitingFilter;
import com.hyperdata.proms.common.ratelimit.filter.PreAuthRateLimitingFilter;
import com.hyperdata.proms.common.ratelimit.model.RateLimitResult;
import com.hyperdata.proms.common.ratelimit.model.RateLimitTarget;
import com.hyperdata.proms.common.ratelimit.resolver.EdgeRateLimitResolver;
import com.hyperdata.proms.common.ratelimit.resolver.UserRateLimitResolver;
import com.hyperdata.proms.common.ratelimit.service.RateLimitService;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@ExtendWith(MockitoExtension.class)
class RateLimitingIntegrationTest {

    @RestController
    static class DummyTestController {

        @GetMapping("/api/v1/test-resource")
        public ResponseEntity<String> getResource() {
            return ResponseEntity.ok("OK");
        }
    }

    @Mock
    private EdgeRateLimitResolver edgeResolver;

    @Mock
    private UserRateLimitResolver userResolver;

    @Mock
    private RateLimitService rateLimitService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        PreAuthRateLimitingFilter preAuthFilter = new PreAuthRateLimitingFilter(
            edgeResolver, rateLimitService, objectMapper);
        PostAuthRateLimitingFilter postAuthFilter = new PostAuthRateLimitingFilter(
            userResolver, rateLimitService, objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(new DummyTestController())
            .addFilters(preAuthFilter, postAuthFilter)
            .build();
    }

    @Test
    @DisplayName("Should return 200 OK with RateLimit headers when quota is available")
    void shouldReturn200WhenQuotaAvailable() throws Exception {
        RateLimitTarget edgeTarget = new RateLimitTarget("rl:edge:ip:127.0.0.1", 100, 100, Duration.ofSeconds(1), 20);
        when(edgeResolver.resolve(any())).thenReturn(List.of(edgeTarget));
        when(userResolver.resolve(any())).thenReturn(List.of());
        when(rateLimitService.tryAcquire(edgeTarget)).thenReturn(RateLimitResult.allowed(99, 1));

        mockMvc.perform(get("/api/v1/test-resource"))
            .andExpect(status().isOk())
            .andExpect(header().string(RateLimitHeaders.X_RATELIMIT_LIMIT, "100"))
            .andExpect(header().string(RateLimitHeaders.X_RATELIMIT_REMAINING, "99"));
    }

    @Test
    @DisplayName("Should return 429 Too Many Requests when rate limit is exceeded at filter level")
    void shouldReturn429WhenRateLimitExceeded() throws Exception {
        RateLimitTarget edgeTarget = new RateLimitTarget("rl:edge:ip:127.0.0.1", 100, 100, Duration.ofSeconds(1), 20);
        when(edgeResolver.resolve(any())).thenReturn(List.of(edgeTarget));
        when(rateLimitService.tryAcquire(edgeTarget))
            .thenReturn(RateLimitResult.rejected(0, 45, "Rate limit exceeded"));

        mockMvc.perform(get("/api/v1/test-resource"))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().string(RateLimitHeaders.RETRY_AFTER, "45"))
            .andExpect(jsonPath("$.statusCode").value(429))
            .andExpect(jsonPath("$.error.code").value("RATE_LIMIT_EXCEEDED"));
    }
}
