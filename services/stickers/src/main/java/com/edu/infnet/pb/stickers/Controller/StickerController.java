package com.edu.infnet.pb.stickers.Controller;

import com.edu.infnet.pb.stickers.Dto.StickerRequest;
import com.edu.infnet.pb.stickers.Dto.StickerResponse;
import com.edu.infnet.pb.stickers.Entity.Sticker;
import com.edu.infnet.pb.stickers.Enum.StickerType;
import com.edu.infnet.pb.stickers.Service.StickerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stickers")
@RequiredArgsConstructor
public class StickerController {

    private final StickerService stickerService;

    @GetMapping
    public ResponseEntity<List<StickerResponse>> findAll() {
        return ResponseEntity.ok(stickerService.findAll().stream()
                .map(StickerResponse::fromEntity)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StickerResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(StickerResponse.fromEntity(stickerService.findById(id)));
    }

    @GetMapping("/code/{stickerCode}")
    public ResponseEntity<StickerResponse> findByStickerCode(@PathVariable String stickerCode) {
        return ResponseEntity.ok(StickerResponse.fromEntity(stickerService.findByStickerCode(stickerCode)));
    }

    @GetMapping("/search")
    public ResponseEntity<List<StickerResponse>> search(
            @RequestParam(required = false) String team,
            @RequestParam(required = false) StickerType type,
            @RequestParam(required = false) String stickerCode,
            @RequestParam(required = false) String playerName) {
        return ResponseEntity.ok(stickerService.search(team, type, stickerCode, playerName).stream()
                .map(StickerResponse::fromEntity)
                .toList());
    }

    @PostMapping
    public ResponseEntity<StickerResponse> create(@Valid @RequestBody StickerRequest request) {
        Sticker created = stickerService.create(request.toEntity());
        return ResponseEntity.status(HttpStatus.CREATED).body(StickerResponse.fromEntity(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StickerResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody StickerRequest request) {
        return ResponseEntity.ok(StickerResponse.fromEntity(stickerService.update(id, request.toEntity())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        stickerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
