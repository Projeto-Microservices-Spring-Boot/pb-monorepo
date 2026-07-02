package com.edu.infnet.pb.store.service;

import com.edu.infnet.pb.store.kafka.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaCheckoutEventPublisher implements CheckoutEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaCheckoutEventPublisher.class);

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    private final String topicPedidoCriado;

    public KafkaCheckoutEventPublisher(
            KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate,
            @Value("${app.kafka.topics.order-created}") String topicPedidoCriado
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicPedidoCriado = topicPedidoCriado;
    }

    @Override
    public void publicarPedidoCriado(OrderCreatedEvent event) {
        kafkaTemplate.send(topicPedidoCriado, event.orderId().toString(), event);

        log.info("Evento de pedido criado publicado no Kafka. orderId={}, userExternalId={}, totalValue={}, itemCount={}",
                event.orderId(), event.userExternalId(), event.totalValue(), event.itemCount());
    }
}
