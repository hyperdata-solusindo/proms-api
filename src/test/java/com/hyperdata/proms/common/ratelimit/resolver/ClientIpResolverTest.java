package com.hyperdata.proms.common.ratelimit.resolver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

    private final ClientIpResolver ipResolver = new ClientIpResolver();

    @Test
    @DisplayName("Should extract first IP from X-Forwarded-For header")
    void shouldExtractFirstIpFromXForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.195, 70.41.3.18, 150.172.238.178");
        assertThat(ipResolver.resolveClientIp(request)).isEqualTo("203.0.113.195");
    }

    @Test
    @DisplayName("Should extract IP from X-Real-IP if X-Forwarded-For is missing")
    void shouldExtractIpFromXRealIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "198.51.100.42");
        assertThat(ipResolver.resolveClientIp(request)).isEqualTo("198.51.100.42");
    }

    @Test
    @DisplayName("Should fallback to RemoteAddr if headers are absent")
    void shouldFallbackToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        assertThat(ipResolver.resolveClientIp(request)).isEqualTo("127.0.0.1");
    }
}
