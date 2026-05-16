package com.example.distributed.quest.repository;

import com.example.distributed.quest.domain.QuestionEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface QuestionEventRepository extends JpaRepository<QuestionEventEntity, Long> {

    long countByQuestionId(Long questionId);

    List<QuestionEventEntity> findByQuestionIdAndCreatedAtAfterOrderByCreatedAtAsc(Long questionId, Instant createdAt);

    @Query("select e.eventId from QuestionEventEntity e where e.eventId in :eventIds")
    List<String> findExistingEventIds(@Param("eventIds") List<String> eventIds);
}