package com.ecommerce.community.service;

import com.ecommerce.community.exception.ValidationException;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;


class ValidationServicePropertyTest {

    private final ValidationService validationService = new ValidationService();

    @Property
    void titleLengthBoundaries(@ForAll @IntRange(min = 0, max = 210) int length) {
        String title = "a".repeat(length);

        if (length >= ValidationService.TITLE_MIN_LENGTH && length <= ValidationService.TITLE_MAX_LENGTH) {
            assertDoesNotThrow(() -> validationService.validateTitle(title));
        } else {
            assertThatThrownBy(() -> validationService.validateTitle(title))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Property
    void postContentLengthBoundaries(@ForAll @IntRange(min = 0, max = 10010) int length) {
        String content = "a".repeat(length);

        if (length >= ValidationService.POST_CONTENT_MIN_LENGTH && length <= ValidationService.POST_CONTENT_MAX_LENGTH) {
            assertDoesNotThrow(() -> validationService.validatePostContent(content));
        } else {
            assertThatThrownBy(() -> validationService.validatePostContent(content))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Property
    void commentContentLengthBoundaries(@ForAll @IntRange(min = 0, max = 2010) int length) {
        String content = "a".repeat(length);

        if (length >= ValidationService.COMMENT_CONTENT_MIN_LENGTH && length <= ValidationService.COMMENT_CONTENT_MAX_LENGTH) {
            assertDoesNotThrow(() -> validationService.validateCommentContent(content));
        } else {
            assertThatThrownBy(() -> validationService.validateCommentContent(content))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Example
    void rejectsWhitespaceOnlyTitle() {
        assertThatThrownBy(() -> validationService.validateTitle("     "))
                .isInstanceOf(ValidationException.class);
    }

    @Example
    void acceptsBoundaryValues() {
        assertDoesNotThrow(() -> validationService.validateTitle("a".repeat(5)));
        assertDoesNotThrow(() -> validationService.validateTitle("a".repeat(200)));
        assertDoesNotThrow(() -> validationService.validatePostContent("a".repeat(10)));
        assertDoesNotThrow(() -> validationService.validatePostContent("a".repeat(10000)));
        assertDoesNotThrow(() -> validationService.validateCommentContent("a"));
        assertDoesNotThrow(() -> validationService.validateCommentContent("a".repeat(2000)));
        assertThat(true).isTrue();
    }
}
