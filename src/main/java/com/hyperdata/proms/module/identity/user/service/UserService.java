package com.hyperdata.proms.module.identity.user.service;

import com.hyperdata.proms.common.exception.ResourceNotFoundException;
import com.hyperdata.proms.common.security.CurrentUser;
import com.hyperdata.proms.common.util.Lookups;
import com.hyperdata.proms.module.identity.user.constant.UserCacheNames;
import com.hyperdata.proms.module.identity.user.dto.MeResponse;
import com.hyperdata.proms.module.identity.user.dto.OtherTenantDto;
import com.hyperdata.proms.module.identity.user.entity.TenantMember;
import com.hyperdata.proms.module.identity.user.mapper.UserMapper;
import com.hyperdata.proms.module.identity.user.repository.TenantMemberRepository;
import com.hyperdata.proms.module.identity.user.repository.UserRepository;
import com.hyperdata.proms.module.platform.client.contract.TenantClient;
import com.hyperdata.proms.module.platform.client.dto.TenantDetailDto;
import com.hyperdata.proms.module.platform.client.dto.TenantSummaryDto;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final TenantMemberRepository tenantMemberRepository;
    private final TenantClient tenantClient;
    private final UserMapper userMapper;

    @Cacheable(
        cacheNames = UserCacheNames.USER_ME,
        key = "#currentUser.userId() + ':' + #currentUser.activeTenantId()",
        unless = "#result == null")
    public MeResponse getMe(CurrentUser currentUser) {
        UUID userId = currentUser.userId();
        UUID currentTenantId = currentUser.activeTenantId();

        var user = userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User tidak ditemukan"));

        TenantDetailDto tenantDetail = tenantClient
            .getTenantDetail(currentTenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Tenant tidak ditemukan"));

        var features = tenantClient.getTenantFeatures(currentTenantId);

        List<TenantMember> allMemberships = tenantMemberRepository.findByUserIdAndStatus(userId, "active");
        List<TenantMember> otherMemberships = allMemberships.stream()
            .filter(m -> !m.getTenantId().equals(currentTenantId))
            .toList();

        Set<UUID> otherTenantIds = Lookups.extractIds(otherMemberships, TenantMember::getTenantId);
        List<TenantSummaryDto> tenantSummaries = tenantClient.getTenantSummaries(otherTenantIds);
        Map<UUID, TenantSummaryDto> tenantSummaryMap = Lookups.toMap(tenantSummaries, TenantSummaryDto::id);

        List<OtherTenantDto> otherTenants = otherMemberships.stream()
            .map(m -> tenantSummaryMap.get(m.getTenantId()))
            .filter(Objects::nonNull)
            .map(summary -> new OtherTenantDto(summary.slug(), summary.name()))
            .toList();

        return new MeResponse(
            userMapper.toProfileDto(user), tenantDetail, currentUser.workspaceRole(), features, otherTenants);
    }
}
