package com.edu.infnet.pb.stickers.Service;

import com.edu.infnet.pb.stickers.Entity.Sticker;
import com.edu.infnet.pb.stickers.Entity.UserCollection;
import com.edu.infnet.pb.stickers.Entity.UserCollectionId;
import com.edu.infnet.pb.stickers.Exception.BusinessRuleException;
import com.edu.infnet.pb.stickers.Exception.ResourceNotFoundException;
import com.edu.infnet.pb.stickers.Repository.StickerCollectionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StickerCollectionService {
    private static final String CIRCUIT_BREAKER_NAME = "collection";

    private static final Logger log = LogManager.getLogger(StickerCollectionService.class);
    private final StickerCollectionRepository collectionRepository;

    private final StickerService stickerService;



    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackFindByUserId")
    public List<UserCollection> findByUserId(UUID userId) {
        return collectionRepository.findByIdUserId(userId);
    }

    public List<UserCollection> fallbackFindByUserId(UUID userId, Exception e) {
        log.error("Banco instável ao buscar coleção do usuário id={} erro={}", userId, e.getMessage());
        throw new BusinessRuleException("Serviço temporariamente indisponível, tente novamente em instantes");
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackFindByUserIdAndStickerId")
    public UserCollection findByUserIdAndStickerId(UUID userId, Long stickerId) {
        return collectionRepository.findByIdUserIdAndIdStickerId(userId, stickerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuário não possui essa sticker na coleção"));
    }

    public UserCollection fallbackFindByUserIdAndStickerId(UUID userId, Long stickerId, Exception e) {
        log.error("Banco instável ao buscar sticker id={} do usuário id={} erro={}", stickerId, userId, e.getMessage());
        throw new BusinessRuleException("Serviço temporariamente indisponível, tente novamente em instantes");
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackFindRepeated")
    public List<UserCollection> findRepeated(UUID userId) {
        return collectionRepository.findRepeatedByUserId(userId);
    }

    public List<UserCollection> fallbackFindRepeated(UUID userId, Exception e) {
        log.error("Banco instável ao buscar stickers repetidas do usuário id={} erro={}", userId, e.getMessage());
        throw new BusinessRuleException("Serviço temporariamente indisponível, tente novamente em instantes");
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackFindMissing")
    public List<Sticker> findMissing(UUID userId) {
        return collectionRepository.findMissingByUserId(userId);
    }

    public List<Sticker> fallbackFindMissing(UUID userId, Exception e) {
        log.error("Banco instável ao buscar stickers faltantes do usuário id={} erro={}", userId, e.getMessage());
        throw new BusinessRuleException("Serviço temporariamente indisponível, tente novamente em instantes");
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackCountDistinctOwned")
    public long countDistinctOwned(UUID userId) {
        return collectionRepository.countByUserId(userId);
    }

    public long fallbackCountDistinctOwned(UUID userId, Exception e) {
        log.error("Banco instável ao contar stickers do usuário id={} erro={}", userId, e.getMessage());
        throw new BusinessRuleException("Serviço temporariamente indisponível, tente novamente em instantes");
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
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackGetAvailableQuantity")
    public int getAvailableQuantity(UUID userId, Long stickerId) {
        return collectionRepository.findByIdUserIdAndIdStickerId(userId, stickerId)
                .map(UserCollection::getQuantity)
                .orElse(0);
    }

    public int fallbackGetAvailableQuantity(UUID userId, Long stickerId, Exception e) {
        log.error("Banco instável ao buscar quantidade disponível da sticker id={} do usuário id={} erro={}", stickerId, userId, e.getMessage());
        throw new BusinessRuleException("Serviço temporariamente indisponível, tente novamente em instantes");
    }

    /**
     * Adiciona stickers à coleção do usuário.
     * Se o usuário já tiver a sticker, soma a quantidade.
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackAddSticker")
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

    public UserCollection fallbackAddSticker(UUID userId, Long stickerId, int quantityToAdd, Exception e) {
        log.error("Banco instável ao adicionar sticker id={} para usuário id={} erro={}", stickerId, userId, e.getMessage());
        throw new BusinessRuleException("Serviço temporariamente indisponível, tente novamente em instantes");
    }

    /**
     * Remove uma quantidade de stickers da coleção (uso interno, principalmente
     * chamado pelo TradeProposalService ao efetivar uma troca).
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackRemoveQuantity")
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

    public void fallbackRemoveQuantity(UUID userId, Long stickerId, int quantityToRemove, Exception e) {
        log.error("Banco instável ao remover sticker id={} do usuário id={} erro={}", stickerId, userId, e.getMessage());
        throw new BusinessRuleException("Serviço temporariamente indisponível, tente novamente em instantes");
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