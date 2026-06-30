package com.ecommerce.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private String commentId;
    private String postId;
    private String userId;
    private String authorName;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;
}
