package com.example.distributed.quest.repository;

import com.example.distributed.quest.entity.QuestionnaireAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 问卷答卷Repository
 */
@Repository
public interface QuestionnaireAnswerRepository extends JpaRepository<QuestionnaireAnswer, Long> {

    Optional<QuestionnaireAnswer> findByUserIdAndQuestionnaireId(String userId, Long questionnaireId);

    boolean existsByUserIdAndQuestionnaireId(String userId, Long questionnaireId);

    @Query("SELECT qa FROM QuestionnaireAnswer qa WHERE qa.questionnaire.id = :questionnaireId ORDER BY qa.submittedAt DESC")
    List<QuestionnaireAnswer> findByQuestionnaireIdOrderBySubmittedAt(@Param("questionnaireId") Long questionnaireId);

    @Query("SELECT COUNT(qa) FROM QuestionnaireAnswer qa WHERE qa.questionnaire.id = :questionnaireId")
    Long countByQuestionnaireId(@Param("questionnaireId") Long questionnaireId);

    @Query("SELECT COUNT(DISTINCT qa.userId) FROM QuestionnaireAnswer qa WHERE qa.questionnaire.id = :questionnaireId AND qa.isAnonymous = false")
    Long countUniqueUsersByQuestionnaireId(@Param("questionnaireId") Long questionnaireId);

    @Query("SELECT COUNT(qa) FROM QuestionnaireAnswer qa WHERE qa.questionnaire.id = :questionnaireId AND qa.isAnonymous = true")
    Long countAnonymousByQuestionnaireId(@Param("questionnaireId") Long questionnaireId);

    @Query("SELECT qa FROM QuestionnaireAnswer qa WHERE qa.questionnaire.id = :questionnaireId AND qa.submittedAt >= :startTime")
    List<QuestionnaireAnswer> findByQuestionnaireIdAndSubmittedAfter(
            @Param("questionnaireId") Long questionnaireId,
            @Param("startTime") LocalDateTime startTime);
}
