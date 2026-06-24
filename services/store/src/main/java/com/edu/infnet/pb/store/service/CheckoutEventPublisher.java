package com.edu.infnet.pb.store.service;

import com.edu.infnet.pb.store.dto.event.PedidoCriadoEvent;

public interface CheckoutEventPublisher {
    void publicarPedidoCriado(PedidoCriadoEvent event);
}
