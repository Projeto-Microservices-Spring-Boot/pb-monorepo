package com.edu.infnet.pb.store.kafka.consumer;

import com.edu.infnet.pb.store.domain.pedido.Pedido;
import com.edu.infnet.pb.store.domain.pedido.StatusPedido;
import com.edu.infnet.pb.store.kafka.event.PaymentEvent;
import com.edu.infnet.pb.store.repository.PedidoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final PedidoRepository pedidoRepository;
    private final ObjectMapper objectMapper;

    public PaymentEventConsumer(PedidoRepository pedidoRepository, ObjectMapper objectMapper) {
        this.pedidoRepository = pedidoRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "payment.confirmed", groupId = "store-group")
    @Transactional
    public void onPaymentConfirmed(String message) {
        try {
            PaymentEvent event = objectMapper.readValue(message, PaymentEvent.class);
            log.info("Evento de pagamento recebido: orderId={}, status={}", event.orderId(), event.status());

            pedidoRepository.findById(event.orderId()).ifPresent(pedido -> {
                if ("APPROVED".equalsIgnoreCase(event.status())) {
                    pedido.setStatus(StatusPedido.PAGO);
                    pedidoRepository.save(pedido);
                    log.info("Pedido {} marcado como PAGO", pedido.getId());
                } else if ("REJECTED".equalsIgnoreCase(event.status())) {
                    pedido.setStatus(StatusPedido.CANCELADO);
                    // Devolver estoque
                    pedido.getItens().forEach(item ->
                            item.getProduto().adicionarEstoque(item.getQuantidade())
                    );
                    pedidoRepository.save(pedido);
                    log.info("Pedido {} cancelado por pagamento rejeitado", pedido.getId());
                }
            });
        } catch (Exception e) {
            log.error("Erro ao processar evento de pagamento: {}", e.getMessage(), e);
        }
    }
}
