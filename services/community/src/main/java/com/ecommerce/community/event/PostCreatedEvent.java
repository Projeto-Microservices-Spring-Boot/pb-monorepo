package com.ecommerce.community.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"eventType", "eventId", "timestamp", "source", "payload"})
@JsonInclude(JsonInclude.Include.ALWAYS)
public class PostCreatedEvent {

    @Builder.Default
    private String eventType = "PostCreated";

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private Instant timestamp = Instant.now();

    @Builder.Default
    private String source = "community-service";

    private Payload payload;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonPropertyOrder({"postId", "userId", "authorName", "title", "content", "createdAt"})
    public static class Payload {
        private String postId;
        private String userId;
        private String authorName;
        private String title;
        private String content;
        private Instant createdAt;
    }
}
