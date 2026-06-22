package com.edu.infnet.pb.stickers.Controller;

import java.util.List;
import java.util.UUID;

import com.edu.infnet.pb.stickers.Dto.*;
import com.edu.infnet.pb.stickers.Dto.Communication.TradeMatchResponse;
import com.edu.infnet.pb.stickers.Service.TradeMatchService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.edu.infnet.pb.stickers.Service.StickerCollectionService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/collections")
@RequiredArgsConstructor
public class StickerCollectionController {

  private static final Logger log = LogManager.getLogger(StickerCollectionController.class);

  private final StickerCollectionService collectionService;
  private final TradeMatchService tradeMatchService;

  /**
   * Extrai o userId a partir do "sub" do JWT já validado pelo Spring Security
   * (Resource Server configurado com JwtDecoder). Centralizado aqui para não
   * repetir UUID.fromString(auth.getSubject()) em cada método do controller.
   */
  private UUID currentUserId(Jwt auth) {
    return UUID.fromString(auth.getSubject());
  }

  @GetMapping("/album")
  public ResponseEntity<List<UserCollectionResponse>> findMyCollection(@AuthenticationPrincipal Jwt auth) {
    UUID userId = currentUserId(auth);
    log.info("Buscando coleção do usuário id={}", userId);

    var result = collectionService.findByUserId(userId).stream()
            .map(UserCollectionResponse::fromEntity)
            .toList();
    log.info("Coleção encontrada: {} stickers para usuário id={}", result.size(), userId);
    return ResponseEntity.ok(result);
  }

  @GetMapping("/album/{stickerId}")
  public ResponseEntity<UserCollectionResponse> findByStickerId(
          @AuthenticationPrincipal Jwt auth,
          @PathVariable Long stickerId) {
    UUID userId = currentUserId(auth);
    log.info("Buscando sticker id={} para usuário id={}", stickerId, userId);

    UserCollectionResponse response = UserCollectionResponse.fromEntity(
            collectionService.findByUserIdAndStickerId(userId, stickerId));

    log.info("Sticker id={} encontrada para usuário id={}", stickerId, userId);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/repeated")
  public ResponseEntity<List<UserCollectionResponse>> findRepeated(@AuthenticationPrincipal Jwt auth) {
    UUID userId = currentUserId(auth);
    log.info("Buscando stickers repetidas do usuário id={}", userId);

    var result = collectionService.findRepeated(userId).stream()
            .map(UserCollectionResponse::fromEntity)
            .toList();

    log.info("Encontradas {} stickers repetidas para usuário id={}", result.size(), userId);
    return ResponseEntity.ok(result);
  }

  @GetMapping("/missing")
  public ResponseEntity<List<StickerResponse>> findMissing(@AuthenticationPrincipal Jwt auth) {
    UUID userId = currentUserId(auth);
    log.info("Buscando stickers faltantes do usuário id={}", userId);

    var result = collectionService.findMissing(userId).stream()
            .map(StickerResponse::fromEntity)
            .toList();

    log.info("Encontradas {} stickers faltantes para usuário id={}", result.size(), userId);
    return ResponseEntity.ok(result);
  }

  @GetMapping("/progress")
  public ResponseEntity<AlbumProgressResponse> getAlbumProgress(@AuthenticationPrincipal Jwt auth) {
    UUID userId = currentUserId(auth);
    log.info("Buscando progresso do álbum do usuário id={}", userId);

    long owned = collectionService.countDistinctOwned(userId);
    double progress = collectionService.getAlbumProgressPercentage(userId);

    log.info("Progresso do usuário id={}: {} stickers, {}", userId, owned, progress);
    return ResponseEntity.ok(AlbumProgressResponse.builder()
            .ownedStickers(owned)
            .progressPercentage(progress)
            .build());
  }

  @GetMapping("/album/{stickerId}/available-quantity")
  public ResponseEntity<AvailableQuantityResponse> getAvailableQuantity(
          @AuthenticationPrincipal Jwt auth,
          @PathVariable Long stickerId) {
    UUID userId = currentUserId(auth);
    log.info("Buscando quantidade disponível da sticker id={} para usuário id={}", stickerId, userId);

    int available = collectionService.getAvailableQuantity(userId, stickerId);

    log.info("Quantidade disponível da sticker id={} para usuário id={}: {}", stickerId, userId, available);
    return ResponseEntity.ok(AvailableQuantityResponse.builder()
            .stickerId(stickerId)
            .availableQuantity(available)
            .build());
  }

  @PostMapping("/album")
  public ResponseEntity<UserCollectionResponse> addSticker(
          @AuthenticationPrincipal Jwt auth,
          @Valid @RequestBody AddStickerToCollectionRequest request) {
    UUID userId = currentUserId(auth);
    log.info("Adicionando sticker id={} quantidade={} para usuário id={}",
            request.getStickerId(), request.getQuantity(), userId);

    UserCollectionResponse response = UserCollectionResponse.fromEntity(
            collectionService.addSticker(userId, request.getStickerId(), request.getQuantity()));

    log.info("Sticker id={} adicionada com sucesso para usuário id={}", request.getStickerId(), userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @DeleteMapping("/album/delete/{stickerId}")
  public ResponseEntity<Void> removeSticker(
          @AuthenticationPrincipal Jwt auth,
          @PathVariable Long stickerId,
          @RequestParam(defaultValue = "1") @Min(value = 1, message = "A quantidade deve ser maior que zero") int quantity) {
    UUID userId = currentUserId(auth);
    log.info("Removendo sticker id={} quantidade={} do usuário id={}", stickerId, quantity, userId);

    collectionService.removeQuantity(userId, stickerId, quantity);

    log.info("Sticker id={} removida com sucesso do usuário id={}", stickerId, userId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Calcula, com base em todos os dados já existentes no banco, todos os
   * pares de usuários que podem trocar entre si. Endpoint público: não
   * exige autenticação, pois o cálculo não depende de um usuário logado.
   */
  @GetMapping("/matches")
  public ResponseEntity<List<TradeMatchResponse>> findMatches() {
    log.info("Buscando matches de troca entre todos os usuários");

    List<TradeMatchResponse> matches = tradeMatchService.findMatches();

    log.info("Encontrados {} matches de troca", matches.size());
    return ResponseEntity.ok(matches);
  }
}