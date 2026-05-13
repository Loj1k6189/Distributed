package com.example.distributed.vote.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.vote")
public class VoteProperties {

    private long rateLimitWindowMs = 1000L;
    private int rateLimitMaxRequests = 1;
    private long lockLeaseMs = 3000L;
    private int redlockReplicas = 5;

    private boolean mqEnabled = true;
    private String mqExchange = "vote.event.exchange";
    private String mqRoutingKey = "vote.event";
    private String mqQueue = "vote.event.queue";
    private String mqDlxExchange = "vote.event.dlx.exchange";
    private String mqDlxRoutingKey = "vote.event.dlq";
    private String mqDlqQueue = "vote.event.dlq.queue";
    private int mqBatchSize = 100;

    private int outboxBatchSize = 200;
    private int outboxMaxRetry = 20;
    private Duration outboxRetryBaseDelay = Duration.ofSeconds(2);
    private boolean outboxRelayEnabled = true;

    private Duration idempotentTtl = Duration.ofDays(7);
    private boolean consumerEnabled = true;
    private boolean snapshotEnabled = true;
}
