package com.ecommerce.community.model;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


class PostBucketPatternPropertyTest {

    @Property
    void bucketNeverExceedsTenComments(@ForAll @IntRange(min = 0, max = 100) int commentCount) {
        Post post = Post.builder()
                .id(UUID.randomUUID().toString())
                .userId("user-1")
                .authorName("Author")
                .title("Some Title")
                .content("Some content for the post")
                .createdAt(Instant.now())
                .build();

        List<EmbeddedComment> generated = new ArrayList<>();
        Instant base = Instant.now();
        for (int i = 0; i < commentCount; i++) {
            EmbeddedComment comment = new EmbeddedComment(
                    "comment-" + i,
                    "user-" + i,
                    "Author " + i,
                    "Comment content " + i,
                    base.plus(i, ChronoUnit.SECONDS),
                    null);
            generated.add(comment);
            post.addComment(comment);
        }

        assertThat(post.getRecentComments().size()).isLessThanOrEqualTo(Post.MAX_RECENT_COMMENTS);

        for (int i = 0; i < post.getRecentComments().size() - 1; i++) {
            Instant current = post.getRecentComments().get(i).getCreatedAt();
            Instant next = post.getRecentComments().get(i + 1).getCreatedAt();
            assertThat(current).isAfterOrEqualTo(next);
        }

        if (commentCount > Post.MAX_RECENT_COMMENTS) {
            assertThat(post.getRecentComments()).hasSize(Post.MAX_RECENT_COMMENTS);
            List<EmbeddedComment> sortedDesc = new ArrayList<>(generated);
            sortedDesc.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
            List<String> expectedIds = sortedDesc.subList(0, Post.MAX_RECENT_COMMENTS).stream()
                    .map(EmbeddedComment::getCommentId)
                    .sorted()
                    .toList();
            List<String> actualIds = post.getRecentComments().stream()
                    .map(EmbeddedComment::getCommentId)
                    .sorted()
                    .toList();
            assertThat(actualIds).isEqualTo(expectedIds);
        } else {
            assertThat(post.getRecentComments()).hasSize(commentCount);
        }
    }
}
