package com.example.distributed.quest.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 问卷事件实体类 - 用于事件驱动架构
 */
@Entity
@Table(name = "questionnaire_event")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionnaireEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "questionnaire_id")
    private Long questionnaireId;

    @Column(name = "answer_id")
    private Long answerId;

    @Column(name = "user_id")
    private String userId;

    @Column(columnDefinition = "TEXT")
    private String eventData;

    @Column(name = "is_processed")
    @Builder.Default
    private Boolean isProcessed = false;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
