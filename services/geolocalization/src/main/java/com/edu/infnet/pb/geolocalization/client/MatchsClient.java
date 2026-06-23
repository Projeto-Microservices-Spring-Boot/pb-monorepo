package com.edu.infnet.pb.geolocalization.client;

import com.edu.infnet.pb.geolocalization.dto.figurinhas.TradeMatchResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "buscar-matches" , url = "http://localhost:8087" , fallback = MatchsClient.class)
public interface MatchsClient {

@GetMapping("/collections/matches")
List<TradeMatchResponseDTO> buscarMatches();
}
