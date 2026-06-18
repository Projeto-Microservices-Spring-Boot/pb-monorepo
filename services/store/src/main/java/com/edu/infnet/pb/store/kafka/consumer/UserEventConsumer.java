package com.edu.infnet.pb.store.kafka.consumer;

import com.edu.infnet.pb.store.domain.usuario.UserReference;
import com.edu.infnet.pb.store.kafka.event.UserEvent;
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

    public UserEventConsumer(UserReferenceRepository userReferenceRepository, ObjectMapper objectMapper) {
        this.userReferenceRepository = userReferenceRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "user.events", groupId = "store-group")
    @Transactional
    public void onUserEvent(String message) {
        try {
            UserEvent event = objectMapper.readValue(message, UserEvent.class);
            log.info("Evento de usuário recebido: externalId={}, action={}", event.externalId(), event.action());

            switch (event.action().toUpperCase()) {
                case "CREATED", "UPDATED" -> {
                    UserReference userRef = userReferenceRepository
                            .findByExternalId(event.externalId())
                            .orElse(new UserReference(event.externalId(), event.nome(), event.email()));

                    userRef.setNome(event.nome());
                    userRef.setEmail(event.email());
                    userRef.setAtivo(true);
                    userReferenceRepository.save(userRef);
                    log.info("UserReference sincronizado: {}", event.externalId());
                }
                case "DELETED" -> {
                    userReferenceRepository.findByExternalId(event.externalId())
                            .ifPresent(userRef -> {
                                userRef.setAtivo(false);
                                userReferenceRepository.save(userRef);
                                log.info("UserReference desativado: {}", event.externalId());
                            });
                }
                default -> log.warn("Ação desconhecida para evento de usuário: {}", event.action());
            }
        } catch (Exception e) {
            log.error("Erro ao processar evento de usuário: {}", e.getMessage(), e);
        }
    }
}
