package com.edu.infnet.pb.stickers.Service;

import com.edu.infnet.pb.stickers.Dto.AcceptTradeProposalDTO;
import com.edu.infnet.pb.stickers.Dto.TradeItemDTO;
import com.edu.infnet.pb.stickers.Dto.CreateTradeProposalDTO;
import com.edu.infnet.pb.stickers.Entity.Sticker;
import com.edu.infnet.pb.stickers.Entity.TradeOfferedSticker;
import com.edu.infnet.pb.stickers.Entity.TradeProposal;
import com.edu.infnet.pb.stickers.Entity.TradeRequestedSticker;
import com.edu.infnet.pb.stickers.Enum.TradeStatus;
import com.edu.infnet.pb.stickers.Exception.BusinessRuleException;
import com.edu.infnet.pb.stickers.Exception.ResourceNotFoundException;
import com.edu.infnet.pb.stickers.Exception.UnauthorizedTradeActionException;
import com.edu.infnet.pb.stickers.Repository.TradeProposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TradeProposalService {

    private final TradeProposalRepository tradeProposalRepository;
    private final StickerService stickerService;
    private final StickerCollectionService collectionService;

    // ---------------------------------------------------------------
    // CONSULTAS
    // ---------------------------------------------------------------

    public TradeProposal findById(Long id) {
        TradeProposal trade = tradeProposalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Proposta de troca não encontrada: id=" + id));

        // verificação on-demand: se já passou da validade (PENDING ou
        // ACCEPTED sem confirmação), expira antes de retornar
        expireIfNeeded(trade);
        return trade;
    }

    public List<TradeProposal> findSentBy(UUID proposerId) {
        return tradeProposalRepository.findByProposerIdOrderByCreatedAtDesc(proposerId);
    }

    public List<TradeProposal> findReceivedBy(UUID receiverId) {
        return tradeProposalRepository.findByReceiverIdOrderByCreatedAtDesc(receiverId);
    }

    public List<TradeProposal> findPendingSentBy(UUID proposerId) {
        return tradeProposalRepository.findByProposerIdAndStatus(proposerId, TradeStatus.PENDING);
    }

    public List<TradeProposal> findPendingReceivedBy(UUID receiverId) {
        return tradeProposalRepository.findByReceiverIdAndStatus(receiverId, TradeStatus.PENDING);
    }

    // ---------------------------------------------------------------
    // 1) CRIAR PROPOSTA
    // ---------------------------------------------------------------

    @Transactional
    public TradeProposal createProposal(CreateTradeProposalDTO dto) {
        validateNotSameUser(dto.getProposerId(), dto.getReceiverId());
        validateNotEmpty(dto.getOfferedStickers(), "oferecidas");
        validateNotEmpty(dto.getRequestedSticker(), "desejadas");

        // Requisito não-funcional: impedir duplicidade de propostas em
        // aberto entre o mesmo par de usuários.
        if (tradeProposalRepository.existsPendingBetween(dto.getProposerId(), dto.getReceiverId())) {
            throw new BusinessRuleException(
                    "Já existe uma proposta pendente em aberto entre esses dois usuários");
        }

        // proponente precisa ter quantidade LIVRE suficiente do que está oferecendo
        for (TradeItemDTO item : dto.getOfferedStickers()) {
            validateQuantity(item);
            collectionService.validateAvailableQuantity(
                    dto.getProposerId(), item.getStickerId(), item.getQuantity());
        }

        // receptor precisa ter quantidade LIVRE suficiente do que está sendo pedido
        for (TradeItemDTO item : dto.getRequestedSticker()) {
            validateQuantity(item);
            collectionService.validateAvailableQuantity(
                    dto.getReceiverId(), item.getStickerId(), item.getQuantity());
        }

        TradeProposal trade = TradeProposal.builder()
                .proposerId(dto.getProposerId())
                .receiverId(dto.getReceiverId())
                .message(dto.getMessage())
                .status(TradeStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        dto.getOfferedStickers().forEach(item ->
                trade.getOfferedStickers().add(buildOfferedSticker(trade, item)));

        dto.getRequestedSticker().forEach(item ->
                trade.getRequestedStickers().add(buildRequestedSticker(trade, item)));

        return tradeProposalRepository.save(trade);
    }

    private TradeOfferedSticker buildOfferedSticker(TradeProposal trade, TradeItemDTO item) {
        Sticker sticker = stickerService.findById(item.getStickerId());
        return TradeOfferedSticker.builder()
                .tradeProposal(trade)
                .sticker(sticker)
                .quantity(item.getQuantity())
                .build();
    }

    private TradeRequestedSticker buildRequestedSticker(TradeProposal trade, TradeItemDTO item) {
        Sticker sticker = stickerService.findById(item.getStickerId());
        return TradeRequestedSticker.builder()
                .tradeProposal(trade)
                .sticker(sticker)
                .quantity(item.getQuantity())
                .build();
    }

    // ---------------------------------------------------------------
    // 2) ACEITAR PROPOSTA (marca o encontro, NÃO move figurinhas ainda)
    // ---------------------------------------------------------------

    /**
     * O receptor aceita a proposta e define onde/quando o encontro
     * presencial vai acontecer. As figurinhas continuam com os donos atuais
     * — elas só mudam de mão quando os DOIS lados confirmarem a troca como
     * realizada (ver {@link #confirmCompletion}). A quantidade já está travada desde o
     * PENDING, então nada muda em termos de disponibilidade aqui.
     */
    @Transactional
    public TradeProposal acceptProposal(Long tradeId, UUID receiverId, AcceptTradeProposalDTO dto) {
        TradeProposal trade = findById(tradeId);

        if (!trade.getReceiverId().equals(receiverId)) {
            throw new UnauthorizedTradeActionException(
                    "Apenas o receptor da proposta pode aceitá-la");
        }

        ensurePending(trade);
        validateMeetingData(dto);

        // Revalida posse: pode ter mudado entre a criação e a aceitação
        for (TradeOfferedSticker item : trade.getOfferedStickers()) {
            collectionService.validateAvailableQuantity(
                    trade.getProposerId(), item.getSticker().getId(), item.getQuantity());
        }
        for (TradeRequestedSticker item : trade.getRequestedStickers()) {
            collectionService.validateAvailableQuantity(
                    trade.getReceiverId(), item.getSticker().getId(), item.getQuantity());
        }

        trade.setMeetingLocation(dto.getMeetingLocation());
        trade.setMeetingAt(dto.getMeetingAt());
        trade.setStatus(TradeStatus.ACCEPTED);

        return tradeProposalRepository.save(trade);
    }

    // ---------------------------------------------------------------
    // 3) CONFIRMAR TROCA REALIZADA (aqui sim as figurinhas mudam de dono)
    // ---------------------------------------------------------------

    /**
     * Chamado por UM dos lados (proponente OU receptor) após o encontro
     * presencial, confirmando que a troca aconteceu daquele lado. A troca só
     * é efetivada (figurinhas mudam de dono) quando AMBOS os lados tiverem
     * confirmado — isso evita que um usuário sozinho force a conclusão de
     * uma troca que na prática não ocorreu.
     */
    @Transactional
    public TradeProposal confirmCompletion(Long tradeId, UUID confirmingUserId) {
        TradeProposal trade = findById(tradeId);

        if (!trade.isOwnedBy(confirmingUserId)) {
            throw new UnauthorizedTradeActionException(
                    "Apenas o proponente ou o receptor podem confirmar a troca");
        }

        if (!trade.isAccepted()) {
            throw new BusinessRuleException(
                    "Só é possível confirmar uma troca já ACEITA (status atual: "
                            + trade.getStatus() + ")");
        }

        if (trade.hasConfirmed(confirmingUserId)) {
            throw new BusinessRuleException("Você já confirmou esta troca anteriormente");
        }

        registerConfirmation(trade, confirmingUserId);

        if (trade.isFullyConfirmed()) {
            // Revalida posse uma última vez antes de mover de fato as figurinhas
            for (TradeOfferedSticker item : trade.getOfferedStickers()) {
                collectionService.validateAvailableQuantity(
                        trade.getProposerId(), item.getSticker().getId(), item.getQuantity());
            }
            for (TradeRequestedSticker item : trade.getRequestedStickers()) {
                collectionService.validateAvailableQuantity(
                        trade.getReceiverId(), item.getSticker().getId(), item.getQuantity());
            }

            executeTransfer(trade);

            trade.setStatus(TradeStatus.COMPLETED);
            trade.setCompletedAt(LocalDateTime.now());
        }
        // Se só um lado confirmou até agora, a troca permanece ACCEPTED,
        // aguardando a confirmação do outro lado.

        return tradeProposalRepository.save(trade);
    }

    private void registerConfirmation(TradeProposal trade, UUID confirmingUserId) {
        LocalDateTime now = LocalDateTime.now();
        if (trade.getProposerId().equals(confirmingUserId)) {
            trade.setConfirmedByProposer(true);
            trade.setProposerConfirmedAt(now);
        } else {
            trade.setConfirmedByReceiver(true);
            trade.setReceiverConfirmedAt(now);
        }
    }

    /**
     * Move as figurinhas de fato: o que o proponente ofereceu vai para o
     * receptor, e o que o proponente pediu (que era do receptor) vai para ele.
     */
    private void executeTransfer(TradeProposal trade) {
        for (TradeOfferedSticker item : trade.getOfferedStickers()) {
            Long stickerId = item.getSticker().getId();
            collectionService.removeQuantity(trade.getProposerId(), stickerId, item.getQuantity());
            collectionService.addSticker(trade.getReceiverId(), stickerId, item.getQuantity());
        }

        for (TradeRequestedSticker item : trade.getRequestedStickers()) {
            Long stickerId = item.getSticker().getId();
            collectionService.removeQuantity(trade.getReceiverId(), stickerId, item.getQuantity());
            collectionService.addSticker(trade.getProposerId(), stickerId, item.getQuantity());
        }
    }

    // ---------------------------------------------------------------
    // RECUSAR / CANCELAR
    // ---------------------------------------------------------------

    @Transactional
    public TradeProposal rejectProposal(Long tradeId, UUID receiverId) {
        TradeProposal trade = findById(tradeId);

        if (!trade.getReceiverId().equals(receiverId)) {
            throw new UnauthorizedTradeActionException(
                    "Apenas o receptor da proposta pode recusá-la");
        }

        ensurePending(trade);
        trade.setStatus(TradeStatus.REJECTED);
        return tradeProposalRepository.save(trade);
    }

    /**
     * Cancela a proposta. Funciona tanto para PENDING (proponente desiste
     * antes de qualquer resposta) quanto para ACCEPTED (qualquer um dos dois
     * cancela o encontro já marcado — por exemplo, imprevisto de última hora).
     */
    @Transactional
    public TradeProposal cancelProposal(Long tradeId, UUID requestingUserId) {
        TradeProposal trade = findById(tradeId);

        if (trade.isPending() && !trade.getProposerId().equals(requestingUserId)) {
            throw new UnauthorizedTradeActionException(
                    "Apenas o proponente pode cancelar uma proposta ainda pendente");
        }

        if (trade.isAccepted() && !trade.isOwnedBy(requestingUserId)) {
            throw new UnauthorizedTradeActionException(
                    "Apenas o proponente ou o receptor podem cancelar o encontro marcado");
        }

        if (!trade.isPending() && !trade.isAccepted()) {
            throw new BusinessRuleException(
                    "Proposta não pode ser cancelada no status atual: " + trade.getStatus());
        }

        trade.setStatus(TradeStatus.CANCELLED);
        return tradeProposalRepository.save(trade);
    }

    // ---------------------------------------------------------------
    // EXPIRAÇÃO
    // ---------------------------------------------------------------

    /**
     * Verificação on-demand: chamada sempre que uma proposta é lida
     * (findById). Vale tanto para PENDING (ninguém respondeu) quanto para
     * ACCEPTED (encontro marcado mas nunca confirmado) — em ambos os casos,
     * se passou do prazo, expira na hora.
     */
    private void expireIfNeeded(TradeProposal trade) {
        if (trade.isLocking() && trade.getExpiresAt().isBefore(LocalDateTime.now())) {
            trade.setStatus(TradeStatus.EXPIRED);
            tradeProposalRepository.save(trade);
        }
    }

    /**
     * Job agendado: roda de hora em hora e expira em lote todas as propostas
     * ainda ativas (PENDING ou ACCEPTED) vencidas, mesmo que ninguém tenha
     * acessado elas via findById. Sem isso, uma proposta vencida e nunca
     * consultada ficaria travando a quantidade livre dos dois usuários
     * indefinidamente.
     */
    @Scheduled(fixedRate = 3_600_000) // a cada 1 hora
    @Transactional
    public void expireOverdueProposalsJob() {
        List<TradeProposal> expired = tradeProposalRepository
                .findAllActiveExpiredBefore(LocalDateTime.now());

        expired.forEach(trade -> trade.setStatus(TradeStatus.EXPIRED));
        tradeProposalRepository.saveAll(expired);
    }

    // ---------------------------------------------------------------
    // VALIDAÇÕES INTERNAS
    // ---------------------------------------------------------------

    private void ensurePending(TradeProposal trade) {
        if (!trade.isPending()) {
            throw new BusinessRuleException(
                    "Proposta não está pendente (status atual: " + trade.getStatus() + ")");
        }
    }

    private void validateNotSameUser(UUID proposerId, UUID receiverId) {
        if (proposerId.equals(receiverId)) {
            throw new BusinessRuleException("Não é possível propor uma troca consigo mesmo");
        }
    }

    private void validateNotEmpty(List<TradeItemDTO> items, String label) {
        if (items == null || items.isEmpty()) {
            throw new BusinessRuleException(
                    "A lista de stickers " + label + " não pode estar vazia");
        }
    }

    private void validateQuantity(TradeItemDTO item) {
        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            throw new BusinessRuleException(
                    "Quantidade inválida para sticker id=" + item.getStickerId());
        }
    }

    private void validateMeetingData(AcceptTradeProposalDTO dto) {
        if (dto.getMeetingLocation() == null || dto.getMeetingLocation().isBlank()) {
            throw new BusinessRuleException("Local do encontro é obrigatório");
        }
        if (dto.getMeetingAt() == null) {
            throw new BusinessRuleException("Data/hora do encontro é obrigatória");
        }
        if (dto.getMeetingAt().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Data/hora do encontro não pode estar no passado");
        }
    }
}