-- 大屏抽奖记录表
CREATE TABLE IF NOT EXISTS lottery_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id VARCHAR(128) NOT NULL COMMENT '抽奖活动唯一标识',
    user_id VARCHAR(64) NOT NULL COMMENT '中奖用户ID',
    round INT NOT NULL COMMENT '抽奖轮次',
    won_at DATETIME(6) NOT NULL COMMENT '中奖时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    -- 结合分布式防超发要求，在数据库层面建立唯一索引兜底
    -- 确保同一个用户在同一个抽奖活动中，无论跨多少轮次绝对只能中奖一次
    UNIQUE KEY uk_activity_user (activity_id, user_id),
    
    -- 针对活动维度的历史翻查以及大屏展示优化的查询索引
    INDEX idx_activity_round (activity_id, round),
    INDEX idx_won_at (won_at)
);

-- 初始化示例测试数据（用于验证查询和翻页接口）
INSERT IGNORE INTO lottery_history (activity_id, user_id, round, won_at) VALUES 
('annual-2026', 'u_test_mock_1', 1, '2026-05-17 10:00:00'),
('annual-2026', 'u_test_mock_2', 1, '2026-05-17 10:00:00'),
('annual-2026', 'u_test_mock_3', 2, '2026-05-17 10:15:00'),
('annual-2026', 'u_test_mock_4', 2, '2026-05-17 10:15:00'),
('annual-2026-demo', 'u_test_mock_5', 1, '2026-05-17 11:00:00');