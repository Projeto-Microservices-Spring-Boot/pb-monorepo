package com.edu.infnet.pb.geolocalization.kafka;

import com.edu.infnet.pb.geolocalization.dto.kafka.PropostaAceitaEvento;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class PropostaTrocaProducer {

    private static final Logger log = LogManager.getLogger(PropostaTrocaProducer.class);
    private static final String TOPIC = "collection-transfer";

    private final KafkaTemplate<String, PropostaAceitaEvento> kafkaTemplate;

    public void enviarPropostaAceita(PropostaAceitaEvento evento) {
        log.info("Enviando evento de proposta aceita id={} origem={} destino={}",
                evento.propostaId(), evento.usuarioOrigem(), evento.usuarioDestino());

        kafkaTemplate.send(TOPIC, evento.propostaId().toString(), evento);
    }

}
