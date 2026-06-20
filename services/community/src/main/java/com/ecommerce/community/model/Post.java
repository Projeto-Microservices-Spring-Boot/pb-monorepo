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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "posts")
@CompoundIndexes({
        @CompoundIndex(name = "userId_createdAt_idx", def = "{'userId': 1, 'createdAt': -1}")
})
public class Post {

    public static final int MAX_RECENT_COMMENTS = 10;

    @Id
    private String id;

    @Indexed
    private String userId;

    private String authorName;

    private String title;

    private String content;

    @Indexed(direction = org.springframework.data.mongodb.core.index.IndexDirection.DESCENDING)
    private Instant createdAt;

    private Instant updatedAt;

    @Indexed
    private Instant deletedAt;

    private boolean flagged;

    private String flagReason;

    @Builder.Default
    private List<EmbeddedComment> recentComments = new ArrayList<>();

   
    public void addComment(EmbeddedComment comment) {
        if (recentComments == null) {
            recentComments = new ArrayList<>();
        }
        recentComments.add(comment);
        recentComments.sort(Comparator.comparing(EmbeddedComment::getCreatedAt).reversed());
        while (recentComments.size() > MAX_RECENT_COMMENTS) {
            recentComments.remove(recentComments.size() - 1);
        }
    }

    
    public void markCommentAsDeleted(String commentId) {
        if (recentComments == null) {
            return;
        }
        recentComments.stream()
                .filter(c -> c.getCommentId().equals(commentId))
                .findFirst()
                .ifPresent(c -> c.setDeletedAt(Instant.now()));
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
