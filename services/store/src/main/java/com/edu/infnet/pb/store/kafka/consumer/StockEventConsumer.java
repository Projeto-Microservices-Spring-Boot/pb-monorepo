package com.edu.infnet.pb.store.kafka.consumer;

import com.edu.infnet.pb.store.domain.produto.Produto;
import com.edu.infnet.pb.store.repository.ProdutoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class StockEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(StockEventConsumer.class);

    private final ProdutoRepository produtoRepository;
    private final ObjectMapper objectMapper;

    public StockEventConsumer(ProdutoRepository produtoRepository,
                              ObjectMapper objectMapper) {
        this.produtoRepository = produtoRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.stock-updated}",
            groupId = "store-group"
    )
    @Transactional
    public void onStockUpdated(String message) {
        try {
            StockEvent event = objectMapper.readValue(message, StockEvent.class);

            if (event.produtoId() == null) {
                log.warn("Evento de estoque ignorado: produtoId nulo");
                return;
            }

            if (event.quantidade() == null || event.quantidade() < 0) {
                log.warn("Evento de estoque ignorado: quantidade inválida para produtoId={}", event.produtoId());
                return;
            }

            Produto produto = produtoRepository.findById(event.produtoId()).orElse(null);

            if (produto == null) {
                log.warn("Produto não encontrado para evento de estoque: produtoId={}", event.produtoId());
                return;
            }

            String tipo = event.tipo() == null ? "SET" : event.tipo().trim().toUpperCase();

            switch (tipo) {
                case "IN" -> produto.adicionarEstoque(event.quantidade());
                case "OUT" -> {
                    if (produto.getEstoque() < event.quantidade()) {
                        log.warn("Saída de estoque ignorada por saldo insuficiente: produtoId={}, estoqueAtual={}, saída={}",
                                produto.getId(), produto.getEstoque(), event.quantidade());
                        return;
                    }
                    produto.reduzirEstoque(event.quantidade());
                }
                case "SET" -> produto.setEstoque(event.quantidade());
                default -> {
                    log.warn("Tipo de evento de estoque desconhecido: {}", tipo);
                    return;
                }
            }

            produtoRepository.save(produto);

            log.info("Estoque atualizado com sucesso: produtoId={}, tipo={}, quantidade={}, estoqueFinal={}",
                    produto.getId(), tipo, event.quantidade(), produto.getEstoque());

        } catch (Exception e) {
            log.error("Erro ao processar evento de estoque: {}", e.getMessage(), e);
        }
    }

    public record StockEvent(
            Long produtoId,
            Integer quantidade,
            String tipo
    ) {
    }
}
