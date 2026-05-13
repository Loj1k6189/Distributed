package com.example.distributed.vote.repository;

import com.example.distributed.vote.domain.VoteCountSnapshot;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoteCountSnapshotRepository extends JpaRepository<VoteCountSnapshot, Long> {

    List<VoteCountSnapshot> findByPollId(Long pollId);

    @Modifying
    @Query(value = """
            INSERT INTO vote_count_snapshot(poll_id, option_id, vote_count, snapshot_at)
            VALUES (:pollId, :optionId, :voteCount, :snapshotAt)
            ON DUPLICATE KEY UPDATE
            vote_count = VALUES(vote_count),
            snapshot_at = VALUES(snapshot_at)
            """, nativeQuery = true)
    void upsertSnapshot(@Param("pollId") Long pollId,
                        @Param("optionId") Long optionId,
                        @Param("voteCount") Long voteCount,
                        @Param("snapshotAt") Instant snapshotAt);
}

