package com.edu.infnet.pb.store.kafka.consumer;

import com.edu.infnet.pb.store.domain.produto.Produto;
import com.edu.infnet.pb.store.repository.ProdutoRepository;
import com.fasterxml.jackson.databind.JsonNode;
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

    public StockEventConsumer(ProdutoRepository produtoRepository, ObjectMapper objectMapper) {
        this.produtoRepository = produtoRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "stock-updated", groupId = "store-group")
    @Transactional
    public void onStockUpdated(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);
            Long produtoId = json.get("produtoId").asLong();
            int quantidade = json.get("quantidade").asInt();
            String action = json.get("action").asText(); // ADD, SET

            produtoRepository.findById(produtoId).ifPresent(produto -> {
                if ("ADD".equalsIgnoreCase(action)) {
                    produto.adicionarEstoque(quantidade);
                } else if ("SET".equalsIgnoreCase(action)) {
                    produto.setEstoque(quantidade);
                }
                produtoRepository.save(produto);
                log.info("Estoque atualizado: produtoId={}, novoEstoque={}", produtoId, produto.getEstoque());
            });
        } catch (Exception e) {
            log.error("Erro ao processar evento de estoque: {}", e.getMessage(), e);
        }
    }
}
