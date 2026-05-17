package com.example.distributed.vote.api;

import com.example.distributed.vote.service.VoteDlqRetryService;
import com.example.distributed.vote.service.VoteBusinessException;
import com.example.distributed.vote.service.VoteRecoveryService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/votes/admin")
@RequiredArgsConstructor
public class VoteAdminController {

    private final VoteRecoveryService voteRecoveryService;
    private final ObjectProvider<VoteDlqRetryService> voteDlqRetryServiceProvider;

    @PostMapping("/recovery/rebuild")
    public int rebuildRedis() {
        return voteRecoveryService.rebuildAllFromSnapshotAndReplay();
    }

    @PostMapping("/snapshot")
    public void createSnapshot() {
        voteRecoveryService.createSnapshot();
    }

    @PostMapping("/dlq/retry")
    public DlqRetryResponse retryDlq(@RequestParam(defaultValue = "100") int limit) {
        VoteDlqRetryService voteDlqRetryService = voteDlqRetryServiceProvider.getIfAvailable();
        if (voteDlqRetryService == null) {
            throw new VoteBusinessException("MQ_DISABLED", "MQ 未启用，无法重试死信队列", HttpStatus.CONFLICT);
        }
        return new DlqRetryResponse(voteDlqRetryService.retryFromDlq(Math.max(limit, 1)));
    }
}
