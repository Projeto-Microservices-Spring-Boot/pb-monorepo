package com.edu.infnet.pb.store.service;

import com.edu.infnet.pb.store.kafka.event.OrderCreatedEvent;

public interface CheckoutEventPublisher {
    void publicarPedidoCriado(OrderCreatedEvent event);
}
