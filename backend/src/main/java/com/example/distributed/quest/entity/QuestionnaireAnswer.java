package com.example.distributed.quest.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 问卷答卷实体类
 */
@Entity
@Table(name = "questionnaire_answer", 
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_user_questionnaire", 
                           columnNames = {"user_id", "questionnaire_id"})
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionnaireAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questionnaire_id", nullable = false)
    private Questionnaire questionnaire;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "user_ip")
    private String userIp;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "is_anonymous")
    @Builder.Default
    private Boolean isAnonymous = false;

    @Column(name = "start_time")
    private Long startTime;

    @OneToMany(mappedBy = "questionnaireAnswer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<QuestionAnswer> questionAnswers = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "submitted_at", updatable = false)
    private LocalDateTime submittedAt;

    @Column(name = "completion_time")
    private Long completionTime;

    @Column(name = "submit_version")
    @Builder.Default
    private Integer submitVersion = 1;

    public void addQuestionAnswer(QuestionAnswer answer) {
        questionAnswers.add(answer);
        answer.setQuestionnaireAnswer(this);
    }
}
