package com.example.distributed.vote.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "vote_count_snapshot", indexes = {
        @Index(name = "uk_vote_snapshot_poll_option", columnList = "poll_id,option_id", unique = true),
        @Index(name = "idx_vote_snapshot_poll", columnList = "poll_id")
})
public class VoteCountSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "poll_id", nullable = false)
    private Long pollId;

    @Column(name = "option_id", nullable = false)
    private Long optionId;

    @Column(name = "vote_count", nullable = false)
    private Long voteCount = 0L;

    @Column(name = "snapshot_at", nullable = false)
    private Instant snapshotAt = Instant.now();
}

