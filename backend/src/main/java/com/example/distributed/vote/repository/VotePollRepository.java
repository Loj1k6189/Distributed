package com.example.distributed.vote.repository;

import com.example.distributed.vote.domain.VotePoll;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VotePollRepository extends JpaRepository<VotePoll, Long> {
}

