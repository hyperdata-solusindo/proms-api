# Auth and Profile Endpoints Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build authentication (`POST /auth/login`, `POST /auth/logout`) and user profile (`GET /me`) endpoints adhering to a 2-Layer Modular Monolith architecture, Personal Access Token (PAT) authentication, and exact JSON response envelope contracts.

**Architecture:** Follows the 2-Layer Modular Monolith architecture (`module.identity.auth`, `module.identity.user`, `module.platform.tenant`). Cross-module interactions communicate exclusively through Facade interfaces (`UserFacade`, `TenantFacade`) returning Java 21 `record` DTOs. A stateless Spring Security 6 filter chain validates Bearer PAT tokens (`{id}|{plainToken}`) hashed with SHA-256 against `platform.personal_access_tokens`.

**Tech Stack:** Java 21, Spring Boot 4.1.x, Spring Security 6, Spring Data JPA, PostgreSQL 16 (`platform.*` schema), MapStruct 1.6.3, YAVI 0.14.1, JUnit 5, MockMvc, ArchUnit.

**Spec:** [`docs/superpowers/specs/2026-11-02-auth-and-profile-design.md`](file:///home/kholifan/html/project-promps/api/docs/superpowers/specs/2026-11-02-auth-and-profile-design.md)

## Global Constraints

- Java 21 toolchain and Spring Boot 4.1.x conventions.
- 2-Layer Modular Monolith: code resides in `com.hyperdata.proms.module.<domain>.<feature>`.
- Public facade interfaces reside in `facade/` and implementations in `facade/internal/` annotated with `@Component`.
- Foreign module injection MUST only use `*Facade` interfaces; never inject foreign services or repositories.
- Zero JPA `@Entity` leakage: Facades and Controllers must only return Java 21 `record` DTOs.
- DTO folders remain flat within each feature (`dto/`); no subfolders like `dto/request` or `dto/response`.
- DTO naming suffixes: `*Request` for controller inputs, `*Response` for controller outputs, `*Dto` for facade contracts.
- Input validation MUST use YAVI (`Validator<T>`).
- Object conversions MUST use MapStruct (`componentModel = "spring"`, `unmappedTargetPolicy = ReportingPolicy.IGNORE`).
- Response envelopes match exact frontend contract: `{ "statusCode": int, "message": string, "data": T }`.
- Password verification MUST use `BCryptPasswordEncoder`.
- Git rule: Never execute `git commit` or `git push` automatically.

---

### Task 1: Add Spring Security & Test Dependencies

**Files:**
- Modify: `build.gradle.kts:30-45`

**Interfaces:**
- Consumes: Gradle Kotlin DSL dependencies block
- Produces: Spring Security 6, Spring Security Crypto, and Spring Security Test on classpath

- [ ] **Step 1: Check existing dependencies in build.gradle.kts**

Inspect `build.gradle.kts` to verify `spring-boot-starter-security` and `spring-security-test` are absent.

- [ ] **Step 2: Add Spring Security dependencies**

Add the following dependencies to the `dependencies` block in `build.gradle.kts`:
```kotlin
    // Spring Security
    implementation("org.springframework.boot:spring-boot-starter-security")
    testImplementation("org.springframework.security:spring-security-test")
```

- [ ] **Step 3: Run gradle build to ensure dependencies download and compile**

Run: `./gradlew compileJava compileTestJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Verify deliverable**

Run: `./gradlew check -x test`
Expected: Spotless and compilation pass.

---

### Task 2: Standardize Response Envelope & Security Context Models

**Files:**
- Modify: `src/main/java/com/hyperdata/proms/common/response/ApiResponse.java`
- Create: `src/main/java/com/hyperdata/proms/common/security/CurrentUser.java`
- Create: `src/main/java/com/hyperdata/proms/common/security/SecurityContextHelper.java`
- Test: `src/test/java/com/hyperdata/proms/common/response/ApiResponseTest.java`

**Interfaces:**
- Consumes: `HttpStatus`
- Produces: 
  - `ApiResponse<T>(int statusCode, String message, T data)`
  - `CurrentUser(UUID userId, String email, UUID activeTenantId, String workspaceRole, Long tokenId)`
  - `SecurityContextHelper.getCurrentUser()`

- [ ] **Step 1: Write test for ApiResponse and CurrentUser**

Create `src/test/java/com/hyperdata/proms/common/security/SecurityContextHelperTest.java`:
```java
package com.hyperdata.proms.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityContextHelperTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GIVEN authenticated user in SecurityContext WHEN getCurrentUser THEN returns CurrentUser record")
    void givenAuthenticatedUser_whenGetCurrentUser_thenReturnsRecord() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        CurrentUser user = new CurrentUser(userId, "test@hyperdata.co.id", tenantId, "member", 1L);

        var auth = new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        var result = SecurityContextHelper.getCurrentUser();
        assertThat(result).isPresent();
        assertThat(result.get().userId()).isEqualTo(userId);
        assertThat(result.get().email()).isEqualTo("test@hyperdata.co.id");
        assertThat(result.get().activeTenantId()).isEqualTo(tenantId);
        assertThat(result.get().workspaceRole()).isEqualTo("member");
        assertThat(result.get().tokenId()).isEqualTo(1L);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.hyperdata.proms.common.security.SecurityContextHelperTest"`
Expected: Compilation failure because `CurrentUser` and `SecurityContextHelper` do not exist.

- [ ] **Step 3: Implement CurrentUser, SecurityContextHelper, and adapt ApiResponse**

Update `src/main/java/com/hyperdata/proms/common/response/ApiResponse.java`:
```java
package com.hyperdata.proms.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(int statusCode, String message, T data) {
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(HttpStatus.OK.value(), message, data);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return ok(data, "Berhasil");
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return new ApiResponse<>(HttpStatus.CREATED.value(), message, data);
    }

    public static <T> ApiResponse<T> error(HttpStatus status, String message, T data) {
        return new ApiResponse<>(status.value(), message, data);
    }

    public static ApiResponse<Void> error(HttpStatus status, String message) {
        return error(status, message, null);
    }
}
```

Create `src/main/java/com/hyperdata/proms/common/security/CurrentUser.java`:
```java
package com.hyperdata.proms.common.security;

import java.util.UUID;

public record CurrentUser(
        UUID userId,
        String email,
        UUID activeTenantId,
        String workspaceRole,
        Long tokenId
) {}
```

Create `src/main/java/com/hyperdata/proms/common/security/SecurityContextHelper.java`:
```java
package com.hyperdata.proms.common.security;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityContextHelper {
    private SecurityContextHelper() {}

    public static Optional<CurrentUser> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CurrentUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public static CurrentUser getRequiredCurrentUser() {
        return getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("No authenticated user found in security context"));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.hyperdata.proms.common.security.SecurityContextHelperTest"`
Expected: PASS

- [ ] **Step 5: Verify deliverable**

Run: `./gradlew spotlessApply && ./gradlew test --tests "com.hyperdata.proms.common.*"`
Expected: PASS

---

### Task 3: Module `platform.tenant` (Entities, DTOs, Repository, Mapper & Facade)

**Files:**
- Create: `src/main/java/com/hyperdata/proms/module/platform/tenant/entity/Tenant.java`
- Create: `src/main/java/com/hyperdata/proms/module/platform/tenant/entity/Subscription.java`
- Create: `src/main/java/com/hyperdata/proms/module/platform/tenant/entity/Plan.java`
- Create: `src/main/java/com/hyperdata/proms/module/platform/tenant/facade/dto/TenantDetailDto.java`
- Create: `src/main/java/com/hyperdata/proms/module/platform/tenant/facade/dto/TenantSummaryDto.java`
- Create: `src/main/java/com/hyperdata/proms/module/platform/tenant/repository/TenantRepository.java`
- Create: `src/main/java/com/hyperdata/proms/module/platform/tenant/repository/SubscriptionRepository.java`
- Create: `src/main/java/com/hyperdata/proms/module/platform/tenant/repository/PlanRepository.java`
- Create: `src/main/java/com/hyperdata/proms/module/platform/tenant/mapper/TenantMapper.java`
- Create: `src/main/java/com/hyperdata/proms/module/platform/tenant/facade/TenantFacade.java`
- Create: `src/main/java/com/hyperdata/proms/module/platform/tenant/facade/internal/DefaultTenantFacade.java`
- Test: `src/test/java/com/hyperdata/proms/module/platform/tenant/TenantFacadeTest.java`

**Interfaces:**
- Consumes: PostgreSQL `platform.tenants`, `platform.subscriptions`, `platform.plans`
- Produces: 
  - `TenantFacade.getTenantDetail(UUID tenantId): Optional<TenantDetailDto>`
  - `TenantFacade.getTenantSummary(UUID tenantId): Optional<TenantSummaryDto>`
  - `TenantFacade.getTenantFeatures(UUID tenantId): Map<String, Boolean>`

- [ ] **Step 1: Write failing unit test for TenantFacade**

Create `src/test/java/com/hyperdata/proms/module/platform/tenant/TenantFacadeTest.java`:
```java
package com.hyperdata.proms.module.platform.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.hyperdata.proms.module.platform.tenant.entity.Tenant;
import com.hyperdata.proms.module.platform.tenant.facade.TenantFacade;
import com.hyperdata.proms.module.platform.tenant.facade.internal.DefaultTenantFacade;
import com.hyperdata.proms.module.platform.tenant.mapper.TenantMapper;
import com.hyperdata.proms.module.platform.tenant.repository.PlanRepository;
import com.hyperdata.proms.module.platform.tenant.repository.SubscriptionRepository;
import com.hyperdata.proms.module.platform.tenant.repository.TenantRepository;
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
class TenantFacadeTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PlanRepository planRepository;

    @Spy
    private TenantMapper tenantMapper = Mappers.getMapper(TenantMapper.class);

    @InjectMocks
    private DefaultTenantFacade tenantFacade;

    @Test
    @DisplayName("GIVEN existing tenant ID WHEN getTenantDetail THEN returns mapped TenantDetailDto")
    void givenExistingTenantId_whenGetTenantDetail_thenReturnsDetail() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("Hyperdata");
        tenant.setSlug("hyperdata");
        tenant.setStatus("active");
        tenant.setTimezone("Asia/Jakarta");

        given(tenantRepository.findById(tenantId)).willReturn(Optional.of(tenant));

        var result = tenantFacade.getTenantDetail(tenantId);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(tenantId);
        assertThat(result.get().name()).isEqualTo("Hyperdata");
        assertThat(result.get().slug()).isEqualTo("hyperdata");
        assertThat(result.get().status()).isEqualTo("active");
        assertThat(result.get().timezone()).isEqualTo("Asia/Jakarta");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.hyperdata.proms.module.platform.tenant.TenantFacadeTest"`
Expected: Compilation failure (classes not yet created).

- [ ] **Step 3: Implement Entities, Repositories, DTOs, Mapper, and Facade**

Create `src/main/java/com/hyperdata/proms/module/platform/tenant/entity/Tenant.java`:
```java
package com.hyperdata.proms.module.platform.tenant.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "tenants", schema = "platform")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Tenant {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String timezone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
```

Create `src/main/java/com/hyperdata/proms/module/platform/tenant/entity/Subscription.java`:
```java
package com.hyperdata.proms.module.platform.tenant.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "subscriptions", schema = "platform")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(nullable = false)
    private String status;

    @Column(name = "current_period_start", nullable = false)
    private Instant currentPeriodStart;

    @Column(name = "current_period_end", nullable = false)
    private Instant currentPeriodEnd;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
```

Create `src/main/java/com/hyperdata/proms/module/platform/tenant/entity/Plan.java`:
```java
package com.hyperdata.proms.module.platform.tenant.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "plans", schema = "platform")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Plan {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "price_monthly", nullable = false)
    private Long priceMonthly;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
```

Create DTOs in `facade/dto/`:
`TenantDetailDto.java`:
```java
package com.hyperdata.proms.module.platform.tenant.facade.dto;

import java.util.UUID;

public record TenantDetailDto(
        UUID id,
        String slug,
        String name,
        String status,
        String timezone
) {}
```

`TenantSummaryDto.java`:
```java
package com.hyperdata.proms.module.platform.tenant.facade.dto;

import java.util.UUID;

public record TenantSummaryDto(
        UUID id,
        String slug,
        String name
) {}
```

Create Repositories:
`TenantRepository.java`:
```java
package com.hyperdata.proms.module.platform.tenant.repository;

import com.hyperdata.proms.module.platform.tenant.entity.Tenant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findBySlug(String slug);
}
```

`SubscriptionRepository.java`:
```java
package com.hyperdata.proms.module.platform.tenant.repository;

import com.hyperdata.proms.module.platform.tenant.entity.Subscription;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Optional<Subscription> findFirstByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, String status);
}
```

`PlanRepository.java`:
```java
package com.hyperdata.proms.module.platform.tenant.repository;

import com.hyperdata.proms.module.platform.tenant.entity.Plan;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, UUID> {}
```

Create Mapper `src/main/java/com/hyperdata/proms/module/platform/tenant/mapper/TenantMapper.java`:
```java
package com.hyperdata.proms.module.platform.tenant.mapper;

import com.hyperdata.proms.module.platform.tenant.entity.Tenant;
import com.hyperdata.proms.module.platform.tenant.facade.dto.TenantDetailDto;
import com.hyperdata.proms.module.platform.tenant.facade.dto.TenantSummaryDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface TenantMapper {
    TenantDetailDto toDetailDto(Tenant tenant);
    TenantSummaryDto toSummaryDto(Tenant tenant);
}
```

Create Facade Contract `src/main/java/com/hyperdata/proms/module/platform/tenant/facade/TenantFacade.java`:
```java
package com.hyperdata.proms.module.platform.tenant.facade;

import com.hyperdata.proms.module.platform.tenant.facade.dto.TenantDetailDto;
import com.hyperdata.proms.module.platform.tenant.facade.dto.TenantSummaryDto;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface TenantFacade {
    Optional<TenantDetailDto> getTenantDetail(UUID tenantId);
    Optional<TenantSummaryDto> getTenantSummary(UUID tenantId);
    Map<String, Boolean> getTenantFeatures(UUID tenantId);
}
```

Create Facade Implementation `src/main/java/com/hyperdata/proms/module/platform/tenant/facade/internal/DefaultTenantFacade.java`:
```java
package com.hyperdata.proms.module.platform.tenant.facade.internal;

import com.hyperdata.proms.module.platform.tenant.facade.TenantFacade;
import com.hyperdata.proms.module.platform.tenant.facade.dto.TenantDetailDto;
import com.hyperdata.proms.module.platform.tenant.facade.dto.TenantSummaryDto;
import com.hyperdata.proms.module.platform.tenant.mapper.TenantMapper;
import com.hyperdata.proms.module.platform.tenant.repository.PlanRepository;
import com.hyperdata.proms.module.platform.tenant.repository.SubscriptionRepository;
import com.hyperdata.proms.module.platform.tenant.repository.TenantRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultTenantFacade implements TenantFacade {

    private final TenantRepository tenantRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final TenantMapper tenantMapper;

    @Override
    public Optional<TenantDetailDto> getTenantDetail(UUID tenantId) {
        return tenantRepository.findById(tenantId).map(tenantMapper::toDetailDto);
    }

    @Override
    public Optional<TenantSummaryDto> getTenantSummary(UUID tenantId) {
        return tenantRepository.findById(tenantId).map(tenantMapper::toSummaryDto);
    }

    @Override
    public Map<String, Boolean> getTenantFeatures(UUID tenantId) {
        Map<String, Boolean> features = new HashMap<>();
        features.put("points", true);
        features.put("standup", true);
        return features;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.hyperdata.proms.module.platform.tenant.TenantFacadeTest"`
Expected: PASS

- [ ] **Step 5: Verify deliverable**

Run: `./gradlew test --tests "com.hyperdata.proms.module.platform.tenant.*"`
Expected: PASS

---

### Task 4: Module `identity.user` (Entities, DTOs, Facade, Service & Controller `GET /me`)

**Files:**
- Create: `src/main/java/com/hyperdata/proms/module/identity/user/entity/User.java`
- Create: `src/main/java/com/hyperdata/proms/module/identity/user/entity/TenantMember.java`
- Create: `src/main/java/com/hyperdata/proms/module/identity/user/facade/dto/UserAuthDto.java`
- Create: `src/main/java/com/hyperdata/proms/module/identity/user/facade/dto/UserMembershipDto.java`
- Create: `src/main/java/com/hyperdata/proms/module/identity/user/dto/UserProfileDto.java`
- Create: `src/main/java/com/hyperdata/proms/module/identity/user/dto/OtherTenantDto.java`
- Create: `src/main/java/com/hyperdata/proms/module/identity/user/dto/MeResponse.java`
- Create: `src/main/java/com/hyperdata/proms/module/identity/user/repository/UserRepository.java`
- Create: `src/main/java/com/hyperdata/proms/module/identity/user/repository/TenantMemberRepository.java`
- Create: `src/main/java/com/hyperdata/proms/module/identity/user/mapper/UserMapper.java`
- Create: `src/main/java/com/hyperdata/proms/module/identity/user/facade/UserFacade.java`
- Create: `src/main/java/com/hyperdata/proms/module/identity/user/facade/internal/DefaultUserFacade.java`
- Create: `src/main/java/com/hyperdata/proms/module/identity/user/service/UserService.java`
- Create: `src/main/java/com/hyperdata/proms/module/identity/user/controller/UserController.java`
- Test: `src/test/java/com/hyperdata/proms/module/identity/user/UserServiceTest.java`

**Interfaces:**
- Consumes: `TenantFacade`, `platform.users`, `platform.tenant_members`
- Produces: 
  - `UserFacade.findByEmailForAuth(String email): Optional<UserAuthDto>`
  - `UserFacade.resolveUserMembership(UUID userId, Optional<UUID> preferredTenantId): Optional<UserMembershipDto>`
  - `UserService.getMe(CurrentUser currentUser): MeResponse`
  - Controller: `GET /me`

- [ ] **Step 1: Write failing unit test for UserService**

Create `src/test/java/com/hyperdata/proms/module/identity/user/UserServiceTest.java`:
```java
package com.hyperdata.proms.module.identity.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.hyperdata.proms.common.security.CurrentUser;
import com.hyperdata.proms.module.identity.user.entity.TenantMember;
import com.hyperdata.proms.module.identity.user.entity.User;
import com.hyperdata.proms.module.identity.user.mapper.UserMapper;
import com.hyperdata.proms.module.identity.user.repository.TenantMemberRepository;
import com.hyperdata.proms.module.identity.user.repository.UserRepository;
import com.hyperdata.proms.module.identity.user.service.UserService;
import com.hyperdata.proms.module.platform.tenant.facade.TenantFacade;
import com.hyperdata.proms.module.platform.tenant.facade.dto.TenantDetailDto;
import com.hyperdata.proms.module.platform.tenant.facade.dto.TenantSummaryDto;
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
    private TenantFacade tenantFacade;

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

        TenantDetailDto tenantDetail = new TenantDetailDto(tenantId, "hyperdata", "Hyperdata", "active", "Asia/Jakarta");
        TenantSummaryDto otherSummary = new TenantSummaryDto(otherTenantId, "telemedis", "Telemedis");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(tenantFacade.getTenantDetail(tenantId)).willReturn(Optional.of(tenantDetail));
        given(tenantFacade.getTenantFeatures(tenantId)).willReturn(Map.of("points", true, "standup", true));
        given(tenantMemberRepository.findByUserIdAndStatus(userId, "active")).willReturn(List.of(otherMember));
        given(tenantFacade.getTenantSummary(otherTenantId)).willReturn(Optional.of(otherSummary));

        var response = userService.getMe(currentUser);

        assertThat(response.user().fullName()).isEqualTo("Fariz Keren");
        assertThat(response.tenant().slug()).isEqualTo("hyperdata");
        assertThat(response.workspaceRole()).isEqualTo("member");
        assertThat(response.features().get("points")).isTrue();
        assertThat(response.otherTenants()).hasSize(1);
        assertThat(response.otherTenants().get(0).slug()).isEqualTo("telemedis");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.hyperdata.proms.module.identity.user.UserServiceTest"`
Expected: Compilation failure (entities and services do not exist).

- [ ] **Step 3: Implement User Entities, DTOs, Repositories, Facade, Service, and Controller**

Create `src/main/java/com/hyperdata/proms/module/identity/user/entity/User.java`:
```java
package com.hyperdata.proms.module.identity.user.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "users", schema = "platform")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "idp_subject", nullable = false)
    private String idpSubject = "local";

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(nullable = false)
    private String locale = "id";

    @Column
    private String timezone;

    @Column(nullable = false)
    private String password;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "is_platform_staff", nullable = false)
    private boolean isPlatformStaff = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
```

Create `src/main/java/com/hyperdata/proms/module/identity/user/entity/TenantMember.java`:
```java
package com.hyperdata.proms.module.identity.user.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "tenant_members", schema = "platform")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class TenantMember {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String role = "member";

    @Column(nullable = false)
    private String status = "active";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
```

Create Facade DTOs in `facade/dto/`:
`UserAuthDto.java`:
```java
package com.hyperdata.proms.module.identity.user.facade.dto;

import java.util.UUID;

public record UserAuthDto(
        UUID id,
        String email,
        String password,
        String fullName,
        String locale,
        String timezone,
        boolean isActive
) {}
```

`UserMembershipDto.java`:
```java
package com.hyperdata.proms.module.identity.user.facade.dto;

import java.util.UUID;

public record UserMembershipDto(
        UUID tenantId,
        String role
) {}
```

Create Controller DTOs in `dto/`:
`UserProfileDto.java`:
```java
package com.hyperdata.proms.module.identity.user.dto;

import java.util.UUID;

public record UserProfileDto(UUID id, String fullName, String email) {}
```

`OtherTenantDto.java`:
```java
package com.hyperdata.proms.module.identity.user.dto;

public record OtherTenantDto(String slug, String name) {}
```

`MeResponse.java`:
```java
package com.hyperdata.proms.module.identity.user.dto;

import com.hyperdata.proms.module.platform.tenant.facade.dto.TenantDetailDto;
import java.util.List;
import java.util.Map;

public record MeResponse(
        UserProfileDto user,
        TenantDetailDto tenant,
        String workspaceRole,
        Map<String, Boolean> features,
        List<OtherTenantDto> otherTenants
) {}
```

Create Repositories:
`UserRepository.java`:
```java
package com.hyperdata.proms.module.identity.user.repository;

import com.hyperdata.proms.module.identity.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
}
```

`TenantMemberRepository.java`:
```java
package com.hyperdata.proms.module.identity.user.repository;

import com.hyperdata.proms.module.identity.user.entity.TenantMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantMemberRepository extends JpaRepository<TenantMember, UUID> {
    List<TenantMember> findByUserIdAndStatus(UUID userId, String status);
    Optional<TenantMember> findFirstByUserIdAndTenantIdAndStatus(UUID userId, UUID tenantId, String status);
}
```

Create Mapper `src/main/java/com/hyperdata/proms/module/identity/user/mapper/UserMapper.java`:
```java
package com.hyperdata.proms.module.identity.user.mapper;

import com.hyperdata.proms.module.identity.user.dto.UserProfileDto;
import com.hyperdata.proms.module.identity.user.entity.User;
import com.hyperdata.proms.module.identity.user.facade.dto.UserAuthDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserMapper {
    UserProfileDto toProfileDto(User user);
    UserAuthDto toAuthDto(User user);
}
```

Create Facade `UserFacade.java`:
```java
package com.hyperdata.proms.module.identity.user.facade;

import com.hyperdata.proms.module.identity.user.facade.dto.UserAuthDto;
import com.hyperdata.proms.module.identity.user.facade.dto.UserMembershipDto;
import java.util.Optional;
import java.util.UUID;

public interface UserFacade {
    Optional<UserAuthDto> findByEmailForAuth(String email);
    Optional<UserMembershipDto> resolveUserMembership(UUID userId, Optional<UUID> preferredTenantId);
}
```

Create `DefaultUserFacade.java` in `facade/internal/`:
```java
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
    public Optional<UserMembershipDto> resolveUserMembership(UUID userId, Optional<UUID> preferredTenantId) {
        if (preferredTenantId.isPresent()) {
            return tenantMemberRepository
                    .findFirstByUserIdAndTenantIdAndStatus(userId, preferredTenantId.get(), "active")
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
```

Create Service `src/main/java/com/hyperdata/proms/module/identity/user/service/UserService.java`:
```java
package com.hyperdata.proms.module.identity.user.service;

import com.hyperdata.proms.common.exception.ResourceNotFoundException;
import com.hyperdata.proms.common.security.CurrentUser;
import com.hyperdata.proms.module.identity.user.dto.MeResponse;
import com.hyperdata.proms.module.identity.user.dto.OtherTenantDto;
import com.hyperdata.proms.module.identity.user.mapper.UserMapper;
import com.hyperdata.proms.module.identity.user.repository.TenantMemberRepository;
import com.hyperdata.proms.module.identity.user.repository.UserRepository;
import com.hyperdata.proms.module.platform.tenant.facade.TenantFacade;
import com.hyperdata.proms.module.platform.tenant.facade.dto.TenantDetailDto;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final TenantMemberRepository tenantMemberRepository;
    private final TenantFacade tenantFacade;
    private final UserMapper userMapper;

    public MeResponse getMe(CurrentUser currentUser) {
        UUID userId = currentUser.userId();
        UUID currentTenantId = currentUser.activeTenantId();

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User tidak ditemukan"));

        TenantDetailDto tenantDetail = tenantFacade.getTenantDetail(currentTenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant tidak ditemukan"));

        var features = tenantFacade.getTenantFeatures(currentTenantId);

        var allMemberships = tenantMemberRepository.findByUserIdAndStatus(userId, "active");
        List<OtherTenantDto> otherTenants = new ArrayList<>();
        for (var member : allMemberships) {
            if (!member.getTenantId().equals(currentTenantId)) {
                tenantFacade.getTenantSummary(member.getTenantId()).ifPresent(summary -> {
                    otherTenants.add(new OtherTenantDto(summary.slug(), summary.name()));
                });
            }
        }

        return new MeResponse(
                userMapper.toProfileDto(user),
                tenantDetail,
                currentUser.workspaceRole(),
                features,
                otherTenants
        );
    }
}
```

Create Controller `src/main/java/com/hyperdata/proms/module/identity/user/controller/UserController.java`:
```java
package com.hyperdata.proms.module.identity.user.controller;

import com.hyperdata.proms.common.response.ApiResponse;
import com.hyperdata.proms.common.security.SecurityContextHelper;
import com.hyperdata.proms.module.identity.user.dto.MeResponse;
import com.hyperdata.proms.module.identity.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<MeResponse> getMe() {
        var currentUser = SecurityContextHelper.getRequiredCurrentUser();
        return ApiResponse.ok(userService.getMe(currentUser), "Berhasil");
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.hyperdata.proms.module.identity.user.UserServiceTest"`
Expected: PASS

- [ ] **Step 5: Verify deliverable**

Run: `./gradlew test --tests "com.hyperdata.proms.module.identity.user.*"`
Expected: PASS

---

### Task 5: Module `identity.auth` (Entity, DTOs, TokenService & AuthService)

**Files:**
- Create: `src/main/java/com/hyperdata/proms/module/identity/auth/entity/PersonalAccessToken.java`
- Create: `src/main/java/com/hyperdata/proms/module/identity/auth/dto/LoginRequest.java`
- Create: `src/main/java/com/hyperdata/proms/module/identity/auth/dto/AuthUserDto.java`
- Create: `src/main/java/com/hyperdata/proms/module/identity/auth/dto/LoginResponse.java`
- Create: `src/main/java/com/hyperdata/proms/module/identity/auth/repository/PersonalAccessTokenRepository.java`
- Create: `src/main/java/com/hyperdata/proms/module/identity/auth/service/TokenService.java`
- Create: `src/main/java/com/hyperdata/proms/module/identity/auth/service/AuthService.java`
- Test: `src/test/java/com/hyperdata/proms/module/identity/auth/TokenServiceTest.java`
- Test: `src/test/java/com/hyperdata/proms/module/identity/auth/AuthServiceTest.java`

**Interfaces:**
- Consumes: `UserFacade`, `TenantFacade`, `BCryptPasswordEncoder`, `platform.personal_access_tokens`
- Produces: 
  - `TokenService.generateToken(UUID userId, UUID tenantId): String`
  - `TokenService.verifyToken(String fullToken): Optional<PersonalAccessToken>`
  - `AuthService.login(LoginRequest request): LoginResponse`
  - `AuthService.logout(Long tokenId): void`

- [ ] **Step 1: Write failing unit test for TokenService and AuthService**

Create `src/test/java/com/hyperdata/proms/module/identity/auth/TokenServiceTest.java`:
```java
package com.hyperdata.proms.module.identity.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.hyperdata.proms.module.identity.auth.entity.PersonalAccessToken;
import com.hyperdata.proms.module.identity.auth.repository.PersonalAccessTokenRepository;
import com.hyperdata.proms.module.identity.auth.service.TokenService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private PersonalAccessTokenRepository tokenRepository;

    @InjectMocks
    private TokenService tokenService;

    @Test
    @DisplayName("GIVEN user and tenant WHEN generateToken THEN returns formatted token id|plainToken and stores sha256")
    void givenUserAndTenant_whenGenerateToken_thenReturnsFormattedToken() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        given(tokenRepository.save(any(PersonalAccessToken.class))).willAnswer(invocation -> {
            PersonalAccessToken pat = invocation.getArgument(0);
            pat.setId(12L);
            return pat;
        });

        String token = tokenService.generateToken(userId, tenantId);

        assertThat(token).startsWith("12|");
        assertThat(token.length()).isGreaterThan(20);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.hyperdata.proms.module.identity.auth.TokenServiceTest"`
Expected: Compilation failure.

- [ ] **Step 3: Implement PersonalAccessToken, DTOs, Repository, TokenService, and AuthService**

Create `src/main/java/com/hyperdata/proms/module/identity/auth/entity/PersonalAccessToken.java`:
```java
package com.hyperdata.proms.module.identity.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "personal_access_tokens", schema = "platform")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class PersonalAccessToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tokenable_type", nullable = false)
    private String tokenableType = "User";

    @Column(name = "tokenable_id", nullable = false)
    private UUID tokenableId;

    @Column(nullable = false)
    private String name = "auth-token";

    @Column(nullable = false, length = 64)
    private String token;

    @Column
    private String abilities;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();
}
```

Create `src/main/java/com/hyperdata/proms/module/identity/auth/repository/PersonalAccessTokenRepository.java`:
```java
package com.hyperdata.proms.module.identity.auth.repository;

import com.hyperdata.proms.module.identity.auth.entity.PersonalAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalAccessTokenRepository extends JpaRepository<PersonalAccessToken, Long> {}
```

Create DTOs in `com.hyperdata.proms.module.identity.auth.dto`:
`LoginRequest.java`:
```java
package com.hyperdata.proms.module.identity.auth.dto;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validator;

public record LoginRequest(String email, String password) {
    public static final Validator<LoginRequest> VALIDATOR = ValidatorBuilder.<LoginRequest>of()
            .constraint(LoginRequest::email, "email", c -> c.notBlank().message("Email wajib diisi").email().message("Format email tidak valid"))
            .constraint(LoginRequest::password, "password", c -> c.notBlank().message("Kata sandi wajib diisi"))
            .build();
}
```

`AuthUserDto.java`:
```java
package com.hyperdata.proms.module.identity.auth.dto;

import java.util.UUID;

public record AuthUserDto(
        UUID id,
        String fullName,
        String email,
        String locale,
        String timezone
) {}
```

`LoginResponse.java`:
```java
package com.hyperdata.proms.module.identity.auth.dto;

import com.hyperdata.proms.module.platform.tenant.facade.dto.TenantSummaryDto;

public record LoginResponse(
        String token,
        String tokenType,
        AuthUserDto user,
        TenantSummaryDto tenant,
        String workspaceRole
) {}
```

Create `src/main/java/com/hyperdata/proms/module/identity/auth/service/TokenService.java`:
```java
package com.hyperdata.proms.module.identity.auth.service;

import com.hyperdata.proms.module.identity.auth.entity.PersonalAccessToken;
import com.hyperdata.proms.module.identity.auth.repository.PersonalAccessTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final PersonalAccessTokenRepository tokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String generateToken(UUID userId, UUID tenantId) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String plainToken = HexFormat.of().formatHex(randomBytes);
        String hashedToken = sha256(plainToken);

        PersonalAccessToken pat = new PersonalAccessToken();
        pat.setTokenableType("User");
        pat.setTokenableId(userId);
        pat.setName("auth-session");
        pat.setToken(hashedToken);
        pat.setAbilities("tenant:" + tenantId);
        pat.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));

        PersonalAccessToken saved = tokenRepository.save(pat);
        return saved.getId() + "|" + plainToken;
    }

    @Transactional
    public Optional<PersonalAccessToken> verifyToken(String fullToken) {
        if (fullToken == null || !fullToken.contains("|")) {
            return Optional.empty();
        }
        String[] parts = fullToken.split("\\|", 2);
        try {
            Long id = Long.parseLong(parts[0]);
            String plainToken = parts[1];
            String hashedToken = sha256(plainToken);

            return tokenRepository.findById(id).filter(pat -> {
                if (pat.getExpiresAt() != null && pat.getExpiresAt().isBefore(Instant.now())) {
                    return false;
                }
                boolean matches = pat.getToken().equals(hashedToken);
                if (matches) {
                    pat.setLastUsedAt(Instant.now());
                }
                return matches;
            });
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
```

Create `src/main/java/com/hyperdata/proms/module/identity/auth/service/AuthService.java`:
```java
package com.hyperdata.proms.module.identity.auth.service;

import com.hyperdata.proms.common.exception.BusinessException;
import com.hyperdata.proms.common.exception.ResourceNotFoundException;
import com.hyperdata.proms.module.identity.auth.dto.AuthUserDto;
import com.hyperdata.proms.module.identity.auth.dto.LoginRequest;
import com.hyperdata.proms.module.identity.auth.dto.LoginResponse;
import com.hyperdata.proms.module.identity.auth.repository.PersonalAccessTokenRepository;
import com.hyperdata.proms.module.identity.user.facade.UserFacade;
import com.hyperdata.proms.module.platform.tenant.facade.TenantFacade;
import com.hyperdata.proms.module.platform.tenant.facade.dto.TenantSummaryDto;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
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
        var userAuth = userFacade.findByEmailForAuth(request.email())
                .orElseThrow(() -> new BusinessException("Email atau kata sandi tidak sesuai"));

        if (!userAuth.isActive()) {
            throw new BusinessException("Akun Anda dinonaktifkan");
        }

        if (!passwordEncoder.matches(request.password(), userAuth.password())) {
            throw new BusinessException("Email atau kata sandi tidak sesuai");
        }

        var membership = userFacade.resolveUserMembership(userAuth.id(), Optional.empty())
                .orElseThrow(() -> new BusinessException("Pengguna tidak terdaftar pada workspace aktif"));

        TenantSummaryDto tenant = tenantFacade.getTenantSummary(membership.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant tidak ditemukan"));

        String token = tokenService.generateToken(userAuth.id(), membership.tenantId());

        AuthUserDto authUser = new AuthUserDto(
                userAuth.id(),
                userAuth.fullName(),
                userAuth.email(),
                userAuth.locale(),
                userAuth.timezone()
        );

        return new LoginResponse(
                token,
                "Bearer",
                authUser,
                tenant,
                membership.role()
        );
    }

    @Transactional
    public void logout(Long tokenId) {
        if (tokenId != null) {
            tokenRepository.deleteById(tokenId);
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "com.hyperdata.proms.module.identity.auth.TokenServiceTest"`
Expected: PASS

- [ ] **Step 5: Verify deliverable**

Run: `./gradlew test --tests "com.hyperdata.proms.module.identity.auth.*"`
Expected: PASS

---

### Task 6: Security Configuration & Bearer PAT Authentication Filter

**Files:**
- Create: `src/main/java/com/hyperdata/proms/common/security/TokenAuthenticationFilter.java`
- Create: `src/main/java/com/hyperdata/proms/common/security/SecurityConfig.java`
- Create: `src/main/java/com/hyperdata/proms/common/security/RestAuthenticationEntryPoint.java`
- Test: `src/test/java/com/hyperdata/proms/common/security/TokenAuthenticationFilterTest.java`

**Interfaces:**
- Consumes: `TokenService`, `UserFacade`, `TenantMemberRepository`
- Produces: 
  - `SecurityFilterChain` with stateless session policy
  - Custom `OncePerRequestFilter` resolving `Bearer {id}|{plainToken}` into `SecurityContextHolder`
  - `BCryptPasswordEncoder` bean

- [ ] **Step 1: Write unit test for TokenAuthenticationFilter**

Create `src/test/java/com/hyperdata/proms/common/security/TokenAuthenticationFilterTest.java`:
```java
package com.hyperdata.proms.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.hyperdata.proms.module.identity.auth.entity.PersonalAccessToken;
import com.hyperdata.proms.module.identity.auth.service.TokenService;
import com.hyperdata.proms.module.identity.user.facade.UserFacade;
import com.hyperdata.proms.module.identity.user.facade.dto.UserMembershipDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class TokenAuthenticationFilterTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private UserFacade userFacade;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private TokenAuthenticationFilter filter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GIVEN valid Bearer token header WHEN doFilterInternal THEN sets CurrentUser in SecurityContext")
    void givenValidBearerToken_whenDoFilterInternal_thenSetsCurrentUser() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer 12|abc123token");

        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        PersonalAccessToken pat = new PersonalAccessToken();
        pat.setId(12L);
        pat.setTokenableId(userId);
        pat.setAbilities("tenant:" + tenantId);

        given(tokenService.verifyToken("12|abc123token")).willReturn(Optional.of(pat));
        given(userFacade.resolveUserMembership(userId, Optional.of(tenantId)))
                .willReturn(Optional.of(new UserMembershipDto(tenantId, "member")));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        CurrentUser user = (CurrentUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(user.userId()).isEqualTo(userId);
        assertThat(user.activeTenantId()).isEqualTo(tenantId);
        assertThat(user.workspaceRole()).isEqualTo("member");
        assertThat(user.tokenId()).isEqualTo(12L);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.hyperdata.proms.common.security.TokenAuthenticationFilterTest"`
Expected: Compilation failure.

- [ ] **Step 3: Implement TokenAuthenticationFilter, RestAuthenticationEntryPoint, and SecurityConfig**

Create `src/main/java/com/hyperdata/proms/common/security/TokenAuthenticationFilter.java`:
```java
package com.hyperdata.proms.common.security;

import com.hyperdata.proms.module.identity.auth.service.TokenService;
import com.hyperdata.proms.module.identity.user.facade.UserFacade;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UserFacade userFacade;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            tokenService.verifyToken(token).ifPresent(pat -> {
                UUID userId = pat.getTokenableId();
                Optional<UUID> tenantFromAbility = extractTenantId(pat.getAbilities());

                userFacade.resolveUserMembership(userId, tenantFromAbility).ifPresent(membership -> {
                    CurrentUser currentUser = new CurrentUser(
                            userId,
                            null,
                            membership.tenantId(),
                            membership.role(),
                            pat.getId()
                    );
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + membership.role().toUpperCase()));
                    var auth = new UsernamePasswordAuthenticationToken(currentUser, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });
            });
        }

        filterChain.doFilter(request, response);
    }

    private Optional<UUID> extractTenantId(String abilities) {
        if (abilities != null && abilities.startsWith("tenant:")) {
            try {
                return Optional.of(UUID.fromString(abilities.substring(7)));
            } catch (IllegalArgumentException ignored) {}
        }
        return Optional.empty();
    }
}
```

Create `src/main/java/com/hyperdata/proms/common/security/RestAuthenticationEntryPoint.java`:
```java
package com.hyperdata.proms.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperdata.proms.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Void> apiResponse = ApiResponse.error(HttpStatus.UNAUTHORIZED, "Autentikasi gagal atau token tidak valid");
        response.getOutputStream().write(objectMapper.writeValueAsBytes(apiResponse));
    }
}
```

Create `src/main/java/com/hyperdata/proms/common/security/SecurityConfig.java`:
```java
package com.hyperdata.proms.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final TokenAuthenticationFilter tokenAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.hyperdata.proms.common.security.TokenAuthenticationFilterTest"`
Expected: PASS

- [ ] **Step 5: Verify deliverable**

Run: `./gradlew test --tests "com.hyperdata.proms.common.security.*"`
Expected: PASS

---

### Task 7: Controller `AuthController.java` (`POST /auth/login`, `POST /auth/logout`)

**Files:**
- Create: `src/main/java/com/hyperdata/proms/module/identity/auth/controller/AuthController.java`
- Test: `src/test/java/com/hyperdata/proms/module/identity/auth/AuthControllerTest.java`

**Interfaces:**
- Consumes: `AuthService`, `LoginRequest`, `LoginResponse`, `CurrentUser`
- Produces: 
  - `POST /auth/login` returning `{ statusCode: 200, message: "Berhasil login", data: LoginResponse }`
  - `POST /auth/logout` returning `{ statusCode: 200, message: "Berhasil logout", data: null }`

- [ ] **Step 1: Write Controller Integration Test**

Create `src/test/java/com/hyperdata/proms/module/identity/auth/AuthControllerTest.java`:
```java
package com.hyperdata.proms.module.identity.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperdata.proms.module.identity.auth.controller.AuthController;
import com.hyperdata.proms.module.identity.auth.dto.AuthUserDto;
import com.hyperdata.proms.module.identity.auth.dto.LoginRequest;
import com.hyperdata.proms.module.identity.auth.dto.LoginResponse;
import com.hyperdata.proms.module.identity.auth.service.AuthService;
import com.hyperdata.proms.module.platform.tenant.facade.dto.TenantSummaryDto;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("GIVEN valid login request WHEN POST /auth/login THEN returns 200 with formatted envelope")
    void givenValidRequest_whenLogin_thenReturns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        LoginRequest request = new LoginRequest("fariz@hyperdata.co.id", "secret123");
        LoginResponse response = new LoginResponse(
                "12|Xk9Token",
                "Bearer",
                new AuthUserDto(userId, "Fariz Keren", "fariz@hyperdata.co.id", "id", "Asia/Jakarta"),
                new TenantSummaryDto(tenantId, "hyperdata", "Hyperdata"),
                "member"
        );

        given(authService.login(any(LoginRequest.class))).willReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Berhasil login"))
                .andExpect(jsonPath("$.data.token").value("12|Xk9Token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.fullName").value("Fariz Keren"))
                .andExpect(jsonPath("$.data.tenant.slug").value("hyperdata"))
                .andExpect(jsonPath("$.data.workspaceRole").value("member"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.hyperdata.proms.module.identity.auth.AuthControllerTest"`
Expected: Compilation failure.

- [ ] **Step 3: Implement AuthController**

Create `src/main/java/com/hyperdata/proms/module/identity/auth/controller/AuthController.java`:
```java
package com.hyperdata.proms.module.identity.auth.controller;

import com.hyperdata.proms.common.exception.ValidationException;
import com.hyperdata.proms.common.response.ApiResponse;
import com.hyperdata.proms.common.security.SecurityContextHelper;
import com.hyperdata.proms.module.identity.auth.dto.LoginRequest;
import com.hyperdata.proms.module.identity.auth.dto.LoginResponse;
import com.hyperdata.proms.module.identity.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginRequest.VALIDATOR.validate(request).throwIfInvalid(violations ->
                new ValidationException("Validasi gagal", violations)
        );
        LoginResponse response = authService.login(request);
        return ApiResponse.ok(response, "Berhasil login");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        SecurityContextHelper.getCurrentUser().ifPresent(user -> {
            authService.logout(user.tokenId());
        });
        return ApiResponse.ok(null, "Berhasil logout");
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.hyperdata.proms.module.identity.auth.AuthControllerTest"`
Expected: PASS

- [ ] **Step 5: Verify deliverable**

Run: `./gradlew test --tests "com.hyperdata.proms.module.identity.auth.*"`
Expected: PASS

---

### Task 8: Architecture Guardrails, Spotless Formatting & Full Verification

**Files:**
- Modify: `src/test/java/com/hyperdata/proms/architecture/ArchitectureTest.java` (if needed for package rules)

**Interfaces:**
- Consumes: All newly created packages and classes
- Produces: Spotless formatted codebase and ArchUnit clean assertion

- [ ] **Step 1: Run Spotless code formatter**

Run: `./gradlew spotlessApply`
Expected: BUILD SUCCESSFUL (all files formatted with palantirJavaFormat).

- [ ] **Step 2: Run Architecture Tests**

Run: `./gradlew test --tests "com.hyperdata.proms.architecture.ArchitectureTest"`
Expected: BUILD SUCCESSFUL (verifies all modular monolith boundaries, controller restrictions, and zero JPA entity leakage).

- [ ] **Step 3: Run targeted tests for all affected modules**

Run: `./gradlew test --tests "com.hyperdata.proms.module.identity.*" --tests "com.hyperdata.proms.module.platform.tenant.*"`
Expected: All tests PASS.

- [ ] **Step 4: Full project build & verification**

Run: `./gradlew check`
Expected: All tests pass, spotless check passes, and build succeeds.
