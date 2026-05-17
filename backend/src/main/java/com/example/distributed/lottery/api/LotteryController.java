package com.example.distributed.lottery.api;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.distributed.lottery.domain.LotteryHistory;
import com.example.distributed.lottery.service.LotteryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/lottery")
@RequiredArgsConstructor
public class LotteryController {

    private final LotteryService lotteryService;

    @PostMapping("/{activityId}/join")
    public void joinPool(@PathVariable String activityId, @RequestParam String userId) {
        lotteryService.joinPool(activityId, userId);
    }

    @PostMapping("/{activityId}/draw")
    public List<String> draw(@PathVariable String activityId, @RequestParam int round, @RequestParam int count) {
        return lotteryService.draw(activityId, round, count);
    }

    @GetMapping("/{activityId}/winners/latest")
    public List<String> getLatestWinners(@PathVariable String activityId, @RequestParam int round) {
        return lotteryService.getLatestWinners(activityId, round);
    }

    @GetMapping("/{activityId}/history")
    public Page<LotteryHistory> getHistory(@PathVariable String activityId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return lotteryService.getHistory(activityId, page, size);
    }
}