-- 合并所有数据库初始化脚本
-- 投票系统表结构
-- 投票活动表
CREATE TABLE IF NOT EXISTS vote_poll (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL COMMENT '投票活动名称',
    allow_multiple BOOLEAN DEFAULT FALSE COMMENT '是否允许多选: TRUE-多选, FALSE-单选',
    status VARCHAR(16) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE-活跃, CLOSED-关闭',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_poll_status (status),
    INDEX idx_poll_created_at (created_at)
) COMMENT='投票活动表';

-- 投票选项表
CREATE TABLE IF NOT EXISTS vote_option (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    poll_id BIGINT NOT NULL COMMENT '关联投票活动ID',
    option_text VARCHAR(128) NOT NULL COMMENT '选项文本',
    sort_no INT DEFAULT 0 COMMENT '排序编号',
    FOREIGN KEY (poll_id) REFERENCES vote_poll(id) ON DELETE CASCADE,
    INDEX idx_vote_option_poll (poll_id),
    INDEX idx_vote_option_sort (sort_no)
) COMMENT='投票选项表';

-- 投票计数表
CREATE TABLE IF NOT EXISTS vote_option_count (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    poll_id BIGINT NOT NULL COMMENT '投票活动ID',
    option_id BIGINT NOT NULL COMMENT '选项ID',
    vote_count BIGINT DEFAULT 0 COMMENT '投票计数',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    UNIQUE KEY uk_vote_count_poll_option (poll_id, option_id),
    FOREIGN KEY (poll_id) REFERENCES vote_poll(id) ON DELETE CASCADE,
    FOREIGN KEY (option_id) REFERENCES vote_option(id) ON DELETE CASCADE,
    INDEX idx_vote_option_count_poll (poll_id),
    INDEX idx_vote_option_count_option (option_id)
) COMMENT='投票计数表';

-- 投票事件表
CREATE TABLE IF NOT EXISTS vote_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL COMMENT '事件ID',
    poll_id BIGINT NOT NULL COMMENT '投票活动ID',
    voter_id VARCHAR(64) NOT NULL COMMENT '投票人ID',
    source_ip VARCHAR(64) NOT NULL COMMENT '来源IP地址',
    option_ids VARCHAR(512) NOT NULL COMMENT '选项ID列表，逗号分隔',
    created_at TIMESTAMP NOT NULL COMMENT '创建时间',
    UNIQUE KEY uk_vote_event_id (event_id),
    INDEX idx_vote_event_poll_created (poll_id, created_at),
    INDEX idx_vote_event_voter (voter_id),
    FOREIGN KEY (poll_id) REFERENCES vote_poll(id) ON DELETE CASCADE
) COMMENT='投票事件表';

-- 投票出箱表 (用于事件发布)
CREATE TABLE IF NOT EXISTS vote_outbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL COMMENT '事件ID',
    payload TEXT NOT NULL COMMENT '事件载荷JSON',
    status VARCHAR(16) NOT NULL DEFAULT 'NEW' COMMENT '状态: NEW-新建, SENT-已发送, FAILED-失败',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    next_retry_at TIMESTAMP NULL COMMENT '下次重试时间',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_vote_outbox_event (event_id),
    INDEX idx_vote_outbox_status_retry (status, next_retry_at)
) COMMENT='投票出箱表';

-- 投票计数快照表
CREATE TABLE IF NOT EXISTS vote_count_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    poll_id BIGINT NOT NULL COMMENT '投票活动ID',
    option_id BIGINT NOT NULL COMMENT '选项ID',
    vote_count BIGINT NOT NULL DEFAULT 0 COMMENT '快照时的投票计数',
    snapshot_at DATE NOT NULL COMMENT '快照日期',
    UNIQUE KEY uk_vote_snapshot_poll_option (poll_id, option_id, snapshot_at),
    INDEX idx_vote_snapshot_poll (poll_id),
    INDEX idx_vote_snapshot_option (option_id),
    INDEX idx_vote_snapshot_date (snapshot_at),
    FOREIGN KEY (poll_id) REFERENCES vote_poll(id) ON DELETE CASCADE,
    FOREIGN KEY (option_id) REFERENCES vote_option(id) ON DELETE CASCADE
) COMMENT='投票计数快照表';

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

-- 问卷主表
CREATE TABLE IF NOT EXISTS questionnaires (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL COMMENT '问卷标题',
    description TEXT COMMENT '问卷描述',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-关闭, 1-开启',
    creator_id VARCHAR(64) NOT NULL COMMENT '创建者ID',
    allow_multiple TINYINT DEFAULT 0 COMMENT '是否允许多选: 0-单选, 1-多选',
    max_options INT DEFAULT 1 COMMENT '最多可选项数量(多选时)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL COMMENT '过期时间',
    INDEX idx_creator_id (creator_id),
    INDEX idx_status (status),
    INDEX idx_expires_at (expires_at)
);

-- 问卷选项表
CREATE TABLE IF NOT EXISTS question_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL COMMENT '关联问卷ID',
    option_type VARCHAR(20) NOT NULL DEFAULT 'TEXT' COMMENT '选项类型: TEXT-文本, TEXTAREA-文本域, RADIO-单选按钮, CHECKBOX-复选框, SELECT-下拉选择, IMAGE-图片, VIDEO-视频, FILE-文件, RATING-评分, BOOLEAN-布尔, DATE-日期, TIME-时间, DATETIME-日期时间, EMAIL-邮箱, PHONE-电话, NUMBER-数字',
    option_key VARCHAR(100) NOT NULL COMMENT '选项键名，用于程序化引用',
    option_value TEXT NOT NULL COMMENT '选项值，根据类型不同存储不同内容',
    placeholder TEXT COMMENT '占位提示文本',
    validation_rule JSON COMMENT '验证规则，JSON格式存储',
    sort_order INT DEFAULT 0 COMMENT '排序序号',
    is_required TINYINT DEFAULT 0 COMMENT '是否必填: 0-否, 1-是',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (question_id) REFERENCES questionnaires(id) ON DELETE CASCADE,
    INDEX idx_question_id (question_id),
    INDEX idx_sort_order (sort_order),
    UNIQUE KEY uk_question_key (question_id, option_key)
);

-- 提交记录表
CREATE TABLE IF NOT EXISTS question_submissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL COMMENT '问卷ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    client_ip VARCHAR(45) COMMENT '客户端IP',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    FOREIGN KEY (question_id) REFERENCES questionnaires(id) ON DELETE CASCADE,
    UNIQUE KEY uk_question_user (question_id, user_id), -- 防止重复提交
    INDEX idx_created_at (created_at),
    INDEX idx_user_id (user_id)
);

-- 提交选项表
CREATE TABLE IF NOT EXISTS submission_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id BIGINT NOT NULL COMMENT '提交记录ID',
    option_id BIGINT NOT NULL COMMENT '选项ID',
    option_value TEXT COMMENT '提交的选项值，根据类型不同存储不同内容',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (submission_id) REFERENCES question_submissions(id) ON DELETE CASCADE,
    FOREIGN KEY (option_id) REFERENCES question_options(id) ON DELETE CASCADE,
    INDEX idx_submission_id (submission_id),
    INDEX idx_option_id (option_id)
);

-- 统计快照表
CREATE TABLE IF NOT EXISTS question_stats_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL COMMENT '问卷ID',
    snapshot_date DATE NOT NULL COMMENT '快照日期',
    option_id BIGINT NOT NULL COMMENT '选项ID',
    vote_count BIGINT NOT NULL DEFAULT 0 COMMENT '投票数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_question_date_option (question_id, snapshot_date, option_id),
    FOREIGN KEY (question_id) REFERENCES questionnaires(id) ON DELETE CASCADE,
    FOREIGN KEY (option_id) REFERENCES question_options(id) ON DELETE CASCADE
);

-- 问卷事件表
CREATE TABLE IF NOT EXISTS question_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL COMMENT '事件ID',
    question_id BIGINT NOT NULL COMMENT '问卷ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    source_ip VARCHAR(64) NOT NULL COMMENT '源IP',
    option_ids TEXT NOT NULL COMMENT '选项ID列表，逗号分隔',
    created_at TIMESTAMP NOT NULL COMMENT '创建时间',
    UNIQUE KEY uk_question_event_id (event_id),
    INDEX idx_question_created (question_id, created_at)
);

-- 问卷Outbox表
CREATE TABLE IF NOT EXISTS question_outbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL COMMENT '事件ID',
    payload TEXT NOT NULL COMMENT '消息负载',
    status VARCHAR(16) NOT NULL COMMENT '状态: NEW, SENT, FAILED',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    next_retry_at TIMESTAMP NULL COMMENT '下次重试时间',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_question_outbox_event (event_id),
    INDEX idx_status_retry (status, next_retry_at)
);