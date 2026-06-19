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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/collections")
@RequiredArgsConstructor
public class StickerCollectionController {
  private static final Logger log = LogManager.getLogger(StickerCollectionController.class);
  private final StickerCollectionService collectionService;

  @GetMapping("/me")
  public ResponseEntity<List<UserCollectionResponse>> findByUserId(@AuthenticationPrincipal Jwt auth,
      HttpServletRequest request) {
    log.info("=== HEADERS DO KONG ===");
    log.info("X-Consumer-Username: {}", request.getHeader("X-Consumer-Username"));
    log.info("X-Credential-Identifier: {}", request.getHeader("X-Credential-Identifier"));
    log.info("X-Consumer-ID: {}", request.getHeader("X-Consumer-ID"));
    log.info("Authorization: {}", request.getHeader("Authorization"));

    UUID userId = UUID.fromString(auth.getSubject());
    var result = collectionService.findByUserId(userId).stream()
        .map(UserCollectionResponse::fromEntity)
        .toList();

    log.info("RESULTADO: ", result);
    return ResponseEntity.ok(result);
  }

  @GetMapping("/{userId}/stickers/{stickerId}")
  public ResponseEntity<UserCollectionResponse> findByUserIdAndStickerId(
      @PathVariable UUID userId,
      @PathVariable Long stickerId) {
    return ResponseEntity.ok(UserCollectionResponse.fromEntity(
        collectionService.findByUserIdAndStickerId(userId, stickerId)));
  }

  @GetMapping("/{userId}/repeated")
  public ResponseEntity<List<UserCollectionResponse>> findRepeated(@PathVariable UUID userId) {
    return ResponseEntity.ok(collectionService.findRepeated(userId).stream()
        .map(UserCollectionResponse::fromEntity)
        .toList());
  }

  @GetMapping("/{userId}/missing")
  public ResponseEntity<List<StickerResponse>> findMissing(@PathVariable UUID userId) {
    return ResponseEntity.ok(collectionService.findMissing(userId).stream()
        .map(StickerResponse::fromEntity)
        .toList());
  }

  @GetMapping("/{userId}/progress")
  public ResponseEntity<AlbumProgressResponse> getAlbumProgress(@PathVariable UUID userId) {
    return ResponseEntity.ok(AlbumProgressResponse.builder()
        .ownedStickers(collectionService.countDistinctOwned(userId))
        .progressPercentage(collectionService.getAlbumProgressPercentage(userId))
        .build());
  }

  @GetMapping("/{userId}/stickers/{stickerId}/available-quantity")
  public ResponseEntity<AvailableQuantityResponse> getAvailableQuantity(
      @PathVariable UUID userId,
      @PathVariable Long stickerId) {
    return ResponseEntity.ok(AvailableQuantityResponse.builder()
        .stickerId(stickerId)
        .availableQuantity(collectionService.getAvailableQuantity(userId, stickerId))
        .build());
  }

  @PostMapping("/{userId}/stickers")
  public ResponseEntity<UserCollectionResponse> addSticker(
      @PathVariable UUID userId,
      @Valid @RequestBody AddStickerToCollectionRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(UserCollectionResponse.fromEntity(
        collectionService.addSticker(userId, request.getStickerId(), request.getQuantity())));
  }

  @DeleteMapping("/{userId}/stickers/{stickerId}")
  public ResponseEntity<Void> removeSticker(
      @PathVariable UUID userId,
      @PathVariable Long stickerId,
      @RequestParam(defaultValue = "1") @Min(value = 1, message = "A quantidade deve ser maior que zero") int quantity) {
    collectionService.removeQuantity(userId, stickerId, quantity);
    return ResponseEntity.noContent().build();
  }
}
