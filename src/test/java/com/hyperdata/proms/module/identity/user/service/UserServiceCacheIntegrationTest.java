package com.hyperdata.proms.module.identity.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.hyperdata.proms.common.config.CacheConfig;
import com.hyperdata.proms.common.security.CurrentUser;
import com.hyperdata.proms.module.identity.user.dto.MeResponse;
import com.hyperdata.proms.module.identity.user.entity.User;
import com.hyperdata.proms.module.identity.user.mapper.UserMapper;
import com.hyperdata.proms.module.identity.user.repository.TenantMemberRepository;
import com.hyperdata.proms.module.identity.user.repository.UserRepository;
import com.hyperdata.proms.module.platform.client.contract.TenantClient;
import com.hyperdata.proms.module.platform.client.dto.TenantDetailDto;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = {UserServiceCacheIntegrationTest.TestConfig.class})
class UserServiceCacheIntegrationTest {

    @Configuration
    @Import(CacheConfig.class)
    static class TestConfig {

        @Bean
        public UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        public TenantMemberRepository tenantMemberRepository() {
            return mock(TenantMemberRepository.class);
        }

        @Bean
        public TenantClient tenantClient() {
            return mock(TenantClient.class);
        }

        @Bean
        public UserMapper userMapper() {
            return Mappers.getMapper(UserMapper.class);
        }

        @Bean
        public UserService userService(
            UserRepository userRepository,
            TenantMemberRepository tenantMemberRepository,
            TenantClient tenantClient,
            UserMapper userMapper) {
            return new UserService(userRepository, tenantMemberRepository, tenantClient, userMapper);
        }
    }

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantClient tenantClient;

    @Autowired
    private TenantMemberRepository tenantMemberRepository;

    @Test
    @DisplayName("Given cached user service, when getMe called twice with same currentUser, then repository queried only once")
    void givenCachedUserService_whenGetMeCalledTwice_thenRepositoryQueriedOnlyOnce() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(userId, "dev@hyperdata.co.id", tenantId, "admin", 1L);

        User user = new User();
        user.setId(userId);
        user.setFullName("Developer User");
        user.setEmail("dev@hyperdata.co.id");

        TenantDetailDto tenantDetail = new TenantDetailDto(
            tenantId, "hyperdata", "Hyperdata", "active", "Asia/Jakarta");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(tenantClient.getTenantDetail(tenantId)).willReturn(Optional.of(tenantDetail));
        given(tenantClient.getTenantFeatures(tenantId)).willReturn(Map.of("points", true));
        given(tenantMemberRepository.findByUserIdAndStatus(userId, "active")).willReturn(List.of());

        // First call - cache miss, queries DB and populates cache
        MeResponse response1 = userService.getMe(currentUser);
        assertThat(response1.user().fullName()).isEqualTo("Developer User");

        // Second call - cache hit, served from L1 cache
        MeResponse response2 = userService.getMe(currentUser);
        assertThat(response2.user().fullName()).isEqualTo("Developer User");

        // Verify repository and tenantClient were called only once
        verify(userRepository, times(1)).findById(userId);
        verify(tenantClient, times(1)).getTenantDetail(tenantId);
    }

    @Test
    @DisplayName("Given different active tenants for same user, when getMe called, then cached independently")
    void givenDifferentActiveTenants_whenGetMeCalled_thenCachedIndependently() {
        UUID userId = UUID.randomUUID();
        UUID tenantId1 = UUID.randomUUID();
        UUID tenantId2 = UUID.randomUUID();

        CurrentUser userTenant1 = new CurrentUser(userId, "dev@hyperdata.co.id", tenantId1, "admin", 1L);
        CurrentUser userTenant2 = new CurrentUser(userId, "dev@hyperdata.co.id", tenantId2, "member", 1L);

        User user = new User();
        user.setId(userId);
        user.setFullName("Developer User");
        user.setEmail("dev@hyperdata.co.id");

        TenantDetailDto tenantDetail1 = new TenantDetailDto(
            tenantId1, "tenant-1", "Tenant 1", "active", "Asia/Jakarta");
        TenantDetailDto tenantDetail2 = new TenantDetailDto(
            tenantId2, "tenant-2", "Tenant 2", "active", "Asia/Jakarta");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(tenantClient.getTenantDetail(tenantId1)).willReturn(Optional.of(tenantDetail1));
        given(tenantClient.getTenantDetail(tenantId2)).willReturn(Optional.of(tenantDetail2));
        given(tenantClient.getTenantFeatures(any())).willReturn(Map.of());
        given(tenantMemberRepository.findByUserIdAndStatus(userId, "active")).willReturn(List.of());

        MeResponse r1 = userService.getMe(userTenant1);
        MeResponse r2 = userService.getMe(userTenant2);

        assertThat(r1.tenant().slug()).isEqualTo("tenant-1");
        assertThat(r2.tenant().slug()).isEqualTo("tenant-2");

        // Because tenant IDs were different, cache missed for second tenant
        verify(userRepository, times(2)).findById(userId);
    }
}
