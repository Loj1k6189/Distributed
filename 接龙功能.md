# 接龙（Chain）系统设计文档

## 📋 概述

接龙系统是一个支持高并发、分布式环境下的接龙功能实现，解决了公司场景中常见的接龙一致性问题。

## 🎯 解决的分布式问题

### 1. **并发追加冲突**
- **问题**：多人同时追加接龙内容导致顺序混乱
- **解决方案**：使用Redis分布式锁（`DistributedLockService`）保证同一接龙的追加操作串行化
- **实现**：在`ChainService.joinChain()`方法中使用`tryLock/Unlock`保护临界区

### 2. **重复接龙**
- **问题**：同一用户多次参与同一接龙
- **解决方案**：数据库唯一约束 + 业务层校验
- **实现**：
  - 数据库：`UNIQUE INDEX idx_chain_user (chain_id, user_id)`
  - 代码：`chainEntryRepository.existsByChainIdAndUserId()` 前置检查

### 3. **顺序一致性**
- **问题**：接龙序号不连续或重复
- **解决方案**：使用数据库序列号生成器，在分布式锁保护下生成
- **实现**：`chainEntryRepository.findMaxSequenceNoByChainId()` + 1

### 4. **乐观锁防并发更新**
- **问题**：多人同时修改同一接龙导致数据覆盖
- **解决方案**：JPA `@Version` 字段实现乐观锁
- **实现**：`Chain`实体的`version`字段，更新时自动检查版本号

### 5. **缓存一致性**
- **问题**：接龙数据更新后缓存未失效
- **解决方案**：使用`@CacheEvict`在写操作时清除缓存
- **实现**：`createChain/joinChain/deleteChain`方法都标注了`@CacheEvict`

## 🗄️ 数据库设计

### Chain表（接龙主表）
```sql
CREATE TABLE chain (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    created_by VARCHAR(100) NOT NULL,
    max_participants INT,
    allow_multiple BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    start_time DATETIME,
    end_time DATETIME,
    version BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_created_by_title (created_by, title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### ChainEntry表（接龙项表）
```sql
CREATE TABLE chain_entry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chain_id BIGINT NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    content VARCHAR(1000),
    sequence_no BIGINT NOT NULL,
    parent_entry_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_chain_user (chain_id, user_id),
    INDEX idx_chain_sequence (chain_id, sequence_no),
    FOREIGN KEY (chain_id) REFERENCES chain(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### ChainEvent表（事件表）
```sql
CREATE TABLE chain_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    chain_id BIGINT NOT NULL,
    entry_id BIGINT,
    user_id VARCHAR(100),
    event_data TEXT,
    is_processed BOOLEAN DEFAULT FALSE,
    processed_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_unprocessed (is_processed, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 🔌 API接口

### 1. 创建接龙
```http
POST /api/chains
Content-Type: application/json
X-User-Id: user123

{
  "title": "今日午餐接龙",
  "description": "请大家接龙报餐",
  "maxParticipants": 50,
  "allowMultiple": false,
  "startTime": "2024-01-01T00:00:00",
  "endTime": "2024-01-01T23:59:59"
}
```

**响应：**
```json
{
  "success": true,
  "message": "接龙创建成功",
  "data": {
    "id": 1
  }
}
```

### 2. 获取接龙详情
```http
GET /api/chains/{id}
```

**响应：**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "今日午餐接龙",
    "description": "请大家接龙报餐",
    "createdBy": "user123",
    "maxParticipants": 50,
    "allowMultiple": false,
    "isActive": true,
    "startTime": "2024-01-01T00:00:00",
    "endTime": "2024-01-01T23:59:59",
    "createdAt": "2024-01-01T10:00:00",
    "participantCount": 3,
    "entries": [
      {
        "id": 1,
        "chainId": 1,
        "userId": "user001",
        "content": "1号套餐",
        "sequenceNo": 1,
        "createdAt": "2024-01-01T10:05:00"
      }
    ]
  }
}
```

### 3. 获取所有活跃接龙
```http
GET /api/chains/active
```

### 4. 参与接龙
```http
POST /api/chains/{id}/join
Content-Type: application/json
X-User-Id: user456

{
  "content": "2号套餐",
  "parentEntryId": null
}
```

**响应：**
```json
{
  "success": true,
  "message": "接龙成功",
  "data": {
    "entryId": 2
  }
}
```

### 5. 删除接龙
```http
DELETE /api/chains/{id}
X-User-Id: user123
```

## 📦 项目结构

```
backend/src/main/java/com/example/distributed/chain/
├── controller/
│   ├── ChainController.java          # REST API控制器
│   └── ChainExceptionHandler.java    # 全局异常处理器
├── dto/
│   ├── ChainCreateRequest.java       # 创建接龙请求
│   ├── ChainEntryRequest.java        # 参与接龙请求
│   ├── ChainResponse.java            # 接龙响应
│   └── ChainEntryResponse.java       # 接龙项响应
├── entity/
│   ├── Chain.java                    # 接龙实体
│   ├── ChainEntry.java               # 接龙项实体
│   └── ChainEvent.java               # 接龙事件实体
├── exception/
│   └── ChainException.java           # 业务异常类
├── repository/
│   ├── ChainRepository.java          # 接龙Repository
│   ├── ChainEntryRepository.java     # 接龙项Repository
│   └── ChainEventRepository.java     # 事件Repository
└── service/
    └── ChainService.java             # 核心业务服务

quest/service/
└── DistributedLockService.java       # 分布式锁服务
```

## 🔒 分布式锁实现

### 使用场景
在`ChainService.joinChain()`方法中，使用Redis分布式锁保证接龙追加的顺序一致性：

```java
String lockKey = String.format("chain:%s:join", chainId);
if (!lockService.tryLock(lockKey)) {
    throw new ChainException(ChainException.ErrorCode.CHAIN_LOCKED);
}

try {
    return doJoinChain(userId, chainId, request);
} finally {
    lockService.unlock(lockKey);
}
```

### 锁的特性
- **非阻塞**：使用`SETNX`命令，获取失败立即返回
- **自动释放**：设置10秒过期时间，防止死锁
- **原子操作**：Redis单线程保证操作的原子性

## ⚡ 性能优化

### 1. Redis缓存
```java
@Cacheable(value = "chains", key = "#id")
@Transactional
public ChainResponse getChain(Long id) {
    // ...
}
```

### 2. 数据库索引
- `idx_chain_user`: 唯一约束，防止重复接龙，优化查询
- `idx_chain_sequence`: 优化接龙列表排序查询
- `idx_unprocessed`: 优化未处理事件查询

### 3. 分页查询
接龙项按`sequence_no`排序，支持分页加载

## 🚨 异常处理

### 业务异常码
| 错误码 | 说明 | HTTP状态码 |
|--------|------|-----------|
| CHAIN_001 | 接龙不存在 | 404 |
| CHAIN_002 | 接龙已停止 | 400 |
| CHAIN_003 | 接龙已满 | 400 |
| CHAIN_004 | 接龙已过期 | 400 |
| CHAIN_005 | 接龙尚未开始 | 400 |
| CHAIN_006 | 您已参与此接龙 | 409 |
| CHAIN_007 | 此接龙不允许重复参与 | 409 |
| CHAIN_008 | 接龙项数已达上限 | 400 |
| CHAIN_009 | 接龙正在被编辑，请稍后重试 | 429 |
| CHAIN_010 | 接龙序号无效 | 400 |

## 🧪 测试建议

### 并发测试
```bash
# 模拟100个用户同时参与接龙
for i in {1..100}; do
  curl -X POST http://localhost:8080/api/chains/1/join \
    -H "X-User-Id: user$i" \
    -H "Content-Type: application/json" \
    -d '{"content": "用户'$i'的接龙"}' &
done
```

### 验证点
1. ✅ 所有接龙序号连续且不重复
2. ✅ 同一用户无法重复参与（当allowMultiple=false）
3. ✅ 接龙人数不超过maxParticipants限制
4. ✅ 接龙过期后无法参与

## 🔧 扩展建议

### 1. 消息队列集成
将`ChainEvent`与RabbitMQ/Kafka集成，实现异步事件处理

### 2. WebSocket实时推送
使用WebSocket将新接龙项实时推送给所有在线用户

### 3. 分库分表
当接龙数据量大时，按`chain_id`分片存储

### 4. 限流保护
使用Redis实现接口限流，防止恶意刷接龙

## 📝 注意事项

1. **Redis依赖**：系统需要Redis作为分布式锁和缓存支持
2. **时钟同步**：多服务器部署时需要NTP时间同步
3. **数据库事务**：所有写操作都在事务中执行，保证数据一致性
4. **锁超时**：分布式锁设置10秒超时，防止死锁
5. **缓存失效**：写操作自动清除相关缓存

## 🚀 部署

### 环境要求
- Java 17+
- Spring Boot 3.x
- MySQL 8.0+
- Redis 6.0+

### 配置示例
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/quest_db
    username: root
    password: password
  redis:
    host: localhost
    port: 6379
    password: 
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

## 📄 许可证

MIT License
