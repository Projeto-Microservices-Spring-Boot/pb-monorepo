package com.edu.infnet.pb.users.domain.models;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.edu.infnet.pb.users.domain.enums.Roles;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
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

  public Boolean ComparePassword(String password, PasswordEncoder passwordEncoder) {
    return passwordEncoder.matches(password, this.password);
  }

}
