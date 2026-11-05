package com.hyperdata.proms.common.security;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityContextHelper {

    private SecurityContextHelper() {
    }

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
