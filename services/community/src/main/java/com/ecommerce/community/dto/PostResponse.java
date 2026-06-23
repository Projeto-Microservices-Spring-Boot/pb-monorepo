package com.ecommerce.community.dto;

import com.ecommerce.community.model.EmbeddedComment;
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
public class PostResponse {
    private String postId;
    private String userId;
    private String authorName;
    private String title;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;
    private List<EmbeddedComment> recentComments;
}
