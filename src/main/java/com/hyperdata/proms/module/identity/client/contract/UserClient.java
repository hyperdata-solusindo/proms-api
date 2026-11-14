package com.hyperdata.proms.module.identity.client.contract;

import com.hyperdata.proms.module.identity.client.dto.UserAuthDto;
import com.hyperdata.proms.module.identity.client.dto.UserMembershipDto;
import com.hyperdata.proms.module.identity.client.dto.UserSummaryDto;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserClient {

    Optional<UserAuthDto> findByEmailForAuth(String email);

    Optional<UserAuthDto> findByIdForAuth(UUID userId);

    Optional<UserMembershipDto> resolveUserMembership(UUID userId, Optional<UUID> preferredTenantId);

    List<UserSummaryDto> findSummariesByIds(Collection<UUID> userIds);
}
