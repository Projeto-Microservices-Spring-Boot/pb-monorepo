package com.ecommerce.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostWithCommentsResponse {
    private String postId;
    private String userId;
    private String authorName;
    private String title;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;
    private List<CommentResponse> comments;
    private long totalElements;
    private int totalPages;
    private int currentPage;
}
