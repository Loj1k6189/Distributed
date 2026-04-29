package com.example.distributed.lottery.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "lottery_campaign")
public class LotteryCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false)
    private Integer plannedWinners;

    @Column(nullable = false)
    private Integer winnersDrawn = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CampaignStatus status = CampaignStatus.ACTIVE;
}
