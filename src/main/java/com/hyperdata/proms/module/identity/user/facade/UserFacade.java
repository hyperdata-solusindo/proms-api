package com.hyperdata.proms.module.identity.user.facade;

import com.hyperdata.proms.module.identity.user.facade.dto.UserAuthDto;
import com.hyperdata.proms.module.identity.user.facade.dto.UserMembershipDto;
import java.util.Optional;
import java.util.UUID;

public interface UserFacade {
    Optional<UserAuthDto> findByEmailForAuth(String email);

    Optional<UserAuthDto> findByIdForAuth(UUID userId);

    Optional<UserMembershipDto> resolveUserMembership(UUID userId, Optional<UUID> preferredTenantId);
}
