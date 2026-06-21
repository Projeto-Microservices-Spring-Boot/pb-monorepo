package com.edu.infnet.pb.stickers.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * DTO de entrada do endpoint POST /trades. Não contém proposerId de propósito:
 * quem propõe a troca é sempre o usuário autenticado (extraído do JWT no
 * Controller), nunca um valor enviado pelo cliente no body.
 */
@Getter
@Setter
public class CreateTradeProposalRequest {

    private UUID receiverId;
    private String message;
    private List<TradeItemDTO> offeredStickers;
    private List<TradeItemDTO> requestedStickers;
}