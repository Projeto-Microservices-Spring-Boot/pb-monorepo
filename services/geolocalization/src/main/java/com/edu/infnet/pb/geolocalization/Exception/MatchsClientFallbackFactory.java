package com.edu.infnet.pb.geolocalization.Exception;

import com.edu.infnet.pb.geolocalization.Mock.MatchsClientMock;
import com.edu.infnet.pb.geolocalization.client.MatchsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class MatchsClientFallbackFactory implements FallbackFactory<MatchsClient> {

    private static final Logger log = LoggerFactory.getLogger(MatchsClientFallbackFactory.class);

    @Override
    public MatchsClient create(Throwable cause) {
        log.warn("Falha ao chamar buscar-matches, usando mock. Motivo: {}", cause.getMessage());
        return new MatchsClientMock();
    }
}