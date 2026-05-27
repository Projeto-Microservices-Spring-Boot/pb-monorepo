package com.edu.infnet.pb.users.presentation.controllers.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.edu.infnet.pb.users.presentation.dtos.auth.LoginRequestDto;
import com.edu.infnet.pb.users.presentation.dtos.auth.LoginResponseDto;

@RestController
@RequiredArgsConstructor
public class AuthController {
  private final JwtEncoder jwtEncoder;

  @PostMapping("/login")
  public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto loginRequest) {
    
  }

}
