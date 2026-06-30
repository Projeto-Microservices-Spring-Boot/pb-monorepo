package com.ecommerce.community.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends CommunityServiceException {

    public UnauthorizedException(String message) {
        super(message, "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
    }
}
