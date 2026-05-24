package com.example.distributed.quest.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 题目答案实体类
 */
@Entity
@Table(name = "question_answer")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questionnaire_answer_id", nullable = false)
    private QuestionnaireAnswer questionnaireAnswer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(columnDefinition = "TEXT")
    private String textAnswer;

    @Column(name = "selected_option_ids")
    private String selectedOptionIds;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "score")
    private Double score;
}
