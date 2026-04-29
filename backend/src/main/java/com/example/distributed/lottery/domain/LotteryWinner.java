package com.example.distributed.lottery.domain;

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
@Table(name = "lottery_winner", indexes = {
        @Index(name = "uk_winner_campaign_round", columnList = "campaign_id,round_no", unique = true),
        @Index(name = "uk_winner_campaign_user", columnList = "campaign_id,user_id", unique = true)
})
public class LotteryWinner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "round_no", nullable = false)
    private Integer roundNo;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "display_name", nullable = false, length = 128)
    private String displayName;

    @Column(name = "drawn_at", nullable = false)
    private Instant drawnAt = Instant.now();
}
