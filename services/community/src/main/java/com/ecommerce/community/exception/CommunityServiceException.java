package com.ecommerce.community.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public abstract class CommunityServiceException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    protected CommunityServiceException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
