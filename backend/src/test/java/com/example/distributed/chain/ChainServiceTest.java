package com.example.distributed.chain;

import com.example.distributed.chain.dto.ChainCreateRequest;
import com.example.distributed.chain.dto.ChainEntryRequest;
import com.example.distributed.chain.dto.ChainResponse;
import com.example.distributed.chain.entity.Chain;
import com.example.distributed.chain.exception.ChainException;
import com.example.distributed.chain.service.ChainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 接龙服务简单测试
 * 使用已有的Spring Boot Test和JPA Test依赖
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ChainServiceTest {

    @Autowired
    private ChainService chainService;

    private String testUserId;

    @BeforeEach
    void setUp() {
        testUserId = "test-user-001";
    }

    @Test
    @DisplayName("测试：创建接龙")
    void testCreateChain() {
        // 准备测试数据
        ChainCreateRequest request = createSampleRequest();

        // 执行创建
        Chain chain = chainService.createChain(testUserId, request);

        // 验证结果
        assertNotNull(chain);
        assertNotNull(chain.getId());
        assertEquals("测试接龙", chain.getTitle());
        assertTrue(chain.getIsActive());
        assertEquals(50, chain.getMaxParticipants());
        assertFalse(chain.getAllowMultiple());
        
        System.out.println("✓ 接龙创建成功，ID: " + chain.getId());
    }

    @Test
    @DisplayName("测试：获取接龙详情")
    void testGetChain() {
        // 先创建接龙
        ChainCreateRequest request = createSampleRequest();
        Chain created = chainService.createChain(testUserId, request);

        // 获取详情
        ChainResponse response = chainService.getChain(created.getId());

        // 验证
        assertNotNull(response);
        assertEquals(created.getId(), response.getId());
        assertEquals("测试接龙", response.getTitle());
        assertEquals(0, response.getParticipantCount());
        
        System.out.println("✓ 接龙获取成功");
    }

    @Test
    @DisplayName("测试：参与接龙")
    void testJoinChain() {
        // 先创建接龙
        ChainCreateRequest request = createSampleRequest();
        Chain created = chainService.createChain(testUserId, request);

        // 参与接龙
        ChainEntryRequest joinRequest = new ChainEntryRequest();
        joinRequest.setContent("我来接龙了！");
        
        Long entryId = chainService.joinChain("user-002", created.getId(), joinRequest);

        // 验证
        assertNotNull(entryId);
        
        // 获取接龙详情，验证参与人数
        ChainResponse response = chainService.getChain(created.getId());
        assertEquals(1, response.getParticipantCount());
        assertEquals(1, response.getEntries().size());
        assertEquals("user-002", response.getEntries().get(0).getUserId());
        assertEquals(1L, response.getEntries().get(0).getSequenceNo());
        
        System.out.println("✓ 接龙参与成功，Entry ID: " + entryId);
    }

    @Test
    @DisplayName("测试：接龙序号递增")
    void testSequenceNumberIncrement() {
        // 创建接龙
        ChainCreateRequest request = createSampleRequest();
        Chain created = chainService.createChain(testUserId, request);

        // 第一个用户参与
        ChainEntryRequest joinRequest1 = new ChainEntryRequest();
        joinRequest1.setContent("用户1");
        chainService.joinChain("user-001", created.getId(), joinRequest1);

        // 第二个用户参与
        ChainEntryRequest joinRequest2 = new ChainEntryRequest();
        joinRequest2.setContent("用户2");
        chainService.joinChain("user-002", created.getId(), joinRequest2);

        // 第三个用户参与
        ChainEntryRequest joinRequest3 = new ChainEntryRequest();
        joinRequest3.setContent("用户3");
        chainService.joinChain("user-003", created.getId(), joinRequest3);

        // 验证序号
        ChainResponse response = chainService.getChain(created.getId());
        assertEquals(3, response.getEntries().size());
        assertEquals(1L, response.getEntries().get(0).getSequenceNo());
        assertEquals(2L, response.getEntries().get(1).getSequenceNo());
        assertEquals(3L, response.getEntries().get(2).getSequenceNo());
        
        System.out.println("✓ 接龙序号递增正确：1, 2, 3");
    }

    @Test
    @DisplayName("测试：不允许重复参与")
    void testDuplicateJoinNotAllowed() {
        // 创建接龙（不允许重复）
        ChainCreateRequest request = createSampleRequest();
        request.setAllowMultiple(false);
        Chain created = chainService.createChain(testUserId, request);

        // 第一次参与
        ChainEntryRequest joinRequest = new ChainEntryRequest();
        joinRequest.setContent("第一次参与");
        chainService.joinChain("user-001", created.getId(), joinRequest);

        // 第二次参与应该失败
        ChainEntryRequest joinRequest2 = new ChainEntryRequest();
        joinRequest2.setContent("第二次参与");
        
        ChainException exception = assertThrows(ChainException.class, () -> {
            chainService.joinChain("user-001", created.getId(), joinRequest2);
        });
        
        assertEquals(ChainException.ErrorCode.MULTIPLE_NOT_ALLOWED, exception.getErrorCode());
        
        System.out.println("✓ 正确阻止重复参与");
    }

    @Test
    @DisplayName("测试：接龙人数限制")
    void testMaxParticipantsLimit() {
        // 创建接龙（限制2人）
        ChainCreateRequest request = createSampleRequest();
        request.setMaxParticipants(2);
        Chain created = chainService.createChain(testUserId, request);

        // 第一个用户参与
        ChainEntryRequest joinRequest1 = new ChainEntryRequest();
        joinRequest1.setContent("用户1");
        chainService.joinChain("user-001", created.getId(), joinRequest1);

        // 第二个用户参与
        ChainEntryRequest joinRequest2 = new ChainEntryRequest();
        joinRequest2.setContent("用户2");
        chainService.joinChain("user-002", created.getId(), joinRequest2);

        // 第三个用户参与应该失败
        ChainEntryRequest joinRequest3 = new ChainEntryRequest();
        joinRequest3.setContent("用户3");
        
        ChainException exception = assertThrows(ChainException.class, () -> {
            chainService.joinChain("user-003", created.getId(), joinRequest3);
        });
        
        assertEquals(ChainException.ErrorCode.CHAIN_FULL, exception.getErrorCode());
        
        System.out.println("✓ 正确限制参与人数");
    }

    @Test
    @DisplayName("测试：获取所有活跃接龙")
    void testGetActiveChains() {
        // 创建多个接龙
        ChainCreateRequest request1 = createSampleRequest();
        request1.setTitle("接龙1");
        chainService.createChain(testUserId, request1);

        ChainCreateRequest request2 = createSampleRequest();
        request2.setTitle("接龙2");
        chainService.createChain(testUserId, request2);

        // 获取活跃接龙
        List<ChainResponse> activeChains = chainService.getActiveChains();

        // 验证
        assertNotNull(activeChains);
        assertTrue(activeChains.size() >= 2);
        
        System.out.println("✓ 获取活跃接龙成功，数量: " + activeChains.size());
    }

    @Test
    @DisplayName("测试：删除接龙")
    void testDeleteChain() {
        // 创建接龙
        ChainCreateRequest request = createSampleRequest();
        Chain created = chainService.createChain(testUserId, request);

        // 删除接龙
        chainService.deleteChain(testUserId, created.getId());

        // 验证删除后的状态
        ChainResponse response = chainService.getChain(created.getId());
        assertFalse(response.getIsActive());
        
        System.out.println("✓ 接龙删除成功");
    }

    @Test
    @DisplayName("测试：接龙不存在时抛出异常")
    void testChainNotFound() {
        // 验证不存在的接龙会抛出异常
        assertThrows(ChainException.class, () -> {
            chainService.getChain(999999L);
        });
        
        System.out.println("✓ 正确抛出接龙不存在异常");
    }

    @Test
    @DisplayName("测试：创建同名接龙冲突")
    void testDuplicateChainTitle() {
        // 创建第一个接龙
        ChainCreateRequest request = createSampleRequest();
        chainService.createChain(testUserId, request);

        // 创建同名接龙应该失败
        ChainException exception = assertThrows(ChainException.class, () -> {
            chainService.createChain(testUserId, request);
        });
        
        assertEquals(ChainException.ErrorCode.DUPLICATE_ENTRY, exception.getErrorCode());
        
        System.out.println("✓ 正确阻止创建同名接龙");
    }

    /**
     * 创建示例接龙请求
     */
    private ChainCreateRequest createSampleRequest() {
        ChainCreateRequest request = new ChainCreateRequest();
        request.setTitle("测试接龙");
        request.setDescription("这是一个测试接龙");
        request.setMaxParticipants(50);
        request.setAllowMultiple(false);
        request.setStartTime(LocalDateTime.now().minusHours(1));
        request.setEndTime(LocalDateTime.now().plusDays(1));
        return request;
    }
}
