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