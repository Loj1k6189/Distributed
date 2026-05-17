package com.example.distributed.quest.repository;

import com.example.distributed.quest.domain.QuestionOutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionOutboxRepository extends JpaRepository<QuestionOutboxMessage, Long> {
}