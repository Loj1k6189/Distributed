package com.example.distributed.chain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 接龙实体类
 */
@Entity
@Table(name = "chain", indexes = {
    @Index(name = "idx_created_by_title", columnList = "created_by, title", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "allow_multiple")
    @Builder.Default
    private Boolean allowMultiple = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @OneToMany(mappedBy = "chain", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ChainEntry> entries = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public void addEntry(ChainEntry entry) {
        entries.add(entry);
        entry.setChain(this);
    }

    public void removeEntry(ChainEntry entry) {
        entries.remove(entry);
        entry.setChain(null);
    }
}
