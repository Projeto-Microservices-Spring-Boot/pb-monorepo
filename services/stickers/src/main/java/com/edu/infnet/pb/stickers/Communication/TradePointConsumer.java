package com.edu.infnet.pb.stickers.Communication;

import com.edu.infnet.pb.stickers.Dto.Communication.CreateTradeDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name="geolocalization", fallbackFactory = TradePointConsumerFallbackFactory.class) // Name do Service no eureka
public interface TradePointConsumer {
    @GetMapping("/maps/trocas")
    List<CreateTradeDTO> getTrocas();



}
