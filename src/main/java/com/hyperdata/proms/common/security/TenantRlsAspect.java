package com.hyperdata.proms.common.security;

import com.hyperdata.proms.common.exception.TenantRequiredException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.PreparedStatement;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Aspect untuk menegakkan fail-closed isolasi tenant dan menginjeksi session variable
 * PostgreSQL RLS (SET LOCAL app.tenant_id, app.workspace_role, app.user_id) pada setiap transaksi.
 */
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class TenantRlsAspect {

    private static final Logger log = LoggerFactory.getLogger(TenantRlsAspect.class);
    private static final String RLS_APPLIED_FLAG = "PROMS_RLS_APPLIED_TX";

    private static final String SQL_SET_TENANT_CONFIG = "SELECT set_config('app.tenant_id', ?, true), " +
        "       set_config('app.workspace_role', ?, true), " +
        "       set_config('app.user_id', ?, true)";

    @PersistenceContext
    private EntityManager entityManager;

    public TenantRlsAspect() {
    }

    public TenantRlsAspect(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Pointcut("execution(public * com.hyperdata.proms.module..service..*(..)) || " +
        "execution(public * com.hyperdata.proms.module..facade.internal..*(..))")
    public void businessOperations() {
    }

    @Pointcut("@annotation(com.hyperdata.proms.common.security.TenantScoped) || " +
        "@within(com.hyperdata.proms.common.security.TenantScoped)")
    public void tenantScopedOperations() {
    }

    @Around("businessOperations() || tenantScopedOperations()")
    public Object applyTenantRls(ProceedingJoinPoint joinPoint) throws Throwable {
        boolean explicitTenantScoped = isTenantScoped(joinPoint);
        var currentUserOpt = SecurityContextHelper.getCurrentUser();
        UUID tenantId = currentUserOpt.map(CurrentUser::activeTenantId).orElse(null);

        // 1. Fail-Closed Validation jika method / class ditandai @TenantScoped
        if (explicitTenantScoped && tenantId == null) {
            log.warn("Fail-Closed: Akses ke [{}] ditolak karena activeTenantId kosong",
                joinPoint.getSignature().toShortString());
            throw new TenantRequiredException();
        }

        // 2. Injeksi session settings PostgreSQL jika transaksi aktif
        if (TransactionSynchronizationManager.isActualTransactionActive() && entityManager != null) {
            applyPostgresRlsSessionSettings(tenantId, currentUserOpt.orElse(null));
        }

        return joinPoint.proceed();
    }

    private void applyPostgresRlsSessionSettings(UUID tenantId, CurrentUser currentUser) {
        if (TransactionSynchronizationManager.hasResource(RLS_APPLIED_FLAG)) {
            return;
        }

        String tenantIdStr = tenantId != null ? tenantId.toString() : "";
        String workspaceRole = currentUser != null && currentUser.workspaceRole() != null
            ? currentUser.workspaceRole()
            : "member";
        String userIdStr = currentUser != null && currentUser.userId() != null
            ? currentUser.userId().toString()
            : "";

        try {
            Session session = entityManager.unwrap(Session.class);
            session.doWork(connection -> {
                String dbProduct = connection.getMetaData().getDatabaseProductName();
                // Hanya eksekusi set_config jika target database adalah PostgreSQL
                if (dbProduct != null && dbProduct.toLowerCase().contains("postgres")) {
                    try (PreparedStatement ps = connection.prepareStatement(SQL_SET_TENANT_CONFIG)) {
                        ps.setString(1, tenantIdStr);
                        ps.setString(2, workspaceRole);
                        ps.setString(3, userIdStr);
                        ps.execute();
                    }
                    log.debug("RLS session configured: tenantId={}, role={}, userId={}",
                        tenantIdStr, workspaceRole, userIdStr);
                }
            });

            TransactionSynchronizationManager.bindResource(RLS_APPLIED_FLAG, Boolean.TRUE);
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

                    @Override
                    public void afterCompletion(int status) {
                        TransactionSynchronizationManager.unbindResourceIfPossible(RLS_APPLIED_FLAG);
                    }
                });
            }
        } catch (Exception ex) {
            log.error("Gagal menginjeksi session setting RLS PostgreSQL: {}", ex.getMessage());
            throw new IllegalStateException("Gagal menginisialisasi isolasi tenant pada koneksi database", ex);
        }
    }

    private boolean isTenantScoped(ProceedingJoinPoint pjp) {
        Class<?> targetClass = pjp.getTarget() != null ? pjp.getTarget().getClass() : null;
        if (targetClass != null && targetClass.isAnnotationPresent(TenantScoped.class)) {
            return true;
        }

        if (pjp.getSignature() instanceof MethodSignature methodSignature) {
            var method = methodSignature.getMethod();
            if (method.isAnnotationPresent(TenantScoped.class)) {
                return true;
            }
            if (targetClass != null) {
                try {
                    var specificMethod = targetClass.getMethod(method.getName(), method.getParameterTypes());
                    return specificMethod.isAnnotationPresent(TenantScoped.class);
                } catch (NoSuchMethodException ignored) {
                    // Fall back to method on interface/super
                }
            }
        }
        return false;
    }
}
