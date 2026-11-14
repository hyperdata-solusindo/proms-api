package com.hyperdata.proms.module.identity.auth.controller;

import com.hyperdata.proms.common.response.RestResponse;
import com.hyperdata.proms.common.security.CurrentUser;
import com.hyperdata.proms.common.security.SecurityContextHelper;
import com.hyperdata.proms.common.util.Messages;
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
    public RestResponse<LoginResponse> login(LoginRequest request) {
        LoginResponse response = authService.login(request);
        return RestResponse.ok(response, Messages.get("auth.login.success"));
    }

    @Override
    public RestResponse<Void> logout() {
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        authService.logout(currentUser.tokenId());
        return RestResponse.ok(null, Messages.get("auth.logout.success"));
    }
}
