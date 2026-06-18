package com.edu.infnet.pb.store.kafka.event;

public record UserEvent(
        String externalId,
        String nome,
        String email,
        String action  // CREATED, UPDATED, DELETED
) {}
