package com.example.distributed.quest.repository;

import com.example.distributed.quest.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 题目Repository
 */
@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query("SELECT q FROM Question q LEFT JOIN FETCH q.options WHERE q.questionnaire.id = :questionnaireId ORDER BY q.sortOrder")
    List<Question> findByQuestionnaireIdWithOptions(@Param("questionnaireId") Long questionnaireId);

    List<Question> findByQuestionnaireIdOrderBySortOrder(Long questionnaireId);

    @Query("SELECT q FROM Question q WHERE q.id IN :ids")
    List<Question> findByIds(@Param("ids") List<Long> ids);
}
