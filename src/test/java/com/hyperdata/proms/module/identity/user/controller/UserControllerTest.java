package com.hyperdata.proms.module.identity.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hyperdata.proms.common.exception.GlobalExceptionHandler;
import com.hyperdata.proms.common.security.CurrentUser;
import com.hyperdata.proms.module.identity.user.dto.MeResponse;
import com.hyperdata.proms.module.identity.user.dto.OtherTenantDto;
import com.hyperdata.proms.module.identity.user.dto.UserProfileDto;
import com.hyperdata.proms.module.identity.user.service.UserService;
import com.hyperdata.proms.module.platform.tenant.facade.dto.TenantDetailDto;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GIVEN authenticated user WHEN GET /me THEN returns 200 with MeResponse envelope")
    void givenAuthenticatedUser_whenGetMe_thenReturns200Envelope() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(userId, "fariz@hyperdata.co.id", tenantId, "member", 42L);

        var auth = new UsernamePasswordAuthenticationToken(
                currentUser, null, List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        UserProfileDto userProfile = new UserProfileDto(userId, "Fariz Keren", "fariz@hyperdata.co.id");
        TenantDetailDto tenantDetail =
                new TenantDetailDto(tenantId, "hyperdata", "Hyperdata", "active", "Asia/Jakarta");
        MeResponse meResponse = new MeResponse(
                userProfile,
                tenantDetail,
                "member",
                Map.of("points", true, "standup", true),
                List.of(new OtherTenantDto("telemedis", "Telemedis")));

        given(userService.getMe(any(CurrentUser.class))).willReturn(meResponse);

        mockMvc.perform(get("/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Berhasil"))
                .andExpect(jsonPath("$.data.user.fullName").value("Fariz Keren"))
                .andExpect(jsonPath("$.data.tenant.slug").value("hyperdata"))
                .andExpect(jsonPath("$.data.workspaceRole").value("member"))
                .andExpect(jsonPath("$.data.features.points").value(true))
                .andExpect(jsonPath("$.data.otherTenants[0].slug").value("telemedis"));
    }
}
