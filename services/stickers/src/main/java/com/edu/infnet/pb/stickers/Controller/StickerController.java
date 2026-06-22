package com.edu.infnet.pb.stickers.Controller;

import com.edu.infnet.pb.stickers.Dto.StickerRequest;
import com.edu.infnet.pb.stickers.Dto.StickerResponse;
import com.edu.infnet.pb.stickers.Entity.Sticker;
import com.edu.infnet.pb.stickers.Enum.StickerType;
import com.edu.infnet.pb.stickers.Service.StickerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stickers")
@RequiredArgsConstructor
public class StickerController {
    private static final Logger log = LogManager.getLogger(StickerController.class);
    private final StickerService stickerService;

    @GetMapping
    public ResponseEntity<List<StickerResponse>> findAll() {
        log.info("Buscando todas as stickers");

        var result = stickerService.findAll().stream()
                .map(StickerResponse::fromEntity)
                .toList();

        log.info("Encontradas {} stickers", result.size());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StickerResponse> findById(@PathVariable Long id) {
        log.info("Buscando sticker id={}", id);

        StickerResponse response = StickerResponse.fromEntity(stickerService.findById(id));

        log.info("Sticker id={} encontrada", id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/code/{stickerCode}")
    public ResponseEntity<StickerResponse> findByStickerCode(@PathVariable String stickerCode) {
        log.info("Buscando sticker com código={}", stickerCode);

        StickerResponse response = StickerResponse.fromEntity(stickerService.findByStickerCode(stickerCode));

        log.info("Sticker com código={} encontrada", stickerCode);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<List<StickerResponse>> search(
            @RequestParam(required = false) String team,
            @RequestParam(required = false) StickerType type,
            @RequestParam(required = false) String stickerCode,
            @RequestParam(required = false) String playerName) {
        log.info("Buscando stickers com filtros: team={} type={} stickerCode={} playerName={}",
                team, type, stickerCode, playerName);

        var result = stickerService.search(team, type, stickerCode, playerName).stream()
                .map(StickerResponse::fromEntity)
                .toList();

        log.info("Encontradas {} stickers com os filtros aplicados", result.size());
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<StickerResponse> create(@Valid @RequestBody StickerRequest request) {
        log.info("Criando sticker com código={}", request.getStickerCode());

        Sticker created = stickerService.create(request.toEntity());
        StickerResponse response = StickerResponse.fromEntity(created);

        log.info("Sticker criada com sucesso id={} código={}", response.getId(), response.getStickerCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StickerResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody StickerRequest request) {
        log.info("Atualizando sticker id={}", id);

        StickerResponse response = StickerResponse.fromEntity(stickerService.update(id, request.toEntity()));

        log.info("Sticker id={} atualizada com sucesso", id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("Deletando sticker id={}", id);

        stickerService.delete(id);

        log.info("Sticker id={} deletada com sucesso", id);
        return ResponseEntity.noContent().build();
    }
}
