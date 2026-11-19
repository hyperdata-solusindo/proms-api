package com.hyperdata.proms.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.hyperdata.proms.module.identity.client.contract.AuthClient;
import com.hyperdata.proms.module.identity.client.contract.UserClient;
import com.hyperdata.proms.module.identity.client.dto.UserAuthDto;
import com.hyperdata.proms.module.identity.client.dto.UserMembershipDto;
import com.hyperdata.proms.module.identity.client.dto.VerifiedTokenDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class TokenAuthenticationFilterTest {

    @Mock
    private AuthClient authClient;

    @Mock
    private UserClient userClient;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private TokenAuthenticationFilter filter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GIVEN valid Bearer token WHEN doFilterInternal THEN sets CurrentUser in SecurityContext")
    void givenValidBearerToken_whenFilter_thenSetsSecurityContext() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer 1|validPlainToken");
        MockHttpServletResponse response = new MockHttpServletResponse();

        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        VerifiedTokenDto token = new VerifiedTokenDto(1L, userId, Optional.of(tenantId));

        UserAuthDto user = new UserAuthDto(userId, "user@hyperdata.co.id", "hashed", "User Test", "id", "Asia/Jakarta",
            true);
        UserMembershipDto membership = new UserMembershipDto(tenantId, "admin");

        given(authClient.verifyToken(1L, "validPlainToken")).willReturn(Optional.of(token));
        given(userClient.findByIdForAuth(userId)).willReturn(Optional.of(user));
        given(userClient.resolveUserMembership(userId, Optional.of(tenantId))).willReturn(Optional.of(membership));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        CurrentUser principal = (CurrentUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal.email()).isEqualTo("user@hyperdata.co.id");
        assertThat(principal.workspaceRole()).isEqualTo("admin");
        assertThat(principal.tokenId()).isEqualTo(1L);
        verify(authClient).recordTokenUsage(1L);
        verify(filterChain).doFilter(request, response);
    }
}
