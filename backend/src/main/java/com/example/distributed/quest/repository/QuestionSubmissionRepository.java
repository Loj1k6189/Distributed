package com.example.distributed.quest.repository;

import com.example.distributed.quest.domain.QuestionSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuestionSubmissionRepository extends JpaRepository<QuestionSubmission, Long> {
    Optional<QuestionSubmission> findByQuestionIdAndUserId(Long questionId, String userId);
}