package com.edu.infnet.pb.users.shared.error;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class ErrorMessage {
  private String message;
  private HttpStatus status;
  private LocalDateTime timestamp;
}
