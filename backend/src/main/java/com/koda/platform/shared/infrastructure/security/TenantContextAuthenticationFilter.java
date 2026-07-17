package com.koda.platform.shared.infrastructure.security;

import com.koda.platform.shared.application.tenant.TenantContext;
import com.koda.platform.shared.infrastructure.tenant.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TenantContextAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (isRealAuthentication(authentication) && authentication.getPrincipal() instanceof TenantAwarePrincipal principal) {
                TenantContextHolder.set(new TenantContext(
                    principal.tenantId(),
                    principal.userId(),
                    principal.roles(),
                    principal.permissions(),
                    principal.platformAdmin()
                ));
            }

            if (requiresTenantContext(request) && TenantContextHolder.get().isEmpty() && isRealAuthentication(authentication)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tenant context is required");
                return;
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private boolean isRealAuthentication(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private boolean requiresTenantContext(HttpServletRequest request) {
        String path = request.getRequestURI();
        return (path.equals("/api") || path.startsWith("/api/")) && !isTenantNeutralApiPath(path);
    }

    private boolean isTenantNeutralApiPath(String path) {
        return path.startsWith("/api/v1/auth") || path.startsWith("/api/v1/platform");
    }
}
