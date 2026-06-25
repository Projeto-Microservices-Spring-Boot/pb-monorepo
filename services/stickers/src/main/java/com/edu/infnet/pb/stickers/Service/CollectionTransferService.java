package com.edu.infnet.pb.stickers.Service;

import com.edu.infnet.pb.stickers.Dto.Communication.PropostaAceitaEvent;
import com.edu.infnet.pb.stickers.Entity.TradeItem;
import com.edu.infnet.pb.stickers.Entity.TradeProposal;
import com.edu.infnet.pb.stickers.Enum.TradeSide;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
public class CollectionTransferService {
    private final StickerCollectionService collectionService;




    /**
     * Aplica a transferência de stickers entre os dois usuários da proposta.
     * - Origem perde o que ofereceu (ORIGEM) e ganha o que destino ofereceu (DESTINO)
     * - Destino perde o que ofereceu (DESTINO) e ganha o que origem ofereceu (ORIGEM)
     */
    @Transactional
    public void applyTransfer(TradeProposal proposal) {
        for (TradeItem item : proposal.getItems()) {
            if (item.getSide().equals(TradeSide.ORIGEM)) {
                // Origem perde, destino ganha
                collectionService.removeQuantity(
                        proposal.getUsuarioOrigem(),
                        item.getSticker().getId(),
                        item.getQuantity()
                );
                collectionService.addSticker(
                        proposal.getUsuarioDestino(),
                        item.getSticker().getId(),
                        item.getQuantity()
                );
            } else {
                // Destino perde, origem ganha
                collectionService.removeQuantity(
                        proposal.getUsuarioDestino(),
                        item.getSticker().getId(),
                        item.getQuantity()
                );
                collectionService.addSticker(
                        proposal.getUsuarioOrigem(),
                        item.getSticker().getId(),
                        item.getQuantity()
                );
            }
        }
    }
}
