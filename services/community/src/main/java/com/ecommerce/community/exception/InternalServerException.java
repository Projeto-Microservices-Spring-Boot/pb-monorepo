package com.ecommerce.community.exception;

import org.springframework.http.HttpStatus;

public class InternalServerException extends CommunityServiceException {

    public InternalServerException(String message) {
        super(message, "INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public InternalServerException(String message, Throwable cause) {
        super(message, "INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        initCause(cause);
    }
}
