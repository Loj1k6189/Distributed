package com.example.distributed.quest.repository;

import com.example.distributed.quest.domain.QuestionStatsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionStatsSnapshotRepository extends JpaRepository<QuestionStatsSnapshot, Long> {
    List<QuestionStatsSnapshot> findByQuestionId(Long questionId);
}