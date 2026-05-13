package com.example.distributed.vote.repository;

import com.example.distributed.vote.domain.VoteOption;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteOptionRepository extends JpaRepository<VoteOption, Long> {

    List<VoteOption> findByPollIdOrderBySortNoAsc(Long pollId);
}

