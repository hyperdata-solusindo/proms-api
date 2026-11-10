package com.hyperdata.proms.module.identity.user.facade;

import com.hyperdata.proms.module.identity.user.facade.dto.UserAuthDto;
import com.hyperdata.proms.module.identity.user.facade.dto.UserMembershipDto;
import com.hyperdata.proms.module.identity.user.facade.dto.UserSummaryDto;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserFacade {

    Optional<UserAuthDto> findByEmailForAuth(String email);

    Optional<UserAuthDto> findByIdForAuth(UUID userId);

    Optional<UserMembershipDto> resolveUserMembership(UUID userId, Optional<UUID> preferredTenantId);

    List<UserSummaryDto> findSummariesByIds(Collection<UUID> userIds);
}
