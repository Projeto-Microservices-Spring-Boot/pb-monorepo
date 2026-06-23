package com.edu.infnet.pb.payments.producer;

import com.edu.infnet.pb.payments.dto.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import io.github.resilience4j.retry.annotation.Retry;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProducer {

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @Retry(name = "kafka-publisher", fallbackMethod = "publishFallback")
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

        try {
            kafkaTemplate.send(topic, event.paymentId().toString(), event)
                    .get(2, TimeUnit.SECONDS);
            log.info("Evento publicado no tópico {} para o pagamento {}", topic, event.paymentId());
        } catch (ExecutionException | TimeoutException ex) {
            log.error("Falha ao publicar evento no tópico {}: {}", topic, ex.getMessage());
            throw new RuntimeException("Falha ao publicar evento Kafka", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Publicação Kafka interrompida", ex);
        }
    }

    private void publishFallback(PaymentEvent event, Exception ex) {
        log.error("Não foi possível publicar o evento do pagamento {} após retries: {}",
                event.paymentId(), ex.getMessage());
    }
}
