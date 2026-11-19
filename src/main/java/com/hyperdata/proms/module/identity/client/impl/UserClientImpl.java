package com.hyperdata.proms.module.identity.client.impl;

import com.hyperdata.proms.common.util.Lookups;
import com.hyperdata.proms.module.identity.client.contract.UserClient;
import com.hyperdata.proms.module.identity.client.dto.UserAuthDto;
import com.hyperdata.proms.module.identity.client.dto.UserMembershipDto;
import com.hyperdata.proms.module.identity.client.dto.UserSummaryDto;
import com.hyperdata.proms.module.identity.user.entity.User;
import com.hyperdata.proms.module.identity.user.mapper.UserMapper;
import com.hyperdata.proms.module.identity.user.repository.TenantMemberRepository;
import com.hyperdata.proms.module.identity.user.repository.UserRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserClientImpl implements UserClient {

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

    @Override
    public List<UserSummaryDto> findSummariesByIds(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return userRepository.findAllById(userIds)
            .stream()
            .map(u -> new UserSummaryDto(u.getId(), u.getFullName(), u.getEmail(), u.getAvatarUrl()))
            .toList();
    }

    @Override
    public Map<String, UserSummaryDto> findUsersByEmails(Collection<String> emails) {
        if (emails == null || emails.isEmpty()) {
            return Map.of();
        }
        List<User> users = userRepository.findByEmailIn(emails);
        return Lookups.toMap(
            users,
            User::getEmail,
            u -> new UserSummaryDto(u.getId(), u.getFullName(), u.getEmail(), u.getAvatarUrl()));
    }
}
