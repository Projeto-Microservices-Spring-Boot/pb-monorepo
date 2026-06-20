package com.ecommerce.community.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.awspring.cloud.sns.core.SnsTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@Service
public class EventPublisher {

    private final SnsTemplate snsTemplate;
    private final ObjectMapper objectMapper;
    private final RetryTemplate retryTemplate;
    private final String topicArn;

    public EventPublisher(SnsTemplate snsTemplate,
                           @Value("${aws.sns.topic.community-events}") String topicArn) {
        this.snsTemplate = snsTemplate;
        this.topicArn = topicArn;
        this.objectMapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .findAndRegisterModules();
        this.retryTemplate = buildRetryTemplate();
    }

    private RetryTemplate buildRetryTemplate() {
        RetryTemplate template = new RetryTemplate();

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000L);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(10000L);
        template.setBackOffPolicy(backOffPolicy);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        template.setRetryPolicy(retryPolicy);

        return template;
    }

    public void publishPostCreated(PostCreatedEvent event) {
        publishEvent(event, event.getEventType());
    }

    public void publishCommentCreated(CommentCreatedEvent event) {
        publishEvent(event, event.getEventType());
    }

    public void publishPostUpdated(PostUpdatedEvent event) {
        publishEvent(event, event.getEventType());
    }

    public void publishPostDeleted(PostDeletedEvent event) {
        publishEvent(event, event.getEventType());
    }

    public void publishCommentDeleted(CommentDeletedEvent event) {
        publishEvent(event, event.getEventType());
    }

  
    private void publishEvent(Object event, String eventType) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            Map<String, Object> headers = new HashMap<>();
            headers.put("eventType", eventType);
            headers.put("source", "community-service");
            headers.put("timestamp", Instant.now().toString());

            Message<String> message = MessageBuilder
                    .withPayload(payload)
                    .copyHeaders(headers)
                    .build();

            retryTemplate.execute(context -> {
                snsTemplate.send(topicArn, message);
                return null;
            });

            log.info("Published event eventType={} topic={}", eventType, topicArn);
        } catch (Exception e) {
            log.error("Failed to publish event eventType={} after retries: {}", eventType, e.getMessage(), e);
        }
    }
}
