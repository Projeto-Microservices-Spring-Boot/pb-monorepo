package com.edu.infnet.pb.stickers.Dto.Communication;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;
@Getter
@Builder
public class TradeMatchResponse {

    // Id de um dos usuários do par de troca
    private UUID userIdA;

    // Id do outro usuário do par de troca
    private UUID userIdB;

    // Soma de userACanOffer + userBCanOffer; usado para ordenar os matches do maior para o menor potencial de troca
    private int score;

    // Quantidade de stickers repetidas que o usuário A pode oferecer ao usuário B
    private int userACanOffer;

    // Quantidade de stickers repetidas que o usuário B pode oferecer ao usuário A
    private int userBCanOffer;

    // Detalhe (id e nome do jogador) das stickers que o usuário B possui repetida e que o usuário A não possui
    private List<StickerTradeItem> stickersUserBHasThatUserANeeds;

    // Detalhe (id e nome do jogador) das stickers que o usuário A possui repetida e que o usuário B não possui
    private List<StickerTradeItem> stickersUserAHasThatUserBNeeds;
}