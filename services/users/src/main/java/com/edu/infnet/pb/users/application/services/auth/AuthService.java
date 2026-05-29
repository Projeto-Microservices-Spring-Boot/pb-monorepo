package com.edu.infnet.pb.users.application.services.auth;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.edu.infnet.pb.users.domain.enums.Roles;
import com.edu.infnet.pb.users.domain.models.User;
import com.edu.infnet.pb.users.infrastructure.repositories.AuthRepository;
import com.edu.infnet.pb.users.presentation.dtos.auth.LoginRequestDto;
import com.edu.infnet.pb.users.presentation.dtos.auth.LoginResponseDto;
import com.edu.infnet.pb.users.presentation.dtos.auth.RegisterRequestDto;
import com.edu.infnet.pb.users.presentation.dtos.auth.RegisterResponseDto;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
  private final AuthRepository repo;
  private final BCryptPasswordEncoder bcrypt;
  private final JwtEncoder jwt;

  @Transactional /* se algo falhar não cria o usuário "incompleto" */
  public RegisterResponseDto register(RegisterRequestDto registerRequest) {
    var userAlreadyExists = repo.findByEmail(registerRequest.email());

    if (userAlreadyExists.isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT);
    }

    var user = new User();
    user.setName(registerRequest.name());
    user.setEmail(registerRequest.email());
    user.setPassword(bcrypt.encode(registerRequest.password()));
    user.setRoles(Roles.BUYER);

    var createdUser = repo.save(user);

    return new RegisterResponseDto(
        createdUser.getName(),
        createdUser.getEmail(),
        createdUser.getRoles(),
        null);
  }

  public LoginResponseDto login(LoginRequestDto loginRequest) {
    var NOW = Instant.now();
    var EXPIRES_IN = 300L;

    var user = repo.findByEmail(loginRequest.email());

    if (user.isEmpty()) {
      throw new BadCredentialsException("Dados inválidos!");
    }

    if (!user.get().ComparePassword(loginRequest.password(), bcrypt)) {
      throw new BadCredentialsException("Dados inválidos!");
    }

    var claims = JwtClaimsSet.builder()
        .issuer("users-service")
        .subject(user.get().getId().toString())
        .issuedAt(NOW) // criado agora
        .expiresAt(NOW.plusSeconds(EXPIRES_IN)).build(); // expira em 5min

    var jwtValue = jwt.encode(JwtEncoderParameters.from(claims)).getTokenValue();

    return new LoginResponseDto(jwtValue, null, EXPIRES_IN);
  }
}
