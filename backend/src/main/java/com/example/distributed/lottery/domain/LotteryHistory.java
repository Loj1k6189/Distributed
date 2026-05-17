package com.example.distributed.lottery.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "lottery_history")
@Getter
@Setter
public class LotteryHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String activityId;

    @Column(nullable = false, length = 64)
    private String userId;

    @Column(nullable = false)
    private Integer round;

    @Column(nullable = false)
    private Instant wonAt;
}