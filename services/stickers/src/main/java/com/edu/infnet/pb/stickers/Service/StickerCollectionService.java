package com.edu.infnet.pb.stickers.Service;

import com.edu.infnet.pb.stickers.Entity.Sticker;
import com.edu.infnet.pb.stickers.Entity.UserCollection;
import com.edu.infnet.pb.stickers.Entity.UserCollectionId;
import com.edu.infnet.pb.stickers.Exception.BusinessRuleException;
import com.edu.infnet.pb.stickers.Exception.ResourceNotFoundException;
import com.edu.infnet.pb.stickers.Repository.StickerCollectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StickerCollectionService {

    private final StickerCollectionRepository collectionRepository;

    private final StickerService stickerService;

    public List<UserCollection> findByUserId(UUID userId) {
        return collectionRepository.findByIdUserId(userId);
    }

    public UserCollection findByUserIdAndStickerId(UUID userId, Long stickerId) {
        return collectionRepository.findByIdUserIdAndIdStickerId(userId, stickerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuário não possui essa sticker na coleção"));
    }

    public List<UserCollection> findRepeated(UUID userId) {
        return collectionRepository.findRepeatedByUserId(userId);
    }

    public List<Sticker> findMissing(UUID userId) {
        return collectionRepository.findMissingByUserId(userId);
    }

    public long countDistinctOwned(UUID userId) {
        return collectionRepository.countByUserId(userId);
    }

    /**
     * Progresso do álbum em porcentagem: quantas stickers distintas o
     * usuário já possui em relação ao total de stickers que existem no
     * sistema. Ex.: 150 de 300 = 50.0
     */
    public double getAlbumProgressPercentage(UUID userId) {
        long totalStickers = stickerService.findAll().size();
        if (totalStickers == 0) {
            return 0.0;
        }
        long owned = countDistinctOwned(userId);
        return (owned * 100.0) / totalStickers;
    }

    /**
     * Quantidade disponível de uma sticker que o usuário possui na coleção.
     */
    public int getAvailableQuantity(UUID userId, Long stickerId) {
        return collectionRepository.findByIdUserIdAndIdStickerId(userId, stickerId)
                .map(UserCollection::getQuantity)
                .orElse(0);
    }

    /**
     * Adiciona stickers à coleção do usuário.
     * Se o usuário já tiver a sticker, soma a quantidade.
     */
    @Transactional
    public UserCollection addSticker(UUID userId, Long stickerId, int quantityToAdd) {
        if (quantityToAdd <= 0) {
            throw new BusinessRuleException("Quantidade a adicionar deve ser maior que zero");
        }

        Sticker sticker = stickerService.findById(stickerId);

        UserCollection collection = collectionRepository
                .findByIdUserIdAndIdStickerId(userId, stickerId)
                .orElseGet(() -> UserCollection.builder()
                        .id(new UserCollectionId(userId, stickerId))
                        .sticker(sticker)
                        .quantity(0)
                        .build());

        collection.setQuantity(collection.getQuantity() + quantityToAdd);
        return collectionRepository.save(collection);
    }

    /**
     * Remove uma quantidade de stickers da coleção (uso interno, principalmente
     * chamado pelo TradeProposalService ao efetivar uma troca).
     */
    @Transactional
    public void removeQuantity(UUID userId, Long stickerId, int quantityToRemove) {
        UserCollection collection = findByUserIdAndStickerId(userId, stickerId);

        if (collection.getQuantity() < quantityToRemove) {
            throw new BusinessRuleException(
                    "Usuário não possui quantidade suficiente da sticker id=" + stickerId);
        }

        int remaining = collection.getQuantity() - quantityToRemove;

        if (remaining == 0) {
            collectionRepository.delete(collection);
        } else {
            collection.setQuantity(remaining);
            collectionRepository.save(collection);
        }
    }

    /**
     * Valida (sem alterar nada) se o usuário tem quantidade LIVRE suficiente
     * de uma sticker. Usado antes de criar/aceitar uma proposta de troca.
     */
    public void validateAvailableQuantity(UUID userId, Long stickerId, int requiredQuantity) {
        int available = getAvailableQuantity(userId, stickerId);
        if (available < requiredQuantity) {
            throw new BusinessRuleException(
                    "Usuário não possui quantidade livre suficiente da sticker id=" + stickerId
                            + " (disponível: " + available + ", necessário: " + requiredQuantity + ")");
        }
    }
}