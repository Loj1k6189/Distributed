package com.example.distributed.chain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 接龙事件实体类
 */
@Entity
@Table(name = "chain_event", indexes = {
    @Index(name = "idx_unprocessed", columnList = "is_processed, created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChainEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "chain_id", nullable = false)
    private Long chainId;

    @Column(name = "entry_id")
    private Long entryId;

    @Column(name = "user_id", length = 100)
    private String userId;

    @Column(name = "event_data", columnDefinition = "TEXT")
    private String eventData;

    @Column(name = "is_processed")
    @Builder.Default
    private Boolean isProcessed = false;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
