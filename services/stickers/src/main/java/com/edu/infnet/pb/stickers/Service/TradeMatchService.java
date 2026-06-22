package com.edu.infnet.pb.stickers.Service;

import com.edu.infnet.pb.stickers.Dto.Communication.TradeMatchResponse;
import com.edu.infnet.pb.stickers.Repository.StickerCollectionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;





import com.edu.infnet.pb.stickers.Dto.Communication.StickerTradeItem;

import com.edu.infnet.pb.stickers.Exception.BusinessRuleException;




import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TradeMatchService {

    private static final String CIRCUIT_BREAKER_NAME = "collection";

    private static final Logger log = LogManager.getLogger(TradeMatchService.class);

    private final StickerCollectionRepository collectionRepository;

    /**
     * Calcula, com base em TODOS os dados já existentes no banco, todos os
     * pares de usuários que podem fazer uma troca entre si: o usuário A tem
     * uma sticker repetida que o usuário B precisa, E o usuário B tem uma
     * sticker repetida que o usuário A precisa.
     *
     * Não recebe nenhum usuário como entrada — o cálculo é feito para o
     * sistema inteiro, cruzando a coleção de cada usuário com a de todos
     * os outros.
     *
     * Ordenado pelo score (soma do que cada lado pode oferecer), do maior
     * para o menor.
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackFindMatches")
    public List<TradeMatchResponse> findMatches() {

        List<Object[]> rows = collectionRepository.findAllCollectionsForMatching();

        // Monta, por usuário: o que ele possui (qualquer quantidade) e o que
        // ele tem repetido (quantity > 1, pois a 1ª unidade fica reservada
        // para o álbum), além do nome de cada sticker para o detalhamento.
        Map<UUID, Set<Long>> ownedByUser = new HashMap<>();
        Map<UUID, Set<Long>> repeatedByUser = new HashMap<>();
        Map<Long, String> playerNameByStickerId = new HashMap<>();

        for (Object[] row : rows) {
            UUID userId = (UUID) row[0];
            Long stickerId = (Long) row[1];
            Integer quantity = (Integer) row[2];
            String playerName = (String) row[3];

            ownedByUser.computeIfAbsent(userId, k -> new HashSet<>()).add(stickerId);
            playerNameByStickerId.putIfAbsent(stickerId, playerName);

            if (quantity != null && quantity > 1) {
                repeatedByUser.computeIfAbsent(userId, k -> new HashSet<>()).add(stickerId);
            }
        }

        List<UUID> allUserIds = new ArrayList<>(ownedByUser.keySet());
        List<TradeMatchResponse> matches = new ArrayList<>();

        // Compara cada par de usuários uma única vez (i < j evita duplicar A-B e B-A)
        for (int i = 0; i < allUserIds.size(); i++) {
            for (int j = i + 1; j < allUserIds.size(); j++) {
                UUID userIdA = allUserIds.get(i);
                UUID userIdB = allUserIds.get(j);

                List<StickerTradeItem> whatBOffersToA =
                        findOffers(repeatedByUser.get(userIdB), ownedByUser.get(userIdA), playerNameByStickerId);

                List<StickerTradeItem> whatAOffersToB =
                        findOffers(repeatedByUser.get(userIdA), ownedByUser.get(userIdB), playerNameByStickerId);

                if (!whatBOffersToA.isEmpty() && !whatAOffersToB.isEmpty()) {
                    int userACanOffer = whatAOffersToB.size();
                    int userBCanOffer = whatBOffersToA.size();
                    int score = userACanOffer + userBCanOffer;

                    matches.add(TradeMatchResponse.builder()
                            .userIdA(userIdA)
                            .userIdB(userIdB)
                            .score(score)
                            .userACanOffer(userACanOffer)
                            .userBCanOffer(userBCanOffer)
                            .stickersUserBHasThatUserANeeds(whatBOffersToA)
                            .build());
                }
            }
        }

        return matches.stream()
                .sorted(Comparator.comparing(TradeMatchResponse::getScore).reversed())
                .toList();
    }

    /**
     * Dado o conjunto de stickers repetidas de um usuário e o conjunto de
     * stickers que o outro usuário já possui, retorna o detalhe das
     * stickers que podem ser oferecidas (repetidas e que o outro não tem).
     */
    private List<StickerTradeItem> findOffers(
            Set<Long> repeatedStickerIds, Set<Long> ownedByOther, Map<Long, String> playerNameByStickerId) {

        if (repeatedStickerIds == null || repeatedStickerIds.isEmpty()) {
            return List.of();
        }

        Set<Long> theyAlreadyOwn = ownedByOther != null ? ownedByOther : Set.of();

        List<StickerTradeItem> offers = new ArrayList<>();
        for (Long stickerId : repeatedStickerIds) {
            if (!theyAlreadyOwn.contains(stickerId)) {
                offers.add(StickerTradeItem.builder()
                        .stickerId(stickerId)
                        .playerName(playerNameByStickerId.get(stickerId))
                        .build());
            }
        }
        return offers;
    }

    public List<TradeMatchResponse> fallbackFindMatches(Exception e) {
        log.error("Banco instável ao buscar matches de troca erro={}", e.getMessage());
        throw new BusinessRuleException("Serviço temporariamente indisponível, tente novamente em instantes");
    }
}



