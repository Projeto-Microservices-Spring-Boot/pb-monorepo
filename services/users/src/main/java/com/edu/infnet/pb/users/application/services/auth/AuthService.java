package com.edu.infnet.pb.users.application.services.auth;

import java.time.Instant;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import com.edu.infnet.pb.users.shared.exceptions.ResourceNotFoundException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
  private final AuthRepository repo;
  private final BCryptPasswordEncoder bcrypt;
  private final JwtEncoder jwt;

  private static final Logger logger = LogManager.getLogger(AuthService.class);

  Instant NOW = Instant.now(); // criado agora
  long ACESS_TOKEN_EXPIRES_IN = 300L; // expira em 5min
  long REFRESH_TOKEN_EXPIRES_IN = 604800; // expira em 7 dias

  @Transactional
  public RegisterResponseDto register(RegisterRequestDto registerRequest) {
    var userAlreadyExists = repo.findByEmail(registerRequest.email());

    if (userAlreadyExists.isPresent()) {
      logger.error("Erro ao criar conta, usuário já existente");
      throw new ResponseStatusException(HttpStatus.CONFLICT);
    }

    var user = new User();
    user.setName(registerRequest.name());
    user.setEmail(registerRequest.email());
    user.setPassword(bcrypt.encode(registerRequest.password()));
    user.setRoles(Roles.BUYER);

    var createdUser = repo.save(user);

    logger.info("Usuário criado com sucesso!");
    return new RegisterResponseDto(
        createdUser.getName(),
        createdUser.getEmail(),
        createdUser.getRoles(),
        null);
  }

  @Transactional
  public LoginResponseDto login(LoginRequestDto loginRequest) {

    var user = repo.findByEmail(loginRequest.email());

    if (user.isEmpty()) {
      logger.error("Erro ao fazer login, dados inválidos!");
      throw new BadCredentialsException("Dados inválidos!");
    }

    if (!user.get().ComparePassword(loginRequest.password(), bcrypt)) {
      logger.error("Erro ao fazer login, dados inválidos!");
      throw new BadCredentialsException("Dados inválidos!");
    }

    var claims = JwtClaimsSet.builder()
        .issuer("users-service")
        .subject(user.get().getId().toString())
        .claim("name", user.get().getName())
        .claim("role", user.get().getRoles())
        .issuedAt(NOW)
        .expiresAt(NOW.plusSeconds(ACESS_TOKEN_EXPIRES_IN)).build();

    var accessToken = jwt.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    var refreshToken = UUID.randomUUID().toString(); // !! talvez não seja a melhor prática

    user.get().setRefreshToken(refreshToken);

    logger.info("Usuário logado com sucesso!");
    return new LoginResponseDto(accessToken, refreshToken, ACESS_TOKEN_EXPIRES_IN);
  }

  @Transactional
  public void Logout(UUID userId) {
    var userExists = repo.findById(userId);

    if (userExists.isEmpty()) {
      logger.error("Usuário não encontrado!");
      throw new ResourceNotFoundException("usuário não encontrado!");
    }

    var user = userExists.get();
    var refreshToken = user.getRefreshToken();

    if (refreshToken == null || refreshToken.isBlank()) {
      logger.error("RefreshToken não encontrado!");
      throw new ResourceNotFoundException("refresh token não encontrado!");
    }

    user.setRefreshToken(null);
    repo.save(user);

    logger.info("Usuário deslogado com sucesso!");
  }
}
