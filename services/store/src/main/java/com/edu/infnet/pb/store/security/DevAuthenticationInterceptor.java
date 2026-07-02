package com.edu.infnet.pb.store.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class DevAuthenticationInterceptor implements HandlerInterceptor {

    private static final String HEADER_EXTERNAL_ID = "X-User-Id";
    private static final String HEADER_NAME = "X-User-Name";
    private static final String HEADER_EMAIL = "X-User-Email";
    private static final String HEADER_ROLE = "X-User-Role";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String externalId = request.getHeader(HEADER_EXTERNAL_ID);
        String nome = request.getHeader(HEADER_NAME);
        String email = request.getHeader(HEADER_EMAIL);
        String role = request.getHeader(HEADER_ROLE);

        if (externalId != null && !externalId.isBlank()) {
            UserPrincipal principal = new UserPrincipal(
                    externalId,
                    nome != null ? nome : "Usuário Dev",
                    email != null ? email : "dev@local",
                    role != null ? role : "CLIENTE"
            );

            UserContextHolder.set(principal);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        UserContextHolder.clear();
    }
}

class UserContextHolder {

    private static final ThreadLocal<UserPrincipal> USER_CONTEXT = new ThreadLocal<>();

    private UserContextHolder() {
    }

    public static void set(UserPrincipal principal) {
        USER_CONTEXT.set(principal);
    }

    public static UserPrincipal get() {
        return USER_CONTEXT.get();
    }

    public static void clear() {
        USER_CONTEXT.remove();
    }
}
