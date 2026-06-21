package com.edu.infnet.pb.payments.consumer;

import com.edu.infnet.pb.payments.dto.OrderPaymentRequestedEvent;
import com.edu.infnet.pb.payments.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentConsumer {

    private final PaymentService service;

        @KafkaListener(topics = "order.payment.requested")
    public void consume(OrderPaymentRequestedEvent event) {
        log.info("Evento recebido em order.payment.requested: orderId={}", event.orderId());

        try {
            service.createFromOrder(event);
        } catch (Exception ex) {
            log.error("Falha ao processar evento de order.payment.requested (orderId={}): {}",
                    event.orderId(), ex.getMessage(), ex);
        }
    }
}