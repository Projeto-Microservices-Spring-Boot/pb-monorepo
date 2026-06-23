package com.ecommerce.community.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends CommunityServiceException {

    public ForbiddenException(String message) {
        super(message, "FORBIDDEN", HttpStatus.FORBIDDEN);
    }
}
