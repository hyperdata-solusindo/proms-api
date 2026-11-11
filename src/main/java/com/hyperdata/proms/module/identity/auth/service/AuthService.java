package com.hyperdata.proms.module.identity.auth.service;

import com.hyperdata.proms.common.exception.BusinessException;
import com.hyperdata.proms.common.exception.InvalidCredentialsException;
import com.hyperdata.proms.common.util.Messages;
import com.hyperdata.proms.common.validation.YaviValidator;
import com.hyperdata.proms.module.identity.auth.dto.AuthUserDto;
import com.hyperdata.proms.module.identity.auth.dto.LoginRequest;
import com.hyperdata.proms.module.identity.auth.dto.LoginResponse;
import com.hyperdata.proms.module.identity.auth.repository.PersonalAccessTokenRepository;
import com.hyperdata.proms.module.identity.user.facade.UserFacade;
import com.hyperdata.proms.module.platform.tenant.facade.TenantFacade;
import com.hyperdata.proms.module.platform.tenant.facade.dto.TenantSummaryDto;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserFacade userFacade;
    private final TenantFacade tenantFacade;
    private final TokenService tokenService;
    private final PersonalAccessTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        YaviValidator.validateAndThrow(LoginRequest.validator, request);

        var user = userFacade
            .findByEmailForAuth(request.email())
            .filter(u -> passwordEncoder.matches(request.password(), u.password()))
            .orElseThrow(InvalidCredentialsException::new);

        if (!user.isActive()) {
            throw new BusinessException(
                Messages.get("auth.error.account_disabled"), HttpStatus.FORBIDDEN);
        }

        var membership = userFacade
            .resolveUserMembership(user.id(), Optional.empty())
            .orElseThrow(() -> new BusinessException(
                Messages.get("auth.error.no_active_tenant"), HttpStatus.FORBIDDEN));

        TenantSummaryDto tenant = tenantFacade
            .getTenantSummary(membership.tenantId())
            .orElseThrow(() -> new BusinessException(
                Messages.get("tenant.error.not_found"), HttpStatus.NOT_FOUND));

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
