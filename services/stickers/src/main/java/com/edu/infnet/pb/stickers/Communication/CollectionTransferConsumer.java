package com.edu.infnet.pb.stickers.Communication;

import com.edu.infnet.pb.stickers.Dto.Communication.CollectionTranferReponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
public class CollectionTransferConsumer {
    private static final Logger log = LogManager.getLogger(CollectionTransferConsumer.class);

    @Retryable(
            retryFor = Exception.class,  // retry para qualquer exceção
            maxAttempts = 3,             // tenta 3 vezes
            backoff = @Backoff(delay = 2000) // espera 2s entre tentativas
    )

    @KafkaListener(topics = "collection-transfer", groupId = "stickers-service")
    public void receive(CollectionTranferReponse response) {
        log.info("Mensagem recebida do geo");



    }
}
