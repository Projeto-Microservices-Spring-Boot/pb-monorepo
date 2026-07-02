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

    public PaymentEventConsumer(PedidoRepository pedidoRepository,
                                ObjectMapper objectMapper) {
        this.pedidoRepository = pedidoRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.payment-confirmed}",
            groupId = "store-group"
    )
    @Transactional
    public void onPaymentConfirmed(String message) {
        try {
            PaymentEvent event = objectMapper.readValue(message, PaymentEvent.class);
            log.info("Evento de pagamento recebido: orderId={}, paymentId={}, status={}",
                    event.orderId(), event.paymentId(), event.status());

            Pedido pedido = pedidoRepository.findById(event.orderId())
                    .orElse(null);

            if (pedido == null) {
                log.warn("Pedido não encontrado para evento de pagamento: orderId={}", event.orderId());
                return;
            }

            StatusPedido statusAnterior = pedido.getStatus();

            if ("APPROVED".equalsIgnoreCase(event.status())) {
                if (pedido.getStatus() == StatusPedido.CANCELADO) {
                    log.warn("Pagamento aprovado ignorado para pedido já cancelado: orderId={}", pedido.getId());
                    return;
                }

                if (pedido.getStatus() != StatusPedido.PAGO) {
                    pedido.setStatus(StatusPedido.PAGO);
                    pedidoRepository.save(pedido);

                    log.info("Pedido {} marcado como PAGO (status anterior: {})",
                            pedido.getId(), statusAnterior);
                } else {
                    log.info("Pedido {} já estava como PAGO", pedido.getId());
                }

                return;
            }

            if ("REJECTED".equalsIgnoreCase(event.status())) {
                if (pedido.getStatus() == StatusPedido.CANCELADO) {
                    log.info("Pedido {} já estava cancelado; evento rejeitado ignorado", pedido.getId());
                    return;
                }

                if (pedido.getStatus() == StatusPedido.ENTREGUE) {
                    log.warn("Pagamento rejeitado ignorado para pedido já entregue: orderId={}", pedido.getId());
                    return;
                }

                pedido.setStatus(StatusPedido.CANCELADO);

                pedido.getItens().forEach(item ->
                        item.getProduto().adicionarEstoque(item.getQuantidade())
                );

                pedidoRepository.save(pedido);

                log.info("Pedido {} cancelado por pagamento rejeitado (status anterior: {})",
                        pedido.getId(), statusAnterior);
                return;
            }

            log.warn("Status de pagamento desconhecido recebido: {}", event.status());

        } catch (Exception e) {
            log.error("Erro ao processar evento de pagamento: {}", e.getMessage(), e);
        }
    }
}
