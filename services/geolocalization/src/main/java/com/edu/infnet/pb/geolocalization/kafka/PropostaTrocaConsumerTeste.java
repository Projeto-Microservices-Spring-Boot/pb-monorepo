package com.edu.infnet.pb.geolocalization.kafka;

import com.edu.infnet.pb.geolocalization.dto.kafka.PropostaAceitaEvento;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PropostaTrocaConsumerTeste {

    private static final Logger log = LogManager.getLogger(PropostaTrocaConsumerTeste.class);

    @KafkaListener(topics = "proposta-aceita", groupId = "teste-local")
    public void escutar(PropostaAceitaEvento evento) {
        log.info("✅ MOCK CONSUMER recebeu o evento: {}", evento);
    }
}