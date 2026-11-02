package com.hyperdata.proms.module.identity.user.facade.internal;

import com.hyperdata.proms.module.identity.user.facade.UserFacade;
import com.hyperdata.proms.module.identity.user.facade.dto.UserAuthDto;
import com.hyperdata.proms.module.identity.user.facade.dto.UserMembershipDto;
import com.hyperdata.proms.module.identity.user.mapper.UserMapper;
import com.hyperdata.proms.module.identity.user.repository.TenantMemberRepository;
import com.hyperdata.proms.module.identity.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultUserFacade implements UserFacade {

    private final UserRepository userRepository;
    private final TenantMemberRepository tenantMemberRepository;
    private final UserMapper userMapper;

    @Override
    public Optional<UserAuthDto> findByEmailForAuth(String email) {
        return userRepository.findByEmail(email).map(userMapper::toAuthDto);
    }

    @Override
    public Optional<UserAuthDto> findByIdForAuth(UUID userId) {
        return userRepository.findById(userId).map(userMapper::toAuthDto);
    }

    @Override
    public Optional<UserMembershipDto> resolveUserMembership(UUID userId, Optional<UUID> preferredTenantId) {
        if (preferredTenantId.isPresent()) {
            return tenantMemberRepository
                    .findMembership(userId, preferredTenantId.get(), "active")
                    .map(m -> new UserMembershipDto(m.getTenantId(), m.getRole()));
        }
        var activeMemberships = tenantMemberRepository.findByUserIdAndStatus(userId, "active");
        if (activeMemberships.isEmpty()) {
            return Optional.empty();
        }
        var first = activeMemberships.get(0);
        return Optional.of(new UserMembershipDto(first.getTenantId(), first.getRole()));
    }
}
