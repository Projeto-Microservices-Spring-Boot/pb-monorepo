package com.edu.infnet.pb.store.service;

import com.edu.infnet.pb.store.dto.event.PedidoCriadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaCheckoutEventPublisher implements CheckoutEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaCheckoutEventPublisher.class);
    private static final String TOPIC_PEDIDO_CRIADO = "pedido-criado";

    private final KafkaTemplate<String, PedidoCriadoEvent> kafkaTemplate;

    public KafkaCheckoutEventPublisher(KafkaTemplate<String, PedidoCriadoEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publicarPedidoCriado(PedidoCriadoEvent event) {
        kafkaTemplate.send(TOPIC_PEDIDO_CRIADO, event.orderId().toString(), event);

        log.info("Evento de pedido criado publicado no Kafka. orderId={}, userId={}, amount={}",
                event.orderId(), event.userId(), event.amount());
    }
}
