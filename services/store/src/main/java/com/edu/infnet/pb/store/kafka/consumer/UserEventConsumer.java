package com.edu.infnet.pb.store.kafka.consumer;

import com.edu.infnet.pb.store.domain.usuario.UserReference;
import com.edu.infnet.pb.store.repository.UserReferenceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserEventConsumer.class);

    private final UserReferenceRepository userReferenceRepository;
    private final ObjectMapper objectMapper;

    public UserEventConsumer(UserReferenceRepository userReferenceRepository,
                             ObjectMapper objectMapper) {
        this.userReferenceRepository = userReferenceRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.user-events}",
            groupId = "store-group"
    )
    @Transactional
    public void onUserEvent(String message) {
        try {
            UserEvent event = objectMapper.readValue(message, UserEvent.class);

            if (event.externalId() == null || event.externalId().isBlank()) {
                log.warn("Evento de usuário ignorado por externalId inválido");
                return;
            }

            UserReference userReference = userReferenceRepository.findByExternalId(event.externalId())
                    .orElseGet(UserReference::new);

            userReference.setExternalId(event.externalId());
            userReference.setNome(event.nome());
            userReference.setEmail(event.email());
            userReference.setAtivo(event.ativo());

            userReferenceRepository.save(userReference);

            log.info("Usuário sincronizado com sucesso: externalId={}, email={}",
                    event.externalId(), event.email());

        } catch (Exception e) {
            log.error("Erro ao processar evento de usuário: {}", e.getMessage(), e);
        }
    }

    public record UserEvent(
            String externalId,
            String nome,
            String email,
            String tipo,
            Boolean ativo
    ) {
    }
}
