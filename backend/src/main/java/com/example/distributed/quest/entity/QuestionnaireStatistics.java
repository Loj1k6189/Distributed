package com.example.distributed.quest.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 问卷统计实体类 - 用于统计和快照功能
 */
@Entity
@Table(name = "questionnaire_statistics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionnaireStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "questionnaire_id", nullable = false)
    private Long questionnaireId;

    @Column(name = "total_submissions")
    @Builder.Default
    private Long totalSubmissions = 0L;

    @Column(name = "completed_submissions")
    @Builder.Default
    private Long completedSubmissions = 0L;

    @Column(name = "partial_submissions")
    @Builder.Default
    private Long partialSubmissions = 0L;

    @Column(name = "average_completion_time")
    private Double averageCompletionTime;

    @Column(name = "unique_users")
    @Builder.Default
    private Long uniqueUsers = 0L;

    @Column(name = "anonymous_submissions")
    @Builder.Default
    private Long anonymousSubmissions = 0L;

    @Column(name = "last_submission_at")
    private LocalDateTime lastSubmissionAt;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDateTime snapshotDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
