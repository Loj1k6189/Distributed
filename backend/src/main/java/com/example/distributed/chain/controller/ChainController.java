package com.example.distributed.chain.controller;

import com.example.distributed.chain.dto.ChainCreateRequest;
import com.example.distributed.chain.dto.ChainEntryRequest;
import com.example.distributed.chain.dto.ChainResponse;
import com.example.distributed.chain.entity.Chain;
import com.example.distributed.chain.service.ChainService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 接龙控制器
 */
@RestController
@RequestMapping("/api/chains")
@RequiredArgsConstructor
public class ChainController {

    private final ChainService chainService;

    /**
     * 创建接龙
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createChain(
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId,
            @Valid @RequestBody ChainCreateRequest request) {

        Chain chain = chainService.createChain(userId, request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "接龙创建成功");
        response.put("data", Map.of("id", chain.getId()));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 获取接龙详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getChain(@PathVariable Long id) {
        ChainResponse response = chainService.getChain(id);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", response);

        return ResponseEntity.ok(result);
    }

    /**
     * 获取所有活跃接龙
     */
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveChains() {
        List<ChainResponse> chains = chainService.getActiveChains();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", chains);

        return ResponseEntity.ok(result);
    }

    /**
     * 参与接龙
     */
    @PostMapping("/{id}/join")
    public ResponseEntity<Map<String, Object>> joinChain(
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId,
            @PathVariable Long id,
            @Valid @RequestBody ChainEntryRequest request) {

        Long entryId = chainService.joinChain(userId, id, request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "接龙成功");
        response.put("data", Map.of("entryId", entryId));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 删除接龙
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteChain(
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId,
            @PathVariable Long id) {

        chainService.deleteChain(userId, id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "接龙删除成功");

        return ResponseEntity.ok(response);
    }
}
