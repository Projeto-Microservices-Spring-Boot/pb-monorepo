package com.edu.infnet.pb.users.application.services.profile;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.edu.infnet.pb.users.infrastructure.repositories.ProfileRepository;
import com.edu.infnet.pb.users.presentation.dtos.auth.ProfileResponseDto;

import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfileService {
  private final ProfileRepository repo;

  public ProfileResponseDto getProfile(UUID userId) {
    var userExists = repo.findById(userId);

    if (userExists.isEmpty()) {
      throw new NotFoundException("user not found");
    }

    return new ProfileResponseDto(userExists.get().getName());
  }
}
