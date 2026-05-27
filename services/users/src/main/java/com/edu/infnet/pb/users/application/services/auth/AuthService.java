package com.edu.infnet.pb.users.application.services.auth;

import org.springframework.stereotype.Service;

import com.edu.infnet.pb.users.infrastructure.repositories.AuthRepository;
import com.edu.infnet.pb.users.presentation.dtos.auth.LoginRequestDto;
import com.edu.infnet.pb.users.presentation.dtos.auth.LoginResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
  private final AuthRepository repo;

  public LoginResponseDto login(LoginRequestDto loginRequest) {
  }
}
