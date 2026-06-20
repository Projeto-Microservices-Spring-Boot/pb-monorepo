package com.edu.infnet.pb.stickers.Controller;

import java.util.List;
import java.util.UUID;

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

import com.edu.infnet.pb.stickers.Dto.AddStickerToCollectionRequest;
import com.edu.infnet.pb.stickers.Dto.AlbumProgressResponse;
import com.edu.infnet.pb.stickers.Dto.AvailableQuantityResponse;
import com.edu.infnet.pb.stickers.Dto.StickerResponse;
import com.edu.infnet.pb.stickers.Dto.UserCollectionResponse;
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

    var result = collectionService.findByUserId(userId).stream()
            .map(UserCollectionResponse::fromEntity)
            .toList();

    return ResponseEntity.ok(result);
  }

  @GetMapping("/album/{stickerId}")
  public ResponseEntity<UserCollectionResponse> findByStickerId(
          @AuthenticationPrincipal Jwt auth,
          @PathVariable Long stickerId) {
    UUID userId = currentUserId(auth);

    return ResponseEntity.ok(UserCollectionResponse.fromEntity(
            collectionService.findByUserIdAndStickerId(userId, stickerId)));
  }

  @GetMapping("/repeated")
  public ResponseEntity<List<UserCollectionResponse>> findRepeated(@AuthenticationPrincipal Jwt auth) {
    UUID userId = currentUserId(auth);

    return ResponseEntity.ok(collectionService.findRepeated(userId).stream()
            .map(UserCollectionResponse::fromEntity)
            .toList());
  }

  @GetMapping("/missing")
  public ResponseEntity<List<StickerResponse>> findMissing(@AuthenticationPrincipal Jwt auth) {
    UUID userId = currentUserId(auth);

    return ResponseEntity.ok(collectionService.findMissing(userId).stream()
            .map(StickerResponse::fromEntity)
            .toList());
  }

  @GetMapping("/progress")
  public ResponseEntity<AlbumProgressResponse> getAlbumProgress(@AuthenticationPrincipal Jwt auth) {
    UUID userId = currentUserId(auth);

    return ResponseEntity.ok(AlbumProgressResponse.builder()
            .ownedStickers(collectionService.countDistinctOwned(userId))
            .progressPercentage(collectionService.getAlbumProgressPercentage(userId))
            .build());
  }

  @GetMapping("/album/{stickerId}/available-quantity")
  public ResponseEntity<AvailableQuantityResponse> getAvailableQuantity(
          @AuthenticationPrincipal Jwt auth,
          @PathVariable Long stickerId) {
    UUID userId = currentUserId(auth);

    return ResponseEntity.ok(AvailableQuantityResponse.builder()
            .stickerId(stickerId)
            .availableQuantity(collectionService.getAvailableQuantity(userId, stickerId))
            .build());
  }

  @PostMapping("/album")
  public ResponseEntity<UserCollectionResponse> addSticker(
          @AuthenticationPrincipal Jwt auth,
          @Valid @RequestBody AddStickerToCollectionRequest request) {
    UUID userId = currentUserId(auth);

    return ResponseEntity.status(HttpStatus.CREATED).body(UserCollectionResponse.fromEntity(
            collectionService.addSticker(userId, request.getStickerId(), request.getQuantity())));
  }

  @DeleteMapping("/album/{stickerId}")
  public ResponseEntity<Void> removeSticker(
          @AuthenticationPrincipal Jwt auth,
          @PathVariable Long stickerId,
          @RequestParam(defaultValue = "1") @Min(value = 1, message = "A quantidade deve ser maior que zero") int quantity) {
    UUID userId = currentUserId(auth);

    collectionService.removeQuantity(userId, stickerId, quantity);
    return ResponseEntity.noContent().build();
  }
}