package com.ecommerce.community.security;

import com.ecommerce.community.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.time.Instant;


@Component
public class JwtClaimsExtractor {

    public UserContext extractUserContext(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new UnauthorizedException("Authentication is not a valid JWT token");
        }
        return extractUserContext(jwtAuth.getToken());
    }

    public UserContext extractUserContext(Jwt jwt) {
        validateTemporalClaims(jwt);

        String userId = jwt.getSubject();
        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("JWT token is missing required 'sub' claim");
        }

        String name = jwt.getClaimAsString("name");
        if (name == null || name.isBlank()) {
            throw new UnauthorizedException("JWT token is missing required 'name' claim");
        }

        String role = jwt.getClaimAsString("role");
        if (role == null || role.isBlank()) {
            throw new UnauthorizedException("JWT token is missing required 'role' claim");
        }

        return new UserContext(userId, name, role);
    }

    private void validateTemporalClaims(Jwt jwt) {
        Instant issuedAt = jwt.getIssuedAt();
        Instant expiresAt = jwt.getExpiresAt();
        Instant now = Instant.now();

        if (issuedAt == null || expiresAt == null) {
            throw new UnauthorizedException("JWT token is missing required temporal claims");
        }
        if (issuedAt.isAfter(now)) {
            throw new UnauthorizedException("JWT token issuedAt is in the future");
        }
        if (!expiresAt.isAfter(now)) {
            throw new UnauthorizedException("JWT token has expired");
        }
    }
}
