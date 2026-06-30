package com.edu.infnet.pb.stickers.Service;

import com.edu.infnet.pb.stickers.Entity.Sticker;
import com.edu.infnet.pb.stickers.Enum.StickerType;
import com.edu.infnet.pb.stickers.Exception.BusinessRuleException;
import com.edu.infnet.pb.stickers.Exception.ResourceNotFoundException;
import com.edu.infnet.pb.stickers.Repository.StickerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StickerService {

    private final StickerRepository stickerRepository;

    public Sticker findById(Long id) {
        return stickerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sticker não encontrada: id=" + id));
    }

    public Sticker findByStickerCode(String stickerCode) {
        return stickerRepository.findByStickerCode(stickerCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sticker não encontrada: code=" + stickerCode));
    }

    public List<Sticker> findByTeam(String team) {
        return stickerRepository.findByTeam(team);
    }

    public List<Sticker> findByType(StickerType type) {
        return stickerRepository.findByType(type);
    }

    public List<Sticker> findByPlayerName(String name) {
        return stickerRepository.findByNameContainingIgnoreCase(name);
    }

    public List<Sticker> search(String team, StickerType type, String stickerCode, String playerName) {
        return stickerRepository.search(team, type, stickerCode, playerName);
    }

    public List<Sticker> findAll() {
        return stickerRepository.findAll();
    }

    @Transactional
    public Sticker create(Sticker sticker) {
        stickerRepository.findByStickerCode(sticker.getStickerCode())
                .ifPresent(s -> {
                    throw new BusinessRuleException(
                            "Já existe uma sticker com o código: " + sticker.getStickerCode());
                });
        return stickerRepository.save(sticker);
    }

    @Transactional
    public Sticker update(Long id, Sticker updated) {
        Sticker existing = findById(id);
        existing.setName(updated.getName());
        existing.setTeam(updated.getTeam());
        existing.setType(updated.getType());
        // stickerCode não é alterado de propósito: é a chave de negócio (única)
        return stickerRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        Sticker sticker = findById(id);
        stickerRepository.delete(sticker);
    }
}