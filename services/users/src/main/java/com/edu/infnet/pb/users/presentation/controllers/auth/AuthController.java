package com.edu.infnet.pb.users.presentation.controllers.auth;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.edu.infnet.pb.users.application.services.auth.AuthService;
import com.edu.infnet.pb.users.docs.AuthControllerDocs;
import com.edu.infnet.pb.users.presentation.dtos.auth.LoginRequestDto;
import com.edu.infnet.pb.users.presentation.dtos.auth.LoginResponseDto;
import com.edu.infnet.pb.users.presentation.dtos.auth.RegisterRequestDto;
import com.edu.infnet.pb.users.presentation.dtos.auth.RegisterResponseDto;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {
  private final AuthService service;

  @PostMapping("/register")
  public ResponseEntity<RegisterResponseDto> register(@RequestBody RegisterRequestDto body) {
    var result = service.register(body);
    return ResponseEntity.ok(result);
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto body) {
    var result = service.login(body);
    return ResponseEntity.ok(result);
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt) {
    UUID userId = UUID.fromString(jwt.getSubject());
    service.Logout(userId);
    return ResponseEntity.noContent().build();
  }

  // @PostMapping("/refresh")
  // public ResponseEntity<LoginResponseDto> refresh(@AuthenticationPrincipal Jwt
  // jwt,
  // @RequestParam() String refreshToken) {
  // UUID userId = UUID.fromString(jwt.getSubject());
  // var result = service.refresh(refreshToken, userId);
  // return ResponseEntity.ok(result);
  // }
}
