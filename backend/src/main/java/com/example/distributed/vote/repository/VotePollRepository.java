package com.example.distributed.vote.repository;

import com.example.distributed.vote.domain.VotePoll;
import com.example.distributed.vote.domain.VotePollStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VotePollRepository extends JpaRepository<VotePoll, Long> {

    List<VotePoll> findAllByOrderByCreatedAtDesc();

    List<VotePoll> findByStatusOrderByCreatedAtDesc(VotePollStatus status);
}
