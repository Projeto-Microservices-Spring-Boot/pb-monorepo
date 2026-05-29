package com.edu.infnet.pb.users.presentation.controllers.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.edu.infnet.pb.users.application.services.auth.AuthService;
import com.edu.infnet.pb.users.presentation.dtos.auth.LoginRequestDto;
import com.edu.infnet.pb.users.presentation.dtos.auth.LoginResponseDto;
import com.edu.infnet.pb.users.presentation.dtos.auth.RegisterRequestDto;
import com.edu.infnet.pb.users.presentation.dtos.auth.RegisterResponseDto;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
public class AuthController {
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

}
