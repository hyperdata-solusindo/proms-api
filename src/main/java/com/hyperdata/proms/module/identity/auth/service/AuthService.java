package com.hyperdata.proms.module.identity.auth.service;

import com.hyperdata.proms.common.exception.BusinessException;
import com.hyperdata.proms.common.exception.InvalidCredentialsException;
import com.hyperdata.proms.common.validation.YaviValidator;
import com.hyperdata.proms.module.identity.auth.dto.AuthUserDto;
import com.hyperdata.proms.module.identity.auth.dto.LoginRequest;
import com.hyperdata.proms.module.identity.auth.dto.LoginResponse;
import com.hyperdata.proms.module.identity.auth.repository.PersonalAccessTokenRepository;
import com.hyperdata.proms.module.identity.client.contract.UserClient;
import com.hyperdata.proms.module.platform.client.contract.TenantClient;
import com.hyperdata.proms.module.platform.client.dto.TenantSummaryDto;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserClient userClient;
    private final TenantClient tenantClient;
    private final TokenService tokenService;
    private final PersonalAccessTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        YaviValidator.validateAndThrow(LoginRequest.validator, request);

        var user = userClient
            .findByEmailForAuth(request.email())
            .filter(u -> passwordEncoder.matches(request.password(), u.password()))
            .orElseThrow(InvalidCredentialsException::new);

        if (!user.isActive()) {
            throw new BusinessException("Akun Anda telah dinonaktifkan", HttpStatus.FORBIDDEN);
        }

        var membership = userClient
            .resolveUserMembership(user.id(), Optional.empty())
            .orElseThrow(() -> new BusinessException("Pengguna tidak memiliki workspace/tenant aktif",
                HttpStatus.FORBIDDEN));

        TenantSummaryDto tenant = tenantClient
            .getTenantSummary(membership.tenantId())
            .orElseThrow(() -> new BusinessException("Tenant tidak ditemukan", HttpStatus.NOT_FOUND));

        String token = tokenService.createToken(user.id(), membership.tenantId());

        AuthUserDto authUser = new AuthUserDto(user.id(), user.fullName(), user.email(), user.locale(),
            user.timezone());

        return new LoginResponse(token, "Bearer", authUser, tenant, membership.role());
    }

    @Transactional
    public void logout(Long tokenId) {
        if (tokenId != null) {
            tokenRepository.deleteById(tokenId);
        }
    }
}
