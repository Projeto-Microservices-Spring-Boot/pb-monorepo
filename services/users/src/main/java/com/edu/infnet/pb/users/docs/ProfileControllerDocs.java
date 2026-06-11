package com.edu.infnet.pb.users.docs;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import com.edu.infnet.pb.users.presentation.dtos.auth.ProfileResponseDto;
import com.edu.infnet.pb.users.shared.error.ErrorMessage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "User Profile", description = "Perfil do usuário")
public interface ProfileControllerDocs {

  @Operation(summary = "Obter perfil", description = "Retorna os dados do perfil do usuário autenticado. Requer token JWT no header Authorization.", responses = {
      @ApiResponse(responseCode = "200", description = "Perfil retornado com sucesso"),
      @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido", content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
  })
  ResponseEntity<ProfileResponseDto> getProfile(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request);
}
