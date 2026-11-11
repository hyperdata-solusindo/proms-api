package com.hyperdata.proms.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityContextHelperTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GIVEN authenticated user in SecurityContext WHEN getCurrentUser THEN returns CurrentUser record")
    void givenAuthenticatedUser_whenGetCurrentUser_thenReturnsRecord() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        CurrentUser user = new CurrentUser(userId, "test@hyperdata.co.id", tenantId, "member", 1L);

        var auth = new UsernamePasswordAuthenticationToken(user, null,
            List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        var result = SecurityContextHelper.getCurrentUser();
        assertThat(result).isPresent();
        assertThat(result.get().userId()).isEqualTo(userId);
        assertThat(result.get().email()).isEqualTo("test@hyperdata.co.id");
        assertThat(result.get().activeTenantId()).isEqualTo(tenantId);
        assertThat(result.get().workspaceRole()).isEqualTo("member");
        assertThat(result.get().tokenId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("GIVEN empty SecurityContext WHEN getRequiredCurrentUser THEN throws UnauthenticatedException")
    void givenEmptySecurityContext_whenGetRequiredCurrentUser_thenThrowsUnauthenticatedException() {
        org.assertj.core.api.Assertions.assertThatThrownBy(SecurityContextHelper::getRequiredCurrentUser)
            .isInstanceOf(com.hyperdata.proms.common.exception.UnauthenticatedException.class)
            .hasMessage("Autentikasi diperlukan untuk mengakses endpoint ini");
    }
}
