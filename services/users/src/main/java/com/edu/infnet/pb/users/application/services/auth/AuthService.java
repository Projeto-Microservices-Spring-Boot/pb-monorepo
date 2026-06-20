package com.edu.infnet.pb.users.application.services.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.edu.infnet.pb.users.domain.enums.Roles;
import com.edu.infnet.pb.users.domain.models.User;
import com.edu.infnet.pb.users.infrastructure.repositories.AuthRepository;
import com.edu.infnet.pb.users.presentation.dtos.auth.LoginRequestDto;
import com.edu.infnet.pb.users.presentation.dtos.auth.LoginResponseDto;
import com.edu.infnet.pb.users.presentation.dtos.auth.RegisterRequestDto;
import com.edu.infnet.pb.users.presentation.dtos.auth.RegisterResponseDto;
import com.edu.infnet.pb.users.shared.exceptions.BadRequestException;
import com.edu.infnet.pb.users.shared.exceptions.ConflictException;
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

  long ACCESS_TOKEN_EXPIRES_IN = 300L; // expira em 5min
  long REFRESH_TOKEN_EXPIRES_IN = 604800; // expira em 7 dias

  @Transactional
  public RegisterResponseDto register(RegisterRequestDto registerRequest) {
    var userAlreadyExists = repo.findByEmail(registerRequest.email());

    if (userAlreadyExists.isPresent()) {
      logger.error("Erro ao criar conta, usuário já existente");
      throw new ConflictException("Erro ao criar conta, usuário já existente!");
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
        createdUser.getRoles());
  }

  @Transactional
  public RegisterResponseDto registerSeller(RegisterRequestDto registerRequest) {
    var sellerUserAlreadyExists = repo.findByEmail(registerRequest.email());

    if (sellerUserAlreadyExists.isPresent()) {
      logger.error("Erro ao criar conta, usuário vendedor já existente");
      throw new ConflictException("Erro ao criar conta, usuário vendedor já existente!");
    }

    var sellerUser = new User();
    sellerUser.setName(registerRequest.name());
    sellerUser.setEmail(registerRequest.email());
    sellerUser.setPassword(bcrypt.encode(registerRequest.password()));
    sellerUser.setRoles(Roles.SELLER);

    var createdSellerUser = repo.save(sellerUser);

    logger.info("Usuário Vendedor criado com sucesso!");
    return new RegisterResponseDto(
        createdSellerUser.getName(),
        createdSellerUser.getEmail(),
        createdSellerUser.getRoles());
  }

  @Transactional
  public LoginResponseDto login(LoginRequestDto loginRequest) {
    Instant NOW = Instant.now();

    var user = repo.findByEmail(loginRequest.email());

    if (user.isEmpty()) {
      logger.error("Erro ao fazer login, dados inválidos!");
      throw new BadRequestException("Dados inválidos!");
    }

    if (!user.get().ComparePassword(loginRequest.password(), bcrypt)) {
      logger.error("Erro ao fazer login, dados inválidos!");
      throw new BadRequestException("Dados inválidos!");
    }

    var claims = JwtClaimsSet.builder()
        .issuer("frontend")
        .subject(user.get().getId().toString())
        .claim("name", user.get().getName())
        .claim("role", user.get().getRoles())
        .issuedAt(NOW)
        .expiresAt(NOW.plusSeconds(ACCESS_TOKEN_EXPIRES_IN)).build();

    var accessToken = jwt.encode(JwtEncoderParameters.from(claims)).getTokenValue();

    var rawRefreshToken = user.get().generateRefreshToken();

    updateRefreshToken(user.get(), rawRefreshToken);
    user.get().setRefreshTokenExpiresIn(NOW.plusSeconds(REFRESH_TOKEN_EXPIRES_IN));

    repo.save(user.get());

    logger.info("Usuário logado com sucesso!");
    return new LoginResponseDto(accessToken, rawRefreshToken, ACCESS_TOKEN_EXPIRES_IN, REFRESH_TOKEN_EXPIRES_IN);

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

  private void updateRefreshToken(User user, String rawRefreshToken) {
    try {
      var hashedRefreshToken = HexFormat.of()
          .formatHex(MessageDigest.getInstance("SHA-256")
              .digest(rawRefreshToken.getBytes()));

      user.setRefreshToken(hashedRefreshToken);
      repo.save(user);
    } catch (NoSuchAlgorithmException e) {
      throw new BadRequestException(e.getMessage());
    }
  }

  // public LoginResponseDto refresh(String refreshToken, UUID userId) {
  // Instant NOW = Instant.now();

  // var userExists = repo.findById(userId);
  // if (!userExists.isPresent()) {
  // logger.error("Usuário não encontrado!");
  // throw new ResourceNotFoundException("usuário não encontrado!");
  // }

  // var user = userExists.get();

  // var claims = JwtClaimsSet.builder()
  // .issuer("frontend")
  // .subject(user.getId().toString())
  // .claim("name", user.getName())
  // .claim("role", user.getRoles())
  // .issuedAt(NOW)
  // .expiresAt(NOW.plusSeconds(ACCESS_TOKEN_EXPIRES_IN)).build();

  // var newAccessToken =
  // jwt.encode(JwtEncoderParameters.from(claims)).getTokenValue();
  // var newRawRefreshToken = user.generateRefreshToken();

  // updateRefreshToken(user, refreshToken);
  // user.setRefreshTokenExpiresIn(NOW.plusSeconds(REFRESH_TOKEN_EXPIRES_IN));

  // logger.info("Tokens recriados com sucesso!");
  // repo.save(user);

  // return new LoginResponseDto(newAccessToken, newRawRefreshToken,
  // ACCESS_TOKEN_EXPIRES_IN, REFRESH_TOKEN_EXPIRES_IN);
  // }
}
