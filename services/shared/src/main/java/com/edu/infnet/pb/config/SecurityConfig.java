package com.edu.infnet.pb.config;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/**").permitAll()
            .requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
            .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
            .anyRequest().authenticated())
        .httpBasic(Customizer.withDefaults()).csrf(csrf -> csrf.disable())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/actuator/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/register/seller").permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                    .anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults()).csrf(csrf -> csrf.disable())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

    return http.build();
  }

  @Bean
  public JwtEncoder jwtEncoder() throws Exception {
    var publicKey = loadPublicKey();
    var privateKey = loadPrivateKey();
    JWK jwk = new RSAKey.Builder(publicKey).privateKey(privateKey).build();
    return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
  }

  @Bean
  public JwtDecoder jwtDecoder() throws Exception {
    return NimbusJwtDecoder.withPublicKey(loadPublicKey()).build();
  }

  /* Le chave pública */
  private RSAPublicKey loadPublicKey() throws Exception {
    var encoded = decodePem("app.key.pub");
    var factory = KeyFactory.getInstance("RSA");
    return (RSAPublicKey) factory.generatePublic(new X509EncodedKeySpec(encoded));
  }

  /* Le chave pública */
  private RSAPrivateKey loadPrivateKey() throws Exception {
    var encoded = decodePem("app.key");
    var factory = KeyFactory.getInstance("RSA");
    return (RSAPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(encoded));
  }

  /*
   * Lê o arquivo PEM do classpath, remove os headers e espacos
   * e decodifica o Base64 restante para bytes
   */
  private byte[] decodePem(String path) throws Exception {
    var resource = new ClassPathResource(path);
    try (var in = resource.getInputStream()) {
      var pem = new String(in.readAllBytes())
          .replaceAll("-----BEGIN [A-Z ]+-----", "")
          .replaceAll("-----END [A-Z ]+-----", "")
          .replaceAll("\\s", "");
      return Base64.getDecoder().decode(pem);
    }
  }
}
