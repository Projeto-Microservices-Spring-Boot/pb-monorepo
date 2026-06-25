//package com.edu.infnet.pb.stickers.Communication;
//
//
//import com.edu.infnet.pb.stickers.Dto.Communication.PropostaAceitaEvent;
//import com.edu.infnet.pb.stickers.Service.CollectionTransferService;
//import lombok.RequiredArgsConstructor;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.stereotype.Service;
//import org.springframework.kafka.support.Acknowledgment;
//
//@Service
//@RequiredArgsConstructor
//public class CollectionTransferConsumer {
//    private static final Logger log = LogManager.getLogger(CollectionTransferConsumer.class);
//    private final CollectionTransferService transferService;
//
//    @KafkaListener(topics = "collection-transfer", containerFactory = "kafkaListenerContainerFactory")
//    public void receive(PropostaAceitaEvent event, Acknowledgment ack) {
//        log.info("Mensagem recebida do geo: propostaId={} usuarioOrigem={} usuarioDestino={}",
//                event.getPropostaId(), event.getUsuarioOrigem(), event.getUsuarioDestino());
//        try{
//            transferService.applyTransfer(event);
//            ack.acknowledge(); // commita e muda offset
//        }catch(Exception e){
//            log.error("Falha ao processar mensagem",e);
//            throw e;
//        }
//
//
//
//    }
//}
