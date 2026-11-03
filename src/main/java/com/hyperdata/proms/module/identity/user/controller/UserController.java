package com.hyperdata.proms.module.identity.user.controller;

import com.hyperdata.proms.common.response.ApiResponse;
import com.hyperdata.proms.common.security.CurrentUser;
import com.hyperdata.proms.common.security.SecurityContextHelper;
import com.hyperdata.proms.module.identity.user.dto.MeResponse;
import com.hyperdata.proms.module.identity.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;

    @Override
    public ApiResponse<MeResponse> getMe() {
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        return ApiResponse.ok(userService.getMe(currentUser), "Berhasil");
    }
}
