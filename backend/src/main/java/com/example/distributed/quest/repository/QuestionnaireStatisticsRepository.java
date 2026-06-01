package com.example.distributed.quest.repository;

import com.example.distributed.quest.entity.QuestionnaireStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 问卷统计Repository
 */
@Repository
public interface QuestionnaireStatisticsRepository extends JpaRepository<QuestionnaireStatistics, Long> {

    Optional<QuestionnaireStatistics> findByQuestionnaireIdAndSnapshotDateBetween(
            Long questionnaireId, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT qs FROM QuestionnaireStatistics qs WHERE qs.questionnaireId = :questionnaireId ORDER BY qs.snapshotDate DESC")
    List<QuestionnaireStatistics> findByQuestionnaireIdOrderBySnapshotDateDesc(@Param("questionnaireId") Long questionnaireId);

    List<QuestionnaireStatistics> findBySnapshotDateBetween(LocalDateTime startDate, LocalDateTime endDate);
}
