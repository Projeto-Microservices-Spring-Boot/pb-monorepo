package com.edu.infnet.pb.stickers.Entity;

import com.edu.infnet.pb.stickers.Enum.TradeStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "trade_proposals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proposer_id", nullable = false)
    private UUID proposerId;

    @Column(name = "receiver_id", nullable = false)
    private UUID receiverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TradeStatus status = TradeStatus.PENDING;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    // --- Dados do encontro presencial (preenchidos no ACCEPT) ---

    @Column(name = "meeting_location", length = 255)
    private String meetingLocation;

    @Column(name = "meeting_at")
    private LocalDateTime meetingAt;

    // Confirmação em duas vias: a troca só vira COMPLETED quando AMBOS os
    // lados confirmarem que o encontro presencial realmente aconteceu.
    // Isso evita que um usuário malicioso force a troca sozinho.
    @Column(name = "confirmed_by_proposer", nullable = false)
    @Builder.Default
    private boolean confirmedByProposer = false;

    @Column(name = "confirmed_by_receiver", nullable = false)
    @Builder.Default
    private boolean confirmedByReceiver = false;

    @Column(name = "proposer_confirmed_at")
    private LocalDateTime proposerConfirmedAt;

    @Column(name = "receiver_confirmed_at")
    private LocalDateTime receiverConfirmedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // --- Controle de tempo ---

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder.Default
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

    @OneToMany(mappedBy = "tradeProposal", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TradeOfferedSticker> offeredStickers = new ArrayList<>();

    @OneToMany(mappedBy = "tradeProposal", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TradeRequestedSticker> requestedStickers = new ArrayList<>();

    // ---------------------------------------------------------------
    // Helpers de estado
    // ---------------------------------------------------------------

    public boolean isPending() {
        return TradeStatus.PENDING.equals(this.status);
    }

    public boolean isAccepted() {
        return TradeStatus.ACCEPTED.equals(this.status);
    }

    public boolean isCompleted() {
        return TradeStatus.COMPLETED.equals(this.status);
    }

    /**
     * Verdadeiro quando os dois lados já confirmaram o encontro
     * presencialmente — ponto em que a troca pode (e deve) ser efetivada.
     */
    public boolean isFullyConfirmed() {
        return confirmedByProposer && confirmedByReceiver;
    }

    public boolean hasConfirmed(UUID userId) {
        if (proposerId.equals(userId)) {
            return confirmedByProposer;
        }
        if (receiverId.equals(userId)) {
            return confirmedByReceiver;
        }
        return false;
    }

    /**
     * Indica se a proposta ainda está "ativa" no sentido de manter a
     * quantidade das figurinhas travada (não disponível para outras trocas).
     * Vale tanto para PENDING (aguardando resposta) quanto ACCEPTED
     * (encontro marcado, mas troca física ainda não confirmada).
     */
    public boolean isLocking() {
        return isPending() || isAccepted();
    }

    public boolean isOwnedBy(UUID userId) {
        return proposerId.equals(userId) || receiverId.equals(userId);
    }
}