package com.edu.infnet.pb.users.presentation.controllers.profile;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.edu.infnet.pb.users.application.services.profile.ProfileService;
import com.edu.infnet.pb.users.presentation.dtos.auth.ProfileResponseDto;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ProfileController {
  private final ProfileService service;

  @GetMapping("/me")
  public ResponseEntity<ProfileResponseDto> getProfile(@AuthenticationPrincipal Jwt jwt) {
    UUID userId = UUID.fromString(jwt.getSubject());
    var result = service.getProfile(userId);
    return ResponseEntity.ok(result);
  }

}
