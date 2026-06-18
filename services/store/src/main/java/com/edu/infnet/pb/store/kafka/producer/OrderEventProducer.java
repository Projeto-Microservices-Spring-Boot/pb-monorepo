package com.edu.infnet.pb.store.kafka.producer;

import com.edu.infnet.pb.store.kafka.event.OrderCancelledEvent;
import com.edu.infnet.pb.store.kafka.event.OrderCreatedEvent;
import com.edu.infnet.pb.store.kafka.event.OrderStatusChangedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);

    private static final String TOPIC_ORDER_CREATED = "order.created";
    private static final String TOPIC_ORDER_CANCELLED = "order.cancelled";
    private static final String TOPIC_ORDER_STATUS = "order.status.changed";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OrderEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendOrderCreated(OrderCreatedEvent event) {
        send(TOPIC_ORDER_CREATED, event.orderId().toString(), event);
    }

    public void sendOrderCancelled(OrderCancelledEvent event) {
        send(TOPIC_ORDER_CANCELLED, event.orderId().toString(), event);
    }

    public void sendStatusChanged(OrderStatusChangedEvent event) {
        send(TOPIC_ORDER_STATUS, event.orderId().toString(), event);
    }

    private void send(String topic, String key, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, key, payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Falha ao enviar evento para tópico {}: {}", topic, ex.getMessage());
                        } else {
                            log.info("Evento enviado: topic={}, key={}, offset={}",
                                    topic, key, result.getRecordMetadata().offset());
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar evento para tópico {}: {}", topic, e.getMessage());
        }
    }
}
