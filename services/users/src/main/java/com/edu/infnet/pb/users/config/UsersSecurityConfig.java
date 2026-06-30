//package com.edu.infnet.pb.users.config;
//
//import java.security.interfaces.RSAPrivateKey;
//import java.security.interfaces.RSAPublicKey;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.oauth2.jwt.JwtEncoder;
//import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
//
//import com.nimbusds.jose.jwk.JWK;
//import com.nimbusds.jose.jwk.JWKSet;
//import com.nimbusds.jose.jwk.RSAKey;
//import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
//
//@Configuration
//public class UsersSecurityConfig {
//  @Value("${jwt.public.key}")
//  private RSAPublicKey rsaPublicKey;
//
//  @Value("${jwt.private.key}")
//  private RSAPrivateKey rsaPrivateKey;
//
//  @Bean
//  public JwtEncoder jwtEncoder() {
//    JWK jwk = new RSAKey.Builder(this.rsaPublicKey).privateKey(rsaPrivateKey).build();
//    var jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
//    return new NimbusJwtEncoder(jwks);
//  }
//
//}
