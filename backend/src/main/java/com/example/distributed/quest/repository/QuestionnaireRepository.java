package com.example.distributed.quest.repository;

import com.example.distributed.quest.entity.Questionnaire;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 问卷Repository
 */
@Repository
public interface QuestionnaireRepository extends JpaRepository<Questionnaire, Long> {

    /**
     * 只获取问卷基本信息（用于缓存等场景，避免EntityGraph与代理冲突）
     */
    @Query("SELECT q FROM Questionnaire q WHERE q.id = :id")
    Optional<Questionnaire> findByIdBasic(@Param("id") Long id);

    /**
     * 获取问卷及问题、选项（使用EntityGraph优化加载）
     */
    @EntityGraph(attributePaths = {"questions", "questions.options"})
    @Query("SELECT q FROM Questionnaire q WHERE q.id = :id")
    Optional<Questionnaire> findByIdWithQuestions(@Param("id") Long id);

    List<Questionnaire> findByIsActiveTrue();

    List<Questionnaire> findByIsActiveTrueAndCreatedBy(String createdBy);

    @Query("SELECT q FROM Questionnaire q WHERE q.isActive = true AND (q.startTime IS NULL OR q.startTime <= :now) AND (q.endTime IS NULL OR q.endTime >= :now)")
    List<Questionnaire> findActiveQuestionnaires(@Param("now") LocalDateTime now);

    boolean existsByCreatedByAndTitle(String createdBy, String title);
}
