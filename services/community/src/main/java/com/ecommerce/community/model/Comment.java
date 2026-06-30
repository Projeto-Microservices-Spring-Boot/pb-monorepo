package com.ecommerce.community.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "comments")
@CompoundIndexes({
        @CompoundIndex(name = "postId_createdAt_idx", def = "{'postId': 1, 'createdAt': 1}"),
        @CompoundIndex(name = "userId_createdAt_idx", def = "{'userId': 1, 'createdAt': -1}")
})
public class Comment {

    @Id
    private String id;

    @Indexed
    private String postId;

    @Indexed
    private String userId;

    private String authorName;

    private String content;

    private Instant createdAt;

    private Instant updatedAt;

    @Indexed
    private Instant deletedAt;

    private boolean flagged;

    private String flagReason;

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
