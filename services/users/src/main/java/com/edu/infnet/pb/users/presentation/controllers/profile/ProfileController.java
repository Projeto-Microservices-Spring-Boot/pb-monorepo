package com.edu.infnet.pb.users.presentation.controllers.profile;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.edu.infnet.pb.users.application.services.profile.ProfileService;
import com.edu.infnet.pb.users.docs.ProfileControllerDocs;
import com.edu.infnet.pb.users.presentation.dtos.auth.ProfileResponseDto;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ProfileController implements ProfileControllerDocs {
  private final ProfileService service;
  private static final Logger log = LogManager.getLogger(ProfileController.class);

  /*
   * @AuthenticationPrincipal Jwt => O Spring Security injeta o token JWT decoded
   */
  @GetMapping("/me")
  public ResponseEntity<ProfileResponseDto> getProfile(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request) {
    log.info("=== HEADERS DO KONG ===");
    log.info("X-Consumer-Username: {}", request.getHeader("X-Consumer-Username"));
    log.info("X-Credential-Identifier: {}", request.getHeader("X-Credential-Identifier"));
    log.info("X-Consumer-ID: {}", request.getHeader("X-Consumer-ID"));
    log.info("Authorization: {}", request.getHeader("Authorization"));

    UUID userId = UUID.fromString(jwt.getSubject());
    var result = service.getProfile(userId);
    return ResponseEntity.ok(result);
  }

}
