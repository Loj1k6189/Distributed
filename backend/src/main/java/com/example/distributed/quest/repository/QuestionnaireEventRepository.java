package com.example.distributed.quest.repository;

import com.example.distributed.quest.entity.QuestionnaireEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 问卷事件Repository
 */
@Repository
public interface QuestionnaireEventRepository extends JpaRepository<QuestionnaireEvent, Long> {

    List<QuestionnaireEvent> findByIsProcessedFalseOrderByCreatedAtAsc();

    List<QuestionnaireEvent> findByEventTypeAndIsProcessedFalse(String eventType);
}
