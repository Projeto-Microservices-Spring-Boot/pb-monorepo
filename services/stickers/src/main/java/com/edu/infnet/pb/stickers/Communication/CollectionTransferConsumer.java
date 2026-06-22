package com.edu.infnet.pb.stickers.Communication;

import com.edu.infnet.pb.stickers.Dto.Communication.CollectionTranferReponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.kafka.support.Acknowledgment;

@Service
public class CollectionTransferConsumer {
    private static final Logger log = LogManager.getLogger(CollectionTransferConsumer.class);

    @KafkaListener(topics = "collection-transfer", groupId = "stickers-service", containerFactory = "kafkaListenerContainerFactory")
    public void receive(CollectionTranferReponse response, Acknowledgment ack) {
        log.info("Mensagem recebida do geo");
        try{



            ack.acknowledge(); // commita e muda offset
        }catch(Exception e){
            log.error("Falha ao processar mensagem",e);

            throw e;
        }



    }
}
