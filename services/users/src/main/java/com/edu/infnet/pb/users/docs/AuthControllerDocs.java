package com.edu.infnet.pb.users.docs;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RequestBody;

import com.edu.infnet.pb.users.presentation.dtos.auth.LoginRequestDto;
import com.edu.infnet.pb.users.presentation.dtos.auth.LoginResponseDto;
import com.edu.infnet.pb.users.presentation.dtos.auth.RegisterRequestDto;
import com.edu.infnet.pb.users.presentation.dtos.auth.RegisterResponseDto;
import com.edu.infnet.pb.users.shared.error.ErrorMessage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Auth", description = "Autenticação e gerenciamento de sessão")
public interface AuthControllerDocs {

  @Operation(summary = "Cadastrar usuário", description = "Cria um novo usuário com perfil BUYER", responses = {
      @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso"),
      @ApiResponse(responseCode = "409", description = "Email já cadastrado", content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
  })
  ResponseEntity<RegisterResponseDto> register(
      @RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados do novo usuário") RegisterRequestDto body);

  @Operation(summary = "Autenticar usuário", description = "Realiza login e retorna tokens JWT de acesso e refresh", responses = {
      @ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
      @ApiResponse(responseCode = "401", description = "Email ou senha inválidos", content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
  })
  ResponseEntity<LoginResponseDto> login(
      @RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Credenciais do usuário") LoginRequestDto body);

  @Operation(summary = "Encerrar sessão", description = "Invalida o refresh token do usuário. Requer token JWT no header Authorization.", responses = {
      @ApiResponse(responseCode = "204", description = "Sessão encerrada com sucesso", content = @Content),
      @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
  })
  ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt);
}
