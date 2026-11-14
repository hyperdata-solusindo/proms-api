package com.hyperdata.proms.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hyperdata.proms.common.exception.TenantRequiredException;
import jakarta.persistence.EntityManager;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class TenantRlsAspectTest {

    @Mock
    private EntityManager entityManager;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
        TransactionSynchronizationManager.unbindResourceIfPossible("PROMS_RLS_APPLIED_TX");
    }

    @TenantScoped
    static class SampleTenantScopedService {

        public String executeOperation() {
            return "SUCCESS";
        }
    }

    static class SampleMethodScopedService {

        @TenantScoped
        public String executeTenantMethod() {
            return "SUCCESS";
        }

        public String executePublicMethod() {
            return "PUBLIC";
        }
    }

    @Test
    @DisplayName("GIVEN class @TenantScoped AND null activeTenantId WHEN applyTenantRls THEN throws TenantRequiredException")
    void givenClassTenantScoped_andNullTenant_whenApplyTenantRls_thenThrowsTenantRequiredException() throws Throwable {
        TenantRlsAspect aspect = new TenantRlsAspect(entityManager);

        SampleTenantScopedService target = new SampleTenantScopedService();
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MethodSignature sig = mock(MethodSignature.class);

        when(pjp.getTarget()).thenReturn(target);
        when(pjp.getSignature()).thenReturn(sig);
        when(sig.toShortString()).thenReturn("SampleTenantScopedService#executeOperation");

        // Authenticated user with null tenantId
        CurrentUser user = new CurrentUser(UUID.randomUUID(), "test@hyperdata.co.id", null, null, 1L);
        var auth = new UsernamePasswordAuthenticationToken(user, null,
            List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(() -> aspect.applyTenantRls(pjp))
            .isInstanceOf(TenantRequiredException.class);
    }

    @Test
    @DisplayName("GIVEN method @TenantScoped AND null activeTenantId WHEN applyTenantRls THEN throws TenantRequiredException")
    void givenMethodTenantScoped_andNullTenant_whenApplyTenantRls_thenThrowsTenantRequiredException() throws Throwable {
        TenantRlsAspect aspect = new TenantRlsAspect(entityManager);

        SampleMethodScopedService target = new SampleMethodScopedService();
        Method method = SampleMethodScopedService.class.getMethod("executeTenantMethod");

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MethodSignature sig = mock(MethodSignature.class);

        when(pjp.getTarget()).thenReturn(target);
        when(pjp.getSignature()).thenReturn(sig);
        when(sig.getMethod()).thenReturn(method);
        when(sig.toShortString()).thenReturn("SampleMethodScopedService#executeTenantMethod");

        CurrentUser user = new CurrentUser(UUID.randomUUID(), "test@hyperdata.co.id", null, null, 1L);
        var auth = new UsernamePasswordAuthenticationToken(user, null,
            List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(() -> aspect.applyTenantRls(pjp))
            .isInstanceOf(TenantRequiredException.class);
    }

    @Test
    @DisplayName("GIVEN class @TenantScoped AND valid activeTenantId WHEN applyTenantRls THEN proceeds successfully")
    void givenClassTenantScoped_andValidTenant_whenApplyTenantRls_thenProceeds() throws Throwable {
        TenantRlsAspect aspect = new TenantRlsAspect(entityManager);

        SampleTenantScopedService target = new SampleTenantScopedService();
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);

        when(pjp.getTarget()).thenReturn(target);
        when(pjp.proceed()).thenReturn("SUCCESS");

        UUID tenantId = UUID.randomUUID();
        CurrentUser user = new CurrentUser(UUID.randomUUID(), "test@hyperdata.co.id", tenantId, "owner", 1L);
        var auth = new UsernamePasswordAuthenticationToken(user, null,
            List.of(new SimpleGrantedAuthority("ROLE_OWNER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        Object result = aspect.applyTenantRls(pjp);
        assertThat(result).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("GIVEN active transaction on PostgreSQL WHEN applyTenantRls THEN injects session variables")
    void givenActiveTransactionOnPostgreSQL_whenApplyTenantRls_thenInjectsSessionVariables() throws Throwable {
        TenantRlsAspect aspect = new TenantRlsAspect(entityManager);

        SampleMethodScopedService target = new SampleMethodScopedService();
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);

        when(pjp.getTarget()).thenReturn(target);
        when(pjp.proceed()).thenReturn("SUCCESS");

        // Setup Spring transaction synchronization
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        // Setup Postgres mocks
        Session session = mock(Session.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(connection.getMetaData()).thenReturn(metaData);
        when(connection.prepareStatement(anyString())).thenReturn(ps);

        doAnswer(invocation -> {
            Work work = invocation.getArgument(0);
            work.execute(connection);
            return null;
        }).when(session).doWork(any(Work.class));

        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CurrentUser user = new CurrentUser(userId, "test@hyperdata.co.id", tenantId, "finance", 1L);
        var auth = new UsernamePasswordAuthenticationToken(user, null,
            List.of(new SimpleGrantedAuthority("ROLE_FINANCE")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        Object result = aspect.applyTenantRls(pjp);
        assertThat(result).isEqualTo("SUCCESS");

        // Verify set_config was called with tenantId, role, and userId
        verify(ps).setString(1, tenantId.toString());
        verify(ps).setString(2, "finance");
        verify(ps).setString(3, userId.toString());
        verify(ps).execute();

        // Second call in same transaction should not re-execute query
        aspect.applyTenantRls(pjp);
        verify(ps, times(1)).execute();
    }
}
