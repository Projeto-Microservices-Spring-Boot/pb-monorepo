package com.ecommerce.community.event;

import com.ecommerce.community.model.Post;
import com.ecommerce.community.repository.PostRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;


@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final PostRepository postRepository;
    private final NotificationServiceClient notificationServiceClient;
    private final ObjectMapper objectMapper;

    @SqsListener("${aws.sqs.queue.notification}")
    public void processMessage(String payload, @Headers Map<String, Object> headers) {
        String eventType = headerAsString(headers, "eventType");
        log.info("Received notification message eventType={}", eventType);

        try {
            if (!"CommentCreated".equals(eventType)) {
                log.info("Ignoring unsupported eventType={} on notification queue", eventType);
                return;
            }

            JsonNode root = objectMapper.readTree(payload);
            JsonNode payloadNode = root.path("payload");
            processCommentCreated(payloadNode);
        } catch (Exception e) {
            log.error("Failed to process notification message eventType={}: {}", eventType, e.getMessage(), e);
            throw new RuntimeException("Notification processing failed", e);
        }
    }

    private void processCommentCreated(JsonNode payloadNode) {
        String postId = payloadNode.path("postId").asText();
        String commentAuthorId = payloadNode.path("userId").asText();
        String commentAuthorName = payloadNode.path("authorName").asText();

        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            log.warn("Post not found for notification, postId={}", postId);
            return;
        }

        Post post = postOpt.get();
        if (post.getUserId() != null && post.getUserId().equals(commentAuthorId)) {
            log.info("Skipping notification: comment author is post author, postId={}", postId);
            return;
        }

        String message = commentAuthorName + " commented on your post: " + post.getTitle();
        notificationServiceClient.sendNotification(post.getUserId(), message);
        log.info("Notification sent to userId={} for postId={}", post.getUserId(), postId);
    }

    private String headerAsString(Map<String, Object> headers, String key) {
        Object value = headers.get(key);
        return value != null ? value.toString() : null;
    }
}
