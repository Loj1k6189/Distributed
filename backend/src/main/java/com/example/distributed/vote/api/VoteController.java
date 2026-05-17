package com.example.distributed.vote.api;

import com.example.distributed.vote.service.VoteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/votes")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @PostMapping("/polls")
    public VotePollCreateResponse createPoll(@RequestBody @Valid VotePollCreateRequest request) {
        return voteService.createPoll(request);
    }

    @PostMapping("/submit")
    public VoteSubmitResponse submitVote(@RequestBody @Valid VoteSubmitRequest request, HttpServletRequest httpServletRequest) {
        return voteService.submitVote(request, clientIp(httpServletRequest));
    }

    @GetMapping("/polls/{pollId}/result")
    public VotePollResultResponse pollResult(@PathVariable Long pollId) {
        return voteService.pollResult(pollId);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

