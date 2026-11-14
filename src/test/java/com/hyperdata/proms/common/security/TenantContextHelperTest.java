package com.hyperdata.proms.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hyperdata.proms.common.exception.TenantRequiredException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class TenantContextHelperTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GIVEN authenticated user with activeTenantId WHEN getActiveTenantId THEN returns tenantId")
    void givenAuthenticatedUserWithTenant_whenGetActiveTenantId_thenReturnsTenantId() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        CurrentUser user = new CurrentUser(userId, "test@hyperdata.co.id", tenantId, "finance", 10L);

        var auth = new UsernamePasswordAuthenticationToken(user, null,
            List.of(new SimpleGrantedAuthority("ROLE_FINANCE")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(TenantContextHelper.getActiveTenantId()).contains(tenantId);
        assertThat(TenantContextHelper.requireActiveTenantId()).isEqualTo(tenantId);
        assertThat(TenantContextHelper.getWorkspaceRole()).isEqualTo("finance");
        assertThat(TenantContextHelper.getActiveUserId()).contains(userId);
    }

    @Test
    @DisplayName("GIVEN authenticated user without tenant WHEN requireActiveTenantId THEN throws TenantRequiredException")
    void givenUserWithoutTenant_whenRequireActiveTenantId_thenThrowsTenantRequiredException() {
        UUID userId = UUID.randomUUID();
        CurrentUser user = new CurrentUser(userId, "test@hyperdata.co.id", null, null, 10L);

        var auth = new UsernamePasswordAuthenticationToken(user, null,
            List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(TenantContextHelper.getActiveTenantId()).isEmpty();
        assertThatThrownBy(TenantContextHelper::requireActiveTenantId)
            .isInstanceOf(TenantRequiredException.class)
            .hasMessageContaining("Operasi ini memerlukan konteks tenant aktif yang valid.");
        assertThat(TenantContextHelper.getWorkspaceRole()).isEqualTo("member");
    }

    @Test
    @DisplayName("GIVEN unauthenticated request WHEN requireActiveTenantId THEN throws TenantRequiredException")
    void givenUnauthenticated_whenRequireActiveTenantId_thenThrowsTenantRequiredException() {
        assertThat(TenantContextHelper.getActiveTenantId()).isEmpty();
        assertThatThrownBy(TenantContextHelper::requireActiveTenantId)
            .isInstanceOf(TenantRequiredException.class);
    }
}
