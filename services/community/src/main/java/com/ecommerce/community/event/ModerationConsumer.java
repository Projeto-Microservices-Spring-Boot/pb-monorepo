package com.ecommerce.community.event;

import com.ecommerce.community.repository.CommentRepository;
import com.ecommerce.community.repository.PostRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;

import java.util.Map;


@Slf4j
@Component
@RequiredArgsConstructor
public class ModerationConsumer {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ObjectMapper objectMapper;

    @SqsListener("${aws.sqs.queue.moderation}")
    public void processMessage(String payload, @Headers Map<String, Object> headers) {
        String eventType = headerAsString(headers, "eventType");
        log.info("Received moderation message eventType={}", eventType);

        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode payloadNode = root.path("payload");

            if ("PostCreated".equals(eventType)) {
                processPostCreated(payloadNode);
            } else if ("CommentCreated".equals(eventType)) {
                processCommentCreated(payloadNode);
            } else {
                log.info("Ignoring unsupported eventType={} on moderation queue", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process moderation message eventType={}: {}", eventType, e.getMessage(), e);
            throw new RuntimeException("Moderation processing failed", e);
        }
    }

    private void processPostCreated(JsonNode payloadNode) {
        String postId = payloadNode.path("postId").asText();
        String content = payloadNode.path("content").asText();

        if (containsInappropriateContent(content)) {
            postRepository.findById(postId).ifPresent(post -> {
                post.setFlagged(true);
                post.setFlagReason("Content flagged by moderation");
                postRepository.save(post);
                log.info("Flagged Post postId={}", postId);
            });
        }
    }

    private void processCommentCreated(JsonNode payloadNode) {
        String commentId = payloadNode.path("commentId").asText();
        String content = payloadNode.path("content").asText();

        if (containsInappropriateContent(content)) {
            commentRepository.findById(commentId).ifPresent(comment -> {
                comment.setFlagged(true);
                comment.setFlagReason("Content flagged by moderation");
                commentRepository.save(comment);
                log.info("Flagged Comment commentId={}", commentId);
            });
        }
    }

    private boolean containsInappropriateContent(String content) {
        return false;
    }

    private String headerAsString(Map<String, Object> headers, String key) {
        Object value = headers.get(key);
        return value != null ? value.toString() : null;
    }
}
