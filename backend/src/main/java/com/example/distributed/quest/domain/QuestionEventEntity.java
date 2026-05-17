package com.example.distributed.quest.domain;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "question_event", indexes = {
        @Index(name = "uk_question_event_id", columnList = "event_id", unique = true),
        @Index(name = "idx_question_event_question_created", columnList = "question_id,created_at")
})
public class QuestionEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "source_ip", nullable = false, length = 64)
    private String sourceIp;

    @Column(name = "option_ids", nullable = false, length = 512)
    private String optionIds;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}