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
import org.springframework.util.StringUtils;
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

        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String tokenValue = authHeader.substring(7).trim();
            int pipeIndex = tokenValue.indexOf('|');

            if (pipeIndex > 0) {
                try {
                    Long tokenId = Long.valueOf(tokenValue.substring(0, pipeIndex));
                    String plainToken = tokenValue.substring(pipeIndex + 1);

                    var patOpt = tokenService.verifyToken(tokenId, plainToken);
                    if (patOpt.isPresent()) {
                        var pat = patOpt.get();
                        UUID userId = pat.getTokenableId();

                        UUID tenantId = null;
                        if (pat.getAbilities() != null && pat.getAbilities().startsWith("tenant:")) {
                            tenantId = UUID.fromString(pat.getAbilities().substring(7));
                        }

                        var userOpt = userFacade.findByIdForAuth(userId);
                        if (userOpt.isPresent() && userOpt.get().isActive()) {
                            var user = userOpt.get();
                            String role = "member";

                            if (tenantId != null) {
                                var membershipOpt = userFacade.resolveUserMembership(userId, Optional.of(tenantId));
                                if (membershipOpt.isPresent()) {
                                    role = membershipOpt.get().role();
                                }
                            }

                            CurrentUser currentUser = new CurrentUser(user.id(), user.email(), tenantId, role,
                                pat.getId());
                            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                            var auth = new UsernamePasswordAuthenticationToken(currentUser, null, authorities);
                            SecurityContextHolder.getContext().setAuthentication(auth);

                            tokenService.recordTokenUsage(pat.getId());
                        }
                    }
                } catch (Exception ignored) {
                    // Invalid token format or parsing error; continue filter chain without authentication
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
