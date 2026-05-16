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
@Table(name = "vote_event", indexes = {
        @Index(name = "uk_vote_event_id", columnList = "event_id", unique = true),
        @Index(name = "idx_vote_event_poll_created", columnList = "poll_id,created_at")
})
public class VoteEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "poll_id", nullable = false)
    private Long pollId;

    @Column(name = "voter_id", nullable = false, length = 64)
    private String voterId;

    @Column(name = "source_ip", nullable = false, length = 64)
    private String sourceIp;

    @Column(name = "option_ids", nullable = false, length = 512)
    private String optionIds;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

