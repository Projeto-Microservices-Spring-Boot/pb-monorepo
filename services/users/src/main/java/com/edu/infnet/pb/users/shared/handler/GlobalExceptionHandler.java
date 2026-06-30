package com.edu.infnet.pb.users.shared.handler;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.edu.infnet.pb.users.shared.error.ErrorMessage;
import com.edu.infnet.pb.users.shared.exceptions.ConflictException;
import com.edu.infnet.pb.users.shared.exceptions.ResourceNotFoundException;
import com.edu.infnet.pb.users.shared.exceptions.UnauthorizedException;

import jakarta.ws.rs.BadRequestException;

@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ConflictException.class)
  private ResponseEntity<ErrorMessage> conflictException(ConflictException ex) {
    ErrorMessage errorMessage = new ErrorMessage("Dados já existem!", HttpStatus.CONFLICT, LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMessage);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  private ResponseEntity<ErrorMessage> notFoundException(ResourceNotFoundException ex) {
    ErrorMessage errorMessage = new ErrorMessage("Recurso não encontrado!", HttpStatus.NOT_FOUND, LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
  }

  @ExceptionHandler(UnauthorizedException.class)
  private ResponseEntity<ErrorMessage> unauthorizedException(UnauthorizedException ex) {
    ErrorMessage errorMessage = new ErrorMessage("Não autorizado!", HttpStatus.UNAUTHORIZED,
        LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorMessage);
  }

  @ExceptionHandler(BadRequestException.class)
  private ResponseEntity<ErrorMessage> badRequestException(BadRequestException ex) {
    ErrorMessage errorMessage = new ErrorMessage("Requisicao inválida!", HttpStatus.BAD_REQUEST,
        LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
  }
}
