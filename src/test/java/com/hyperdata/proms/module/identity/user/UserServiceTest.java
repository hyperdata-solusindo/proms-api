package com.hyperdata.proms.module.identity.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;

import com.hyperdata.proms.common.security.CurrentUser;
import com.hyperdata.proms.module.identity.user.entity.TenantMember;
import com.hyperdata.proms.module.identity.user.entity.User;
import com.hyperdata.proms.module.identity.user.mapper.UserMapper;
import com.hyperdata.proms.module.identity.user.repository.TenantMemberRepository;
import com.hyperdata.proms.module.identity.user.repository.UserRepository;
import com.hyperdata.proms.module.identity.user.service.UserService;
import com.hyperdata.proms.module.platform.client.contract.TenantClient;
import com.hyperdata.proms.module.platform.client.dto.TenantDetailDto;
import com.hyperdata.proms.module.platform.client.dto.TenantSummaryDto;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TenantMemberRepository tenantMemberRepository;

    @Mock
    private TenantClient tenantClient;

    @Spy
    private UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("GIVEN authenticated user WHEN getMe THEN returns complete profile with active tenant, features, and otherTenants")
    void givenAuthenticatedUser_whenGetMe_thenReturnsFullProfile() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID otherTenantId = UUID.randomUUID();

        CurrentUser currentUser = new CurrentUser(userId, "fariz@hyperdata.co.id", tenantId, "member", 1L);

        User user = new User();
        user.setId(userId);
        user.setFullName("Fariz Keren");
        user.setEmail("fariz@hyperdata.co.id");

        TenantMember otherMember = new TenantMember();
        otherMember.setId(UUID.randomUUID());
        otherMember.setUserId(userId);
        otherMember.setTenantId(otherTenantId);
        otherMember.setRole("member");
        otherMember.setStatus("active");

        TenantDetailDto tenantDetail = new TenantDetailDto(tenantId, "hyperdata", "Hyperdata", "active",
            "Asia/Jakarta");
        TenantSummaryDto otherSummary = new TenantSummaryDto(otherTenantId, "telemedis", "Telemedis");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(tenantClient.getTenantDetail(tenantId)).willReturn(Optional.of(tenantDetail));
        given(tenantClient.getTenantFeatures(tenantId)).willReturn(Map.of("points", true, "standup", true));
        given(tenantMemberRepository.findByUserIdAndStatus(userId, "active")).willReturn(List.of(otherMember));
        given(tenantClient.getTenantSummaries(anyCollection())).willReturn(List.of(otherSummary));

        var response = userService.getMe(currentUser);

        assertThat(response.user().fullName()).isEqualTo("Fariz Keren");
        assertThat(response.tenant().slug()).isEqualTo("hyperdata");
        assertThat(response.workspaceRole()).isEqualTo("member");
        assertThat(response.features().get("points")).isTrue();
        assertThat(response.otherTenants()).hasSize(1);
        assertThat(response.otherTenants().get(0).slug()).isEqualTo("telemedis");
    }
}
