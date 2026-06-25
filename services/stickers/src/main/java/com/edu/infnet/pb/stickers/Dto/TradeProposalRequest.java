package com.edu.infnet.pb.stickers.Dto;



import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class TradeProposalRequest {


    private UUID usuarioDestino;


    private List<TradeItemDTO> itensOrigem;   // o que quem propõe oferece


    private List<TradeItemDTO> itensDestino;  // o que quem propõe quer receber
}
