package com.example.distributed.quest.repository;

import com.example.distributed.quest.domain.SubmissionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionOptionRepository extends JpaRepository<SubmissionOption, Long> {
    List<SubmissionOption> findBySubmissionId(Long submissionId);
}