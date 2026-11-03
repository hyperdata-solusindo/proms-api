package com.hyperdata.proms.module.identity.auth.controller;

import com.hyperdata.proms.common.response.ApiResponse;
import com.hyperdata.proms.common.security.CurrentUser;
import com.hyperdata.proms.common.security.SecurityContextHelper;
import com.hyperdata.proms.module.identity.auth.dto.LoginRequest;
import com.hyperdata.proms.module.identity.auth.dto.LoginResponse;
import com.hyperdata.proms.module.identity.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;

    @Override
    public ApiResponse<LoginResponse> login(LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ApiResponse.ok(response, "Berhasil login");
    }

    @Override
    public ApiResponse<Void> logout() {
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        authService.logout(currentUser.tokenId());
        return ApiResponse.ok(null, "Berhasil logout");
    }
}
