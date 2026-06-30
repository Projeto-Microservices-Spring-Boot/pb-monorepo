package com.ecommerce.community.exception;

import com.ecommerce.community.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleValidation_returns400WithFieldErrors() {
        ValidationException ex = new ValidationException("Validation failed",
                Map.of("title", "title is required", "content", "content is required"));

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getFieldErrors()).containsKey("title");
        assertThat(response.getBody().getFieldErrors()).containsKey("content");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void handleUnauthorized_returns401() {
        UnauthorizedException ex = new UnauthorizedException("JWT missing");

        ResponseEntity<ErrorResponse> response = handler.handleUnauthorized(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getError()).isEqualTo("UNAUTHORIZED");
        assertThat(response.getBody().getMessage()).isEqualTo("JWT missing");
    }

    @Test
    void handleForbidden_returns403() {
        ForbiddenException ex = new ForbiddenException("Not allowed");

        ResponseEntity<ErrorResponse> response = handler.handleForbidden(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getError()).isEqualTo("FORBIDDEN");
    }

    @Test
    void handleNotFound_returns404() {
        NotFoundException ex = new NotFoundException("Post not found: abc");

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getError()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().getMessage()).contains("abc");
    }

    @Test
    void handleMethodArgumentNotValid_returns400WithFieldErrors() throws NoSuchMethodException {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("createPostRequest", "title", "must not be blank");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentNotValid(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getFieldErrors()).containsKey("title");
    }

    @Test
    void handleGenericException_returns500() {
        RuntimeException ex = new RuntimeException("Unexpected");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getError()).isEqualTo("INTERNAL_SERVER_ERROR");
    }

    @Test
    void allResponses_haveTimestamp() {
        ResponseEntity<ErrorResponse>[] responses = new ResponseEntity[] {
            handler.handleUnauthorized(new UnauthorizedException("x")),
            handler.handleForbidden(new ForbiddenException("x")),
            handler.handleNotFound(new NotFoundException("x")),
            handler.handleGenericException(new RuntimeException("x")),
        };
        for (ResponseEntity<ErrorResponse> resp : responses) {
            assertThat(resp.getBody().getTimestamp()).isNotNull();
        }
    }
}
