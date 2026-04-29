package com.example.distributed.lottery.api;

import com.example.distributed.lottery.service.LotteryService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lottery/campaigns")
@RequiredArgsConstructor
public class LotteryController {

    private final LotteryService lotteryService;

    @PostMapping
    public CampaignResponse createCampaign(@RequestBody @Valid CreateCampaignRequest request) {
        return lotteryService.createCampaign(request);
    }

    @GetMapping("/{campaignId}")
    public CampaignResponse getCampaign(@PathVariable Long campaignId) {
        return lotteryService.getCampaign(campaignId);
    }

    @PostMapping("/{campaignId}/participants")
    public JoinResponse joinCampaign(@PathVariable Long campaignId, @RequestBody @Valid JoinRequest request) {
        return lotteryService.joinCampaign(campaignId, request);
    }

    @PostMapping("/{campaignId}/draw")
    public DrawWinnerResponse drawWinner(@PathVariable Long campaignId) {
        return lotteryService.drawWinner(campaignId);
    }

    @GetMapping("/{campaignId}/winners")
    public List<DrawWinnerResponse> listWinners(@PathVariable Long campaignId) {
        return lotteryService.listWinners(campaignId);
    }
}
