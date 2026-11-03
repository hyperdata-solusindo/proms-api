package com.hyperdata.proms.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.hyperdata.proms.module.identity.auth.entity.PersonalAccessToken;
import com.hyperdata.proms.module.identity.auth.service.TokenService;
import com.hyperdata.proms.module.identity.user.facade.UserFacade;
import com.hyperdata.proms.module.identity.user.facade.dto.UserAuthDto;
import com.hyperdata.proms.module.identity.user.facade.dto.UserMembershipDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.time.Instant;
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
    private TokenService tokenService;

    @Mock
    private UserFacade userFacade;

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

        PersonalAccessToken pat = new PersonalAccessToken();
        pat.setId(1L);
        pat.setTokenableId(userId);
        pat.setAbilities("tenant:" + tenantId);
        pat.setExpiresAt(Instant.now().plusSeconds(3600));

        UserAuthDto user = new UserAuthDto(userId, "user@hyperdata.co.id", "hashed", "User Test", "id", "Asia/Jakarta",
            true);
        UserMembershipDto membership = new UserMembershipDto(tenantId, "admin");

        given(tokenService.verifyToken(1L, "validPlainToken")).willReturn(Optional.of(pat));
        given(userFacade.findByIdForAuth(userId)).willReturn(Optional.of(user));
        given(userFacade.resolveUserMembership(userId, Optional.of(tenantId))).willReturn(Optional.of(membership));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        CurrentUser principal = (CurrentUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal.email()).isEqualTo("user@hyperdata.co.id");
        assertThat(principal.workspaceRole()).isEqualTo("admin");
        assertThat(principal.tokenId()).isEqualTo(1L);
        verify(tokenService).recordTokenUsage(1L);
        verify(filterChain).doFilter(request, response);
    }
}
