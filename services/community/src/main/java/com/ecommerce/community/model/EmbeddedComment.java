package com.ecommerce.community.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddedComment {

    private String commentId;
    private String userId;
    private String authorName;
    private String content;
    private Instant createdAt;
    private Instant deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
