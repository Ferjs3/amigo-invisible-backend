package com.amigoinvisible.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    // dueno del muro
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_user_id", nullable = false)
    private User targetUser;

    // quien pregunto. Se guarda solo para moderacion / anti-abuso.
    // IMPORTANTE: este campo jamas debe exponerse en un DTO de respuesta.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asker_id", nullable = false)
    private User asker;

    @Column(name = "question_text", nullable = false, length = 300)
    private String questionText;

    @Column(name = "answer_text", length = 300)
    private String answerText;

    @Column(nullable = false)
    @Builder.Default
    private boolean answered = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "answered_at")
    private Instant answeredAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
