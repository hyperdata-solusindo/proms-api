package com.hyperdata.proms.module.identity.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.hyperdata.proms.module.identity.auth.dto.LoginRequest;
import com.hyperdata.proms.module.identity.auth.repository.PersonalAccessTokenRepository;
import com.hyperdata.proms.module.identity.auth.service.AuthService;
import com.hyperdata.proms.module.identity.auth.service.TokenService;
import com.hyperdata.proms.module.identity.user.facade.UserFacade;
import com.hyperdata.proms.module.identity.user.facade.dto.UserAuthDto;
import com.hyperdata.proms.module.identity.user.facade.dto.UserMembershipDto;
import com.hyperdata.proms.module.platform.tenant.facade.TenantFacade;
import com.hyperdata.proms.module.platform.tenant.facade.dto.TenantSummaryDto;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserFacade userFacade;

    @Mock
    private TenantFacade tenantFacade;

    @Mock
    private TokenService tokenService;

    @Mock
    private PersonalAccessTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("GIVEN invalid password WHEN login THEN throws BusinessException 401")
    void givenInvalidPassword_whenLogin_thenThrows401() {
        LoginRequest request = new LoginRequest("test@hyperdata.co.id", "WrongPass!");
        UserAuthDto userDto = new UserAuthDto(
            UUID.randomUUID(), "test@hyperdata.co.id", "$2a$10$hashed", "Fariz", "id", "Asia/Jakarta", true);

        given(userFacade.findByEmailForAuth("test@hyperdata.co.id")).willReturn(Optional.of(userDto));
        given(passwordEncoder.matches("WrongPass!", "$2a$10$hashed")).willReturn(false);

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(com.hyperdata.proms.common.exception.InvalidCredentialsException.class)
            .hasMessage("Email atau kata sandi tidak sesuai");
    }

    @Test
    @DisplayName("GIVEN valid credentials WHEN login THEN returns LoginResponse with token")
    void givenValidCredentials_whenLogin_thenReturnsLoginResponse() {
        LoginRequest request = new LoginRequest("test@hyperdata.co.id", "Secret123!");
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UserAuthDto userDto = new UserAuthDto(userId, "test@hyperdata.co.id", "$2a$10$hashed", "Fariz", "id",
            "Asia/Jakarta", true);
        UserMembershipDto membership = new UserMembershipDto(tenantId, "member");
        TenantSummaryDto tenantSummary = new TenantSummaryDto(tenantId, "hyperdata", "Hyperdata");

        given(userFacade.findByEmailForAuth("test@hyperdata.co.id")).willReturn(Optional.of(userDto));
        given(passwordEncoder.matches("Secret123!", "$2a$10$hashed")).willReturn(true);
        given(userFacade.resolveUserMembership(userId, Optional.empty())).willReturn(Optional.of(membership));
        given(tenantFacade.getTenantSummary(tenantId)).willReturn(Optional.of(tenantSummary));
        given(tokenService.createToken(userId, tenantId)).willReturn("12|PlainTokenSecret");

        var response = authService.login(request);

        assertThat(response.token()).isEqualTo("12|PlainTokenSecret");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().email()).isEqualTo("test@hyperdata.co.id");
        assertThat(response.tenant().slug()).isEqualTo("hyperdata");
        assertThat(response.workspaceRole()).isEqualTo("member");
    }
}
