package com.edu.infnet.pb.stickers.Service;

import com.edu.infnet.pb.stickers.Dto.TradeItemDTO;
import com.edu.infnet.pb.stickers.Dto.TradeProposalRequest;
import com.edu.infnet.pb.stickers.Entity.TradeItem;
import com.edu.infnet.pb.stickers.Entity.TradeProposal;
import com.edu.infnet.pb.stickers.Enum.TradeSide;
import com.edu.infnet.pb.stickers.Enum.TradeStatus;
import com.edu.infnet.pb.stickers.Exception.BusinessRuleException;
import com.edu.infnet.pb.stickers.Exception.ResourceNotFoundException;
import com.edu.infnet.pb.stickers.Repository.TradeProposalRepository;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TradeProposalService {

    private static final Logger log = LogManager.getLogger(TradeProposalService.class);

    private final TradeProposalRepository proposalRepository;
    private final StickerService stickerService;
    private final StickerCollectionService collectionService;
    private final CollectionTransferService transferService;

    /**
     * Cria uma proposta de troca.
     * Valida se o usuário origem tem as stickers que está oferecendo.
     */
    @Transactional
    public TradeProposal create(UUID usuarioOrigem, TradeProposalRequest request) {
        if (usuarioOrigem.equals(request.getUsuarioDestino())) {
            throw new BusinessRuleException("Você não pode propor uma troca para si mesmo");
        }

        if (proposalRepository.existsPendingBetween(usuarioOrigem, request.getUsuarioDestino())) {
            throw new BusinessRuleException("Já existe uma proposta pendente entre vocês");
        }

        // Valida se origem tem as stickers que está oferecendo
        for (TradeItemDTO item : request.getItensOrigem()) {
            collectionService.validateAvailableQuantity(usuarioOrigem, item.getStickerId(), item.getQuantity());
        }

        TradeProposal proposal = TradeProposal.builder()
                .usuarioOrigem(usuarioOrigem)
                .usuarioDestino(request.getUsuarioDestino())
                .status(TradeStatus.PENDENTE)
                .build();

        // Adiciona itens de origem
        for (TradeItemDTO item : request.getItensOrigem()) {
            proposal.getItems().add(TradeItem.builder()
                    .proposal(proposal)
                    .sticker(stickerService.findById(item.getStickerId()))
                    .quantity(item.getQuantity())
                    .side(TradeSide.ORIGEM)
                    .build());
        }

        // Adiciona itens de destino
        for (TradeItemDTO item : request.getItensDestino()) {
            proposal.getItems().add(TradeItem.builder()
                    .proposal(proposal)
                    .sticker(stickerService.findById(item.getStickerId()))
                    .quantity(item.getQuantity())
                    .side(TradeSide.DESTINO)
                    .build());
        }

        TradeProposal saved = proposalRepository.save(proposal);
        log.info("Proposta id={} criada por usuário id={} para usuário id={}", saved.getId(), usuarioOrigem, request.getUsuarioDestino());
        return saved;
    }

    /**
     * Aceita uma proposta. Só o usuário destino pode aceitar.
     * Valida se destino tem as stickers que está oferecendo e executa a transferência.
     */
    @Transactional
    public TradeProposal accept(UUID usuarioDestino, Long proposalId) {
        TradeProposal proposal = findById(proposalId);

        if (!proposal.getUsuarioDestino().equals(usuarioDestino)) {
            throw new BusinessRuleException("Você não tem permissão para aceitar esta proposta");
        }

        if (!proposal.getStatus().equals(TradeStatus.PENDENTE)) {
            throw new BusinessRuleException("Esta proposta não está mais pendente");
        }

        // Valida se destino tem as stickers que está oferecendo
        proposal.getItems().stream()
                .filter(i -> i.getSide().equals(TradeSide.DESTINO))
                .forEach(item -> collectionService.validateAvailableQuantity(
                        usuarioDestino, item.getSticker().getId(), item.getQuantity()));

        // Executa a transferência
        transferService.applyTransfer(proposal);

        proposal.setStatus(TradeStatus.ACEITA);
        proposal.setResolvedAt(LocalDateTime.now());

        TradeProposal saved = proposalRepository.save(proposal);
        log.info("Proposta id={} aceita por usuário id={}", proposalId, usuarioDestino);
        return saved;
    }

    /**
     * Recusa uma proposta. Só o usuário destino pode recusar.
     */
    @Transactional
    public TradeProposal refuse(UUID usuarioDestino, Long proposalId) {
        TradeProposal proposal = findById(proposalId);

        if (!proposal.getUsuarioDestino().equals(usuarioDestino)) {
            throw new BusinessRuleException("Você não tem permissão para recusar esta proposta");
        }

        if (!proposal.getStatus().equals(TradeStatus.PENDENTE)) {
            throw new BusinessRuleException("Esta proposta não está mais pendente");
        }

        proposal.setStatus(TradeStatus.RECUSADA);
        proposal.setResolvedAt(LocalDateTime.now());

        TradeProposal saved = proposalRepository.save(proposal);
        log.info("Proposta id={} recusada por usuário id={}", proposalId, usuarioDestino);
        return saved;
    }

    /**
     * Cancela uma proposta. Só o usuário origem pode cancelar.
     */
    @Transactional
    public TradeProposal cancel(UUID usuarioOrigem, Long proposalId) {
        TradeProposal proposal = findById(proposalId);

        if (!proposal.getUsuarioOrigem().equals(usuarioOrigem)) {
            throw new BusinessRuleException("Você não tem permissão para cancelar esta proposta");
        }

        if (!proposal.getStatus().equals(TradeStatus.PENDENTE)) {
            throw new BusinessRuleException("Esta proposta não está mais pendente");
        }

        proposal.setStatus(TradeStatus.CANCELADA);
        proposal.setResolvedAt(LocalDateTime.now());

        TradeProposal saved = proposalRepository.save(proposal);
        log.info("Proposta id={} cancelada por usuário id={}", proposalId, usuarioOrigem);
        return saved;
    }

    public List<TradeProposal> findByUserId(UUID userId) {
        return proposalRepository.findByUserId(userId);
    }

    public List<TradeProposal> findPendingReceived(UUID usuarioDestino) {
        return proposalRepository.findByUsuarioDestinoAndStatus(usuarioDestino, TradeStatus.PENDENTE);
    }

    public TradeProposal findById(Long id) {
        return proposalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proposta não encontrada: id=" + id));
    }
}
