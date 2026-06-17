package com.edu.infnet.pb.payments.producer;

import com.edu.infnet.pb.payments.dto.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProducer {

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    public void publish(PaymentEvent event) {
        String topic = switch (event.status()) {
            case PENDING -> "payment.initiated";
            case APPROVED -> "payment.approved";
            case FAILED -> "payment.failed";
            default -> null;
        };

        if (topic == null) {
            log.warn("Nenhum tópico mapeado para o status: {}", event.status());
            return;
        }

        kafkaTemplate.send(topic, event.paymentId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Falha ao publicar evento no tópico {}: {}", topic, ex.getMessage());
                    } else {
                        log.info("Evento publicado no tópico {} para o pagamento {}", topic, event.paymentId());
                    }
                });
    }
}
