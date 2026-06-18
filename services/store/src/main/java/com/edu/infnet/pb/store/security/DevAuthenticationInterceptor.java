package com.edu.infnet.pb.store.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor de autenticação para desenvolvimento.
 * Em produção, substituir por validação JWT real.
 *
 * Espera os headers:
 * - X-User-Id: ID externo do usuário
 * - X-User-Name: Nome do usuário
 * - X-User-Email: Email do usuário
 * - X-User-Role: Role (ADMIN, USER)
 */
@Component
public class DevAuthenticationInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(DevAuthenticationInterceptor.class);

    public static final String USER_PRINCIPAL_ATTR = "USER_PRINCIPAL";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userId = request.getHeader("X-User-Id");
        String userName = request.getHeader("X-User-Name");
        String userEmail = request.getHeader("X-User-Email");
        String userRole = request.getHeader("X-User-Role");

        if (userId == null || userId.isBlank()) {
            // Rotas públicas não precisam de autenticação
            String path = request.getRequestURI();
            if (path.startsWith("/api/publico") || path.startsWith("/actuator") || path.equals("/health")) {
                return true;
            }

            log.warn("Requisição sem header X-User-Id para path: {}", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        UserPrincipal principal = new UserPrincipal(
                userId,
                userName != null ? userName : "Usuário",
                userEmail != null ? userEmail : "",
                userRole != null ? userRole : "USER"
        );

        request.setAttribute(USER_PRINCIPAL_ATTR, principal);
        log.debug("Usuário autenticado: {} ({})", principal.externalId(), principal.role());
        return true;
    }
}
