package com.edu.infnet.pb.stickers.Communication;

import com.edu.infnet.pb.stickers.Dto.Communication.CreateTradeDTO;
import com.edu.infnet.pb.stickers.Exception.BusinessRuleException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TradePointConsumerFallbackFactory implements FallbackFactory<TradePointConsumer> {
    private static final Logger log = LogManager.getLogger(TradePointConsumerFallbackFactory.class);

    @Override
    public TradePointConsumer create(Throwable cause) {
        return new TradePointConsumer() {
            @Override
            public List<CreateTradeDTO> getTrocas() {
                log.error("Erro ao buscar pontos de troca: {}", cause.getMessage());
                // Mock de pontos de trocas
                throw new BusinessRuleException(
                        "Serviço de pontos de troca temporariamente indisponível");
            }
        };
    }

}
