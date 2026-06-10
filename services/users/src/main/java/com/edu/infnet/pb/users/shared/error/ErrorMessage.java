package com.edu.infnet.pb.users.shared.error;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class ErrorMessage {
  @Schema(example = "Dados já existem!")
  private String message;

  @Schema(example = "CONFLICT")
  private HttpStatus status;

  @Schema(example = "2026-06-09T20:00:00")
  private LocalDateTime timestamp;
}
