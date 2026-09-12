package com.amigoinvisible.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "rooms", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 6)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @Column(name = "suggested_budget", precision = 10, scale = 2)
    private BigDecimal suggestedBudget;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(length = 150)
    private String place;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RoomStatus status = RoomStatus.OPEN;

    @Column(name = "drawn_at")
    private Instant drawnAt;

    // Si hay una votacion de presupuesto en curso (abierta por el admin).
    // Se cierra sola cuando todos votaron y hay un ganador claro, o cuando
    // el admin desempata.
    @Column(name = "budget_voting_open", nullable = false)
    @Builder.Default
    private boolean budgetVotingOpen = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
