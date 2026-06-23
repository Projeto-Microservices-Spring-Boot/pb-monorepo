package com.ecommerce.community.service;

import com.ecommerce.community.exception.ValidationException;
import org.springframework.stereotype.Service;


@Service
public class ValidationService {

    public static final int TITLE_MIN_LENGTH = 5;
    public static final int TITLE_MAX_LENGTH = 200;
    public static final int POST_CONTENT_MIN_LENGTH = 10;
    public static final int POST_CONTENT_MAX_LENGTH = 10000;
    public static final int COMMENT_CONTENT_MIN_LENGTH = 1;
    public static final int COMMENT_CONTENT_MAX_LENGTH = 2000;

    public void validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new ValidationException("title", "title is required", true);
        }
        int length = title.length();
        if (length < TITLE_MIN_LENGTH || length > TITLE_MAX_LENGTH) {
            throw new ValidationException("title",
                    "title length must be between " + TITLE_MIN_LENGTH + " and " + TITLE_MAX_LENGTH + " characters",
                    true);
        }
    }

    
    public void validatePostContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new ValidationException("content", "content is required", true);
        }
        int length = content.length();
        if (length < POST_CONTENT_MIN_LENGTH || length > POST_CONTENT_MAX_LENGTH) {
            throw new ValidationException("content",
                    "content length must be between " + POST_CONTENT_MIN_LENGTH + " and " + POST_CONTENT_MAX_LENGTH
                            + " characters",
                    true);
        }
    }

    
    public void validateCommentContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new ValidationException("content", "content is required", true);
        }
        int length = content.length();
        if (length < COMMENT_CONTENT_MIN_LENGTH || length > COMMENT_CONTENT_MAX_LENGTH) {
            throw new ValidationException("content",
                    "content length must be between " + COMMENT_CONTENT_MIN_LENGTH + " and "
                            + COMMENT_CONTENT_MAX_LENGTH + " characters",
                    true);
        }
    }

    
    public void validatePostId(String postId) {
        if (postId == null || postId.trim().isEmpty()) {
            throw new ValidationException("postId", "postId is required", true);
        }
    }
}
