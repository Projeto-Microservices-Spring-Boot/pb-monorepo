package com.ecommerce.community.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends CommunityServiceException {

    public NotFoundException(String message) {
        super(message, "NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
