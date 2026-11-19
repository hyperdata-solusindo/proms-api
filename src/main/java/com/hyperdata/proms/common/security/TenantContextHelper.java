package com.hyperdata.proms.common.security;

import com.hyperdata.proms.common.exception.TenantRequiredException;
import java.util.Optional;
import java.util.UUID;

/**
 * Utility helper untuk mengakses konteks tenant aktif dari SecurityContext.
 */
public final class TenantContextHelper {

    private TenantContextHelper() {
    }

    /**
     * Mengambil activeTenantId dari CurrentUser yang sedang login jika ada.
     */
    public static Optional<UUID> getActiveTenantId() {
        return SecurityContextHelper.getCurrentUser()
            .map(CurrentUser::activeTenantId);
    }

    /**
     * Mengambil activeTenantId secara fail-closed (throw TenantRequiredException / 403 jika null/tidak ada).
     */
    public static UUID requireActiveTenantId() {
        return getActiveTenantId()
            .orElseThrow(TenantRequiredException::new);
    }

    /**
     * Mengambil workspace role user aktif di tenant saat ini (default: "member").
     */
    public static String getWorkspaceRole() {
        return SecurityContextHelper.getCurrentUser()
            .map(CurrentUser::workspaceRole)
            .orElse("member");
    }

    /**
     * Mengambil userId user aktif jika ada.
     */
    public static Optional<UUID> getActiveUserId() {
        return SecurityContextHelper.getCurrentUser()
            .map(CurrentUser::userId);
    }
}
