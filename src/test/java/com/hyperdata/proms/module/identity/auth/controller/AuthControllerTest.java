package com.hyperdata.proms.module.identity.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperdata.proms.common.exception.GlobalExceptionHandler;
import com.hyperdata.proms.common.security.CurrentUser;
import com.hyperdata.proms.module.identity.auth.dto.AuthUserDto;
import com.hyperdata.proms.module.identity.auth.dto.LoginRequest;
import com.hyperdata.proms.module.identity.auth.dto.LoginResponse;
import com.hyperdata.proms.module.identity.auth.service.AuthService;
import com.hyperdata.proms.module.platform.tenant.facade.dto.TenantSummaryDto;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GIVEN valid login credentials WHEN POST /auth/login THEN returns 200 with LoginResponse envelope")
    void givenValidCredentials_whenLogin_thenReturns200Envelope() throws Exception {
        LoginRequest request = new LoginRequest("fariz@hyperdata.co.id", "SecretPassword123!");
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        AuthUserDto user = new AuthUserDto(userId, "Fariz Keren", "fariz@hyperdata.co.id", "id", "Asia/Jakarta");
        TenantSummaryDto tenant = new TenantSummaryDto(tenantId, "hyperdata", "Hyperdata");
        LoginResponse response = new LoginResponse("12|Xk9Token", "Bearer", user, tenant, "member");

        given(authService.login(any(LoginRequest.class))).willReturn(response);

        mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.message").value("Berhasil login"))
            .andExpect(jsonPath("$.data.token").value("12|Xk9Token"))
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.data.user.fullName").value("Fariz Keren"))
            .andExpect(jsonPath("$.data.tenant.slug").value("hyperdata"))
            .andExpect(jsonPath("$.data.workspaceRole").value("member"));
    }

    @Test
    @DisplayName("GIVEN authenticated user WHEN POST /auth/logout THEN invokes logout and returns 200 with data null")
    void givenAuthenticatedUser_whenLogout_thenReturns200WithDataNull() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(userId, "fariz@hyperdata.co.id", tenantId, "member", 42L);

        var auth = new UsernamePasswordAuthenticationToken(
            currentUser, null, List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(post("/auth/logout"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.message").value("Berhasil logout"))
            .andExpect(jsonPath("$.data").doesNotExist());

        verify(authService).logout(42L);
    }
}
