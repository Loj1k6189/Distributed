package com.example.distributed.chain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 接龙项实体类
 */
@Entity
@Table(name = "chain_entry", indexes = {
    @Index(name = "idx_chain_user", columnList = "chain_id, user_id", unique = true),
    @Index(name = "idx_chain_sequence", columnList = "chain_id, sequence_no")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChainEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chain_id", nullable = false)
    private Chain chain;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(length = 1000)
    private String content;

    @Column(name = "sequence_no", nullable = false)
    private Long sequenceNo;

    @Column(name = "parent_entry_id")
    private Long parentEntryId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
