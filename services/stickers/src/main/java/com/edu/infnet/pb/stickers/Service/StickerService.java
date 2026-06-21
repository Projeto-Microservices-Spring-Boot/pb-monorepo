package com.edu.infnet.pb.stickers.Service;

import com.edu.infnet.pb.stickers.Entity.Sticker;
import com.edu.infnet.pb.stickers.Enum.StickerType;
import com.edu.infnet.pb.stickers.Exception.BusinessRuleException;
import com.edu.infnet.pb.stickers.Exception.ResourceNotFoundException;
import com.edu.infnet.pb.stickers.Repository.StickerRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StickerService {
    private static final Logger log = LogManager.getLogger(StickerService.class);
    private static final String CIRCUIT_BREAKER_NAME = "sticker";
    private final StickerRepository stickerRepository;

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackFindById")
    public Sticker findById(Long id) {
        return stickerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sticker não encontrada: id=" + id));
    }

    public Sticker fallbackFindById(Long id, Exception e) {
        log.error("Banco instável ao buscar sticker id={} erro={}", id, e.getMessage());
        throw new BusinessRuleException("Serviço temporariamente indisponível, tente novamente em instantes");
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackFindByStickerCode")
    public Sticker findByStickerCode(String stickerCode) {
        return stickerRepository.findByStickerCode(stickerCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sticker não encontrada: code=" + stickerCode));
    }

    public Sticker fallbackFindByStickerCode(String stickerCode, Exception e) {
        log.error("Banco instável ao buscar sticker code={} erro={}", stickerCode, e.getMessage());
        throw new BusinessRuleException("Serviço temporariamente indisponível, tente novamente em instantes");
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackFindByTeam")
    public List<Sticker> findByTeam(String team) {
        return stickerRepository.findByTeam(team);
    }

    public List<Sticker> fallbackFindByTeam(String team, Exception e) {
        log.error("Banco instável ao buscar stickers do time={} erro={}", team, e.getMessage());
        throw new BusinessRuleException("Serviço temporariamente indisponível, tente novamente em instantes");
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackFindByType")
    public List<Sticker> findByType(StickerType type) {
        return stickerRepository.findByType(type);
    }

    public List<Sticker> fallbackFindByType(StickerType type, Exception e) {
        log.error("Banco instável ao buscar stickers do tipo={} erro={}", type, e.getMessage());
        throw new BusinessRuleException("Serviço temporariamente indisponível, tente novamente em instantes");
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackFindByPlayerName")
    public List<Sticker> findByPlayerName(String name) {
        return stickerRepository.findByNameContainingIgnoreCase(name);
    }

    public List<Sticker> fallbackFindByPlayerName(String name, Exception e) {
        log.error("Banco instável ao buscar stickers do jogador={} erro={}", name, e.getMessage());
        throw new BusinessRuleException("Serviço temporariamente indisponível, tente novamente em instantes");
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackSearch")
    public List<Sticker> search(String team, StickerType type, String stickerCode, String playerName) {
        return stickerRepository.search(team, type, stickerCode, playerName);
    }

    public List<Sticker> fallbackSearch(String team, StickerType type, String stickerCode, String playerName, Exception e) {
        log.error("Banco instável ao buscar stickers com filtros erro={}", e.getMessage());
        throw new BusinessRuleException("Serviço temporariamente indisponível, tente novamente em instantes");
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackFindAll")
    public List<Sticker> findAll() {
        return stickerRepository.findAll();
    }

    public List<Sticker> fallbackFindAll(Exception e) {
        log.error("Banco instável ao buscar todas as stickers erro={}", e.getMessage());
        throw new BusinessRuleException("Serviço temporariamente indisponível, tente novamente em instantes");
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackCreate")
    @Transactional
    public Sticker create(Sticker sticker) {
        stickerRepository.findByStickerCode(sticker.getStickerCode())
                .ifPresent(s -> {
                    throw new BusinessRuleException(
                            "Já existe uma sticker com o código: " + sticker.getStickerCode());
                });
        return stickerRepository.save(sticker);
    }

    public Sticker fallbackCreate(Sticker sticker, Exception e) {
        log.error("Banco instável ao criar sticker code={} erro={}", sticker.getStickerCode(), e.getMessage());
        throw new BusinessRuleException("Serviço temporariamente indisponível, tente novamente em instantes");
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackUpdate")
    @Transactional
    public Sticker update(Long id, Sticker updated) {
        Sticker existing = findById(id);
        existing.setName(updated.getName());
        existing.setTeam(updated.getTeam());
        existing.setType(updated.getType());
        // stickerCode não é alterado de propósito: é a chave de negócio (única)
        return stickerRepository.save(existing);
    }

    public Sticker fallbackUpdate(Long id, Sticker updated, Exception e) {
        log.error("Banco instável ao atualizar sticker id={} erro={}", id, e.getMessage());
        throw new BusinessRuleException("Serviço temporariamente indisponível, tente novamente em instantes");
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackDelete")
    @Transactional
    public void delete(Long id) {
        Sticker sticker = findById(id);
        stickerRepository.delete(sticker);
    }

    public void fallbackDelete(Long id, Exception e) {
        log.error("Banco instável ao deletar sticker id={} erro={}", id, e.getMessage());
        throw new BusinessRuleException("Serviço temporariamente indisponível, tente novamente em instantes");
    }
}