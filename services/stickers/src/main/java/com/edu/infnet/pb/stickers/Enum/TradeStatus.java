package com.edu.infnet.pb.stickers.Enum;

public enum TradeStatus {
    PENDING,    // proposta enviada, aguardando resposta do receptor
    ACCEPTED,   // receptor aceitou, encontro presencial marcado (figurinhas ainda NÃO mudam de dono)
    COMPLETED,  // encontro confirmado por um dos usuários, figurinhas trocaram de dono
    REJECTED,   // receptor recusou a proposta
    CANCELLED,  // proponente cancelou (estando PENDING) ou alguém cancelou o encontro (estando ACCEPTED)
    EXPIRED     // prazo esgotado sem resposta/confirmação
}
