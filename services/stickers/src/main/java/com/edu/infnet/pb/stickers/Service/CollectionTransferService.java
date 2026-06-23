package com.edu.infnet.pb.stickers.Service;

import com.edu.infnet.pb.stickers.Dto.Communication.PropostaAceitaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
public class CollectionTransferService {
    private final StickerCollectionService collectionService;

    @Transactional
    public void applyTransfer(PropostaAceitaEvent event) {
        // usuarioOrigem perde o que ofereceu, e ganha o que usuarioDestino ofereceu
        collectionService.removeQuantity(
                event.getUsuarioOrigem(),
                event.getStickerOferecidoOrigem(),
                event.getQuantidadeOferecidaOrigem()
        );
        collectionService.addSticker(
                event.getUsuarioOrigem(),
                event.getStickerOferecidoDestino(),
                event.getQuantidadeOferecidaDestino()
        );

        // usuarioDestino perde o que ofereceu, e ganha o que usuarioOrigem ofereceu
        collectionService.removeQuantity(
                event.getUsuarioDestino(),
                event.getStickerOferecidoDestino(),
                event.getQuantidadeOferecidaDestino()
        );
        collectionService.addSticker(
                event.getUsuarioDestino(),
                event.getStickerOferecidoOrigem(),
                event.getQuantidadeOferecidaOrigem()
        );
    }
}

