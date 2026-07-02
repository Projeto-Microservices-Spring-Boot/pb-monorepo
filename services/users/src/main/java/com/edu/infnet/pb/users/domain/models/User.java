package com.edu.infnet.pb.users.domain.models;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.edu.infnet.pb.users.domain.enums.Roles;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_email", columnList = "email")
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  private String name;

  @Column(unique = true)
  private String email;

  private String password;

  @Enumerated(EnumType.STRING)
  private Roles roles;

  @Column(nullable = true)
  private String refreshToken;

  @Column(name = "refresh_token_expires_in", nullable = true)
  private Instant refreshTokenExpiresIn;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone default now()")
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = true, columnDefinition = "timestamp with time zone default now()")
  private Instant updatedAt;

  public Boolean ComparePassword(String password, PasswordEncoder passwordEncoder) {
    return passwordEncoder.matches(password, this.password);
  }

  public String generateRefreshToken() {
    var rawBytes = new byte[32];
    new SecureRandom().nextBytes(rawBytes);
    return HexFormat.of().formatHex(rawBytes);
  }
}
