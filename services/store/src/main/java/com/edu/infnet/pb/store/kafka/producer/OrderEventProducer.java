package com.edu.infnet.pb.store.kafka.producer;

import com.edu.infnet.pb.store.kafka.event.OrderCancelledEvent;
import com.edu.infnet.pb.store.kafka.event.OrderCreatedEvent;
import com.edu.infnet.pb.store.kafka.event.OrderStatusChangedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topicOrderCreated;
    private final String topicOrderCancelled;
    private final String topicOrderStatusChanged;

    public OrderEventProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${app.kafka.topics.order-created}") String topicOrderCreated,
            @Value("${app.kafka.topics.order-cancelled}") String topicOrderCancelled,
            @Value("${app.kafka.topics.order-status-changed}") String topicOrderStatusChanged
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topicOrderCreated = topicOrderCreated;
        this.topicOrderCancelled = topicOrderCancelled;
        this.topicOrderStatusChanged = topicOrderStatusChanged;
    }

    public void sendOrderCreated(OrderCreatedEvent event) {
        send(topicOrderCreated, event.orderId().toString(), event);
    }

    public void sendOrderCancelled(OrderCancelledEvent event) {
        send(topicOrderCancelled, event.orderId().toString(), event);
    }

    public void sendStatusChanged(OrderStatusChangedEvent event) {
        send(topicOrderStatusChanged, event.orderId().toString(), event);
    }

    private void send(String topic, String key, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, key, payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Falha ao enviar evento para tópico {}: {}", topic, ex.getMessage(), ex);
                        } else if (result != null && result.getRecordMetadata() != null) {
                            log.info("Evento enviado: topic={}, key={}, partition={}, offset={}",
                                    topic,
                                    key,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.info("Evento enviado: topic={}, key={}", topic, key);
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar evento para tópico {}: {}", topic, e.getMessage(), e);
        }
    }
}
