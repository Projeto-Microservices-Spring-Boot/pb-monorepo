package com.edu.infnet.pb.store.security;

/**
 * Representa o usuário autenticado extraído do header/token.
 */
public record UserPrincipal(
        String externalId,
        String nome,
        String email,
        String role
) {
    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
}
