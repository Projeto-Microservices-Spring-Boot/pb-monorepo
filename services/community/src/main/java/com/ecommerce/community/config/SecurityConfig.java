package com.ecommerce.community.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.annotation.Value;

import java.security.interfaces.RSAPublicKey;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * SecurityConfig do community-service.
 *
 * Sobrescreve o SecurityConfig da biblioteca shared (@Primary) para aplicar
 * as regras de autorização específicas deste serviço:
 *   - GET /posts/**       → público (sem autenticação)
 *   - POST /posts         → SELLER ou ADMIN
 *   - POST /comments      → qualquer autenticado
 *   - PUT/DELETE /**      → qualquer autenticado (ownership verificado no service)
 *   - /actuator/**        → público
 *
 * O JwtDecoder usa a mesma chave RSA pública da shared (jwt.public.key),
 * garantindo que todos os serviços validem o mesmo token emitido pelo
 * users-service e assinado com a chave privada RSA.
 *
 * O Kong valida o JWT na borda antes de repassar para este serviço.
 * A validação aqui é uma segunda camada de defesa (defense in depth).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Chave pública RSA para validar a assinatura do JWT.
     * Deve ser a mesma chave pública do par RSA usado pelo users-service
     * para assinar os tokens.
     *
     * Configurar via variável de ambiente JWT_PUBLIC_KEY ou no application.yml:
     *   jwt.public.key=classpath:public.pem
     */
    @Value("${jwt.public.key}")
    private RSAPublicKey rsaPublicKey;

    @Bean
    @Primary
    public SecurityFilterChain communitySecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Infra
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Feed e leitura de posts — público
                .requestMatchers(HttpMethod.GET, "/posts/**").permitAll()
                // Criação de post — apenas SELLER e ADMIN
                .requestMatchers(HttpMethod.POST, "/posts").hasAnyRole("SELLER", "ADMIN")
                // Comentários e demais operações — qualquer autenticado
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );

        return http.build();
    }

    /**
     * JwtDecoder usando a chave pública RSA da shared library.
     * Marcado como @Primary para sobrescrever o bean da shared.
     */
    @Bean
    @Primary
    public JwtDecoder communityJwtDecoder() {
        return NimbusJwtDecoder.withPublicKey(rsaPublicKey).build();
    }

    /**
     * Converte o claim "role" do JWT em uma GrantedAuthority do Spring Security
     * no formato ROLE_<VALOR> (ex: role=SELLER → ROLE_SELLER).
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
        return converter;
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        String role = jwt.getClaimAsString("role");
        if (role == null || role.isBlank()) {
            return Stream.<GrantedAuthority>empty().collect(Collectors.toList());
        }
        return Stream.of(new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    }
}
