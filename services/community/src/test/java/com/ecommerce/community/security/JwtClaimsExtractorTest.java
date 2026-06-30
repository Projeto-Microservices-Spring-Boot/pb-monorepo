package com.ecommerce.community.security;

import com.ecommerce.community.exception.UnauthorizedException;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtClaimsExtractorTest {

    private final JwtClaimsExtractor extractor = new JwtClaimsExtractor();

    private Jwt buildJwt(String subject, String name, String role, String issuer,
                          Instant issuedAt, Instant expiresAt) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");

        Map<String, Object> claims = new HashMap<>();
        if (subject != null) claims.put("sub", subject);
        if (name != null) claims.put("name", name);
        if (role != null) claims.put("role", role);
        if (issuer != null) claims.put("iss", issuer);
        if (issuedAt != null) claims.put("iat", issuedAt);
        if (expiresAt != null) claims.put("exp", expiresAt);

        return new Jwt("token-value", issuedAt, expiresAt, headers, claims);
    }

    @Test
    void shouldExtractValidUserContext() {
        Instant now = Instant.now();
        Jwt jwt = buildJwt("user-12345", "João Silva", "SELLER", "users-service",
                now.minusSeconds(60), now.plusSeconds(3600));

        UserContext context = extractor.extractUserContext(jwt);

        assertThat(context.getUserId()).isEqualTo("user-12345");
        assertThat(context.getName()).isEqualTo("João Silva");
        assertThat(context.getRole()).isEqualTo("SELLER");
    }

    @Test
    void shouldReject_ExpiredToken() {
        Instant now = Instant.now();
        Jwt expiredToken = buildJwt("user-1", "John", "BUYER", "users-service",
                now.minusSeconds(7200), now.minusSeconds(3600));

        assertThatThrownBy(() -> extractor.extractUserContext(expiredToken))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void issuerClaimIsIgnored_authenticityComesFromRsaSignature() {
        Instant now = Instant.now();
        Jwt anyIssuerToken = buildJwt("user-1", "John", "BUYER", "any-issuer-value",
                now.minusSeconds(60), now.plusSeconds(3600));

        UserContext context = extractor.extractUserContext(anyIssuerToken);

        assertThat(context.getUserId()).isEqualTo("user-1");
        assertThat(context.getName()).isEqualTo("John");
        assertThat(context.getRole()).isEqualTo("BUYER");
    }

    @Test
    void shouldReject_IssuedAtInFuture() {
        Instant now = Instant.now();
        Jwt futureToken = buildJwt("user-1", "John", "BUYER", "users-service",
                now.plusSeconds(60), now.plusSeconds(3600));

        assertThatThrownBy(() -> extractor.extractUserContext(futureToken))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void shouldReject_MissingClaims() {
        Instant now = Instant.now();
        Jwt missingRole = buildJwt("user-1", "John", null, "users-service",
                now.minusSeconds(60), now.plusSeconds(3600));

        assertThatThrownBy(() -> extractor.extractUserContext(missingRole))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Property
    void claimExtractionRoundTripPreservesValues(
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String userId,
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String name,
            @ForAll("validRoles") String role) {

        Instant now = Instant.now();
        Jwt jwt = buildJwt(userId, name, role, "users-service",
                now.minusSeconds(60), now.plusSeconds(3600));

        UserContext first = extractor.extractUserContext(jwt);
        UserContext second = extractor.extractUserContext(jwt);

        assertThat(first.getUserId()).isEqualTo(second.getUserId()).isEqualTo(userId);
        assertThat(first.getName()).isEqualTo(second.getName()).isEqualTo(name);
        assertThat(first.getRole()).isEqualTo(second.getRole()).isEqualTo(role);
    }

    @net.jqwik.api.Provide
    net.jqwik.api.Arbitrary<String> validRoles() {
        return net.jqwik.api.Arbitraries.of("ADMIN", "SELLER", "BUYER");
    }
}
