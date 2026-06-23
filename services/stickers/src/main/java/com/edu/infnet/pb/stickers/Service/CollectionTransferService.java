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
        event.getStickerOferecidoOrigem().forEach(sticker ->
                collectionService.removeQuantity(
                        event.getUsuarioOrigem(),
                        sticker.getStickerId(),
                        sticker.getQuantidadeOferecida()
                )
        );
        event.getStickerOferecidoDestino().forEach(sticker ->
                collectionService.addSticker(
                        event.getUsuarioOrigem(),
                        sticker.getStickerId(),
                        sticker.getQuantidadeOferecida()
                )
        );

        // usuarioDestino perde o que ofereceu, e ganha o que usuarioOrigem ofereceu
        event.getStickerOferecidoDestino().forEach(sticker ->
                collectionService.removeQuantity(
                        event.getUsuarioDestino(),
                        sticker.getStickerId(),
                        sticker.getQuantidadeOferecida()
                )
        );
        event.getStickerOferecidoOrigem().forEach(sticker ->
                collectionService.addSticker(
                        event.getUsuarioDestino(),
                        sticker.getStickerId(),
                        sticker.getQuantidadeOferecida()
                )
        );
    }
}
