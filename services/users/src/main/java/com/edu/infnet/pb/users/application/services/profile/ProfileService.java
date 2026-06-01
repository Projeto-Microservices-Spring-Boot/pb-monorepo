package com.edu.infnet.pb.users.application.services.profile;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.edu.infnet.pb.users.infrastructure.repositories.ProfileRepository;
import com.edu.infnet.pb.users.presentation.dtos.auth.ProfileResponseDto;
import com.edu.infnet.pb.users.shared.exceptions.ResourceNotFoundException;
import com.edu.infnet.pb.users.shared.exceptions.UnauthorizedException;

import lombok.RequiredArgsConstructor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Service
@RequiredArgsConstructor
public class ProfileService {
  private final ProfileRepository repo;

  private static final Logger logger = LogManager.getLogger(ProfileService.class);

  public ProfileResponseDto getProfile(UUID userId) {
    var userExists = repo.findById(userId);

    if (userExists.isEmpty()) {
      logger.error("Usuário não encontrado!");
      throw new ResourceNotFoundException("Usuário não encontrado!");
    }

    var user = userExists.get();

    if (user.getRefreshToken() == null) {
      logger.error("Usuário não está autenticado!");
      throw new UnauthorizedException("Usuário não está autenticado!");
    }

    return new ProfileResponseDto(user.getName());
  }
}
