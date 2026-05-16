package com.example.distributed.quest.domain;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "question_outbox", indexes = {
        @Index(name = "uk_question_outbox_event", columnList = "event_id", unique = true),
        @Index(name = "idx_question_outbox_status_retry", columnList = "status,next_retry_at")
})
public class QuestionOutboxMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private QuestionOutboxStatus status = QuestionOutboxStatus.NEW;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}