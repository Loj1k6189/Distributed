# 问卷系统 - 数据库设计与功能说明

## 📊 数据库设计

### 1. 核心实体关系

```
Questionnaire (1) ────< (N) Question (1) ────< (N) QuestionOption
      │
      │ (1)
      │
      └────< (N) QuestionnaireAnswer (1) ────< (N) QuestionAnswer
```

### 2. 数据表结构

#### 2.1 Questionnaire (问卷表)
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT (PK) | 问卷ID，自增主键 |
| title | VARCHAR(200) | 问卷标题 |
| description | TEXT | 问卷描述 |
| is_active | BOOLEAN | 是否启用 |
| created_by | VARCHAR | 创建者ID |
| max_submissions | INT | 最大提交次数 |
| allow_anonymous | BOOLEAN | 是否允许匿名 |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 结束时间 |
| version | BIGINT | 乐观锁版本号 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

#### 2.2 Question (题目表)
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT (PK) | 题目ID，自增主键 |
| questionnaire_id | BIGINT (FK) | 关联问卷ID |
| content | VARCHAR(500) | 题目内容 |
| question_type | ENUM | 题目类型 |
| sort_order | INT | 排序顺序 |
| is_required | BOOLEAN | 是否必填 |
| validation | VARCHAR(1000) | 验证规则 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

#### 2.3 QuestionOption (题目选项表)
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT (PK) | 选项ID，自增主键 |
| question_id | BIGINT (FK) | 关联题目ID |
| content | VARCHAR(500) | 选项内容 |
| sort_order | INT | 排序顺序 |
| is_correct | BOOLEAN | 是否为正确答案 |
| created_at | DATETIME | 创建时间 |

#### 2.4 QuestionnaireAnswer (问卷答卷表)
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT (PK) | 答卷ID，自增主键 |
| questionnaire_id | BIGINT (FK) | 关联问卷ID |
| user_id | VARCHAR | 用户ID |
| user_ip | VARCHAR | 用户IP |
| user_agent | VARCHAR | 用户代理 |
| is_anonymous | BOOLEAN | 是否匿名 |
| submitted_at | DATETIME | 提交时间 |
| completion_time | BIGINT | 完成时间(毫秒) |
| submit_version | INT | 提交版本号 |

**唯一约束**: (user_id, questionnaire_id) - 防止重复提交

#### 2.5 QuestionAnswer (题目答案表)
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT (PK) | 答案ID，自增主键 |
| questionnaire_answer_id | BIGINT (FK) | 关联答卷ID |
| question_id | BIGINT (FK) | 关联题目ID |
| text_answer | TEXT | 文本答案 |
| selected_option_ids | VARCHAR | 选中的选项ID（逗号分隔）|
| is_correct | BOOLEAN | 是否正确 |
| score | DOUBLE | 得分 |

#### 2.6 QuestionnaireEvent (问卷事件表)
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT (PK) | 事件ID |
| event_type | VARCHAR(50) | 事件类型 |
| questionnaire_id | BIGINT | 问卷ID |
| answer_id | BIGINT | 答案ID |
| user_id | VARCHAR | 用户ID |
| event_data | TEXT | 事件数据 |
| is_processed | BOOLEAN | 是否已处理 |
| processed_at | DATETIME | 处理时间 |
| retry_count | INT | 重试次数 |
| created_at | DATETIME | 创建时间 |

#### 2.7 OutboxMessage (出站消息表)
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT (PK) | 消息ID |
| aggregate_type | VARCHAR(50) | 聚合根类型 |
| aggregate_id | BIGINT | 聚合根ID |
| event_type | VARCHAR(100) | 事件类型 |
| event_data | TEXT | 事件数据 |
| status | VARCHAR(20) | 状态(PENDING/PUBLISHED/FAILED) |
| retry_count | INT | 重试次数 |
| max_retries | INT | 最大重试次数 |
| next_retry_at | DATETIME | 下次重试时间 |
| published_at | DATETIME | 发布时间 |
| error_message | TEXT | 错误信息 |
| created_at | DATETIME | 创建时间 |

#### 2.8 QuestionnaireStatistics (问卷统计表)
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT (PK) | 统计ID |
| questionnaire_id | BIGINT | 问卷ID |
| total_submissions | BIGINT | 总提交数 |
| completed_submissions | BIGINT | 完成提交数 |
| partial_submissions | BIGINT | 部分提交数 |
| average_completion_time | DOUBLE | 平均完成时间 |
| unique_users | BIGINT | 唯一用户数 |
| anonymous_submissions | BIGINT | 匿名提交数 |
| last_submission_at | DATETIME | 最后提交时间 |
| snapshot_date | DATETIME | 快照日期 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### 3. 题目类型枚举 (QuestionType)

| 枚举值 | 说明 | 是否有选项 |
|--------|------|------------|
| SINGLE_CHOICE | 单选题 | ✓ |
| MULTIPLE_CHOICE | 多选题 | ✓ |
| TEXT_ANSWER | 问答题 | ✗ |
| RATING | 评分题 | ✗ |
| DATE | 日期题 | ✗ |

## 🔧 分布式特性

### 1. Redis分布式锁
- **用途**: 防止并发提交冲突
- **实现**: 使用Redis的SET NX命令
- **Key格式**: `lock:questionnaire:submit:{userId}:{questionnaireId}`
- **超时**: 10秒

### 2. Redis计数器
- **用途**: 防重复提交、统计提交次数
- **Key格式**: `counter:questionnaire:submission:{userId}:{questionnaireId}`
- **TTL**: 30天

### 3. 防重复提交机制
- **数据库层面**: 唯一约束 (user_id, questionnaire_id)
- **Redis层面**: 计数器记录提交次数
- **业务层面**: 检查最大提交次数限制

### 4. 事务管理
- 使用 `@Transactional` 确保数据一致性
- 支持回滚机制
- 乐观锁控制并发更新

### 5. 事件驱动架构
- **QuestionnaireEvent**: 记录所有业务事件
- **定时处理**: 每5秒处理未处理事件
- **重试机制**: 最多重试3次

### 6. Outbox Pattern
- **用途**: 确保消息可靠投递到消息队列
- **定时发布**: 每3秒发布待发送消息
- **指数退避**: 重试延迟呈指数增长
- **最大重试**: 默认3次

### 7. 统计快照
- **实时更新**: 每次提交后更新统计
- **定时快照**: 每天生成统计快照
- **缓存支持**: 支持Redis缓存查询结果

## 📡 API接口

### 1. 创建问卷
```http
POST /api/questionnaires
Headers: X-User-Id: {userId}
Body: {
  "title": "问卷标题",
  "description": "问卷描述",
  "maxSubmissions": 1,
  "allowAnonymous": false,
  "startTime": "2026-06-01T00:00:00",
  "endTime": "2026-06-30T23:59:59",
  "questions": [
    {
      "content": "题目内容",
      "questionType": "SINGLE_CHOICE",
      "sortOrder": 0,
      "isRequired": true,
      "options": [
        {"content": "选项A", "sortOrder": 0},
        {"content": "选项B", "sortOrder": 1}
      ]
    }
  ]
}
```

### 2. 获取问卷详情
```http
GET /api/questionnaires/{id}
```

### 3. 获取活跃问卷列表
```http
GET /api/questionnaires/active
```

### 4. 提交答卷
```http
POST /api/questionnaires/submit
Headers: 
  X-User-Id: {userId}
  X-Forwarded-For: {userIp}
  User-Agent: {userAgent}
Body: {
  "questionnaireId": 1,
  "isAnonymous": false,
  "startTime": 1717200000000,
  "answers": [
    {
      "questionId": 1,
      "selectedOptionIds": [1, 2]
    },
    {
      "questionId": 2,
      "textAnswer": "我的回答"
    }
  ]
}
```

### 5. 获取问卷统计
```http
GET /api/questionnaires/{id}/statistics
```

## 📦 技术栈

- **Spring Boot 4.0.6**
- **Spring Data JPA** - 数据访问层
- **MySQL** - 关系型数据库
- **Redis** - 缓存和分布式锁
- **Lombok** - 简化代码
- **Bean Validation** - 参数验证

## 🚀 快速开始

### 1. 环境要求
- Java 17+
- MySQL 5.7+
- Redis 6.0+

### 2. 数据库配置
确保 `application.properties` 中配置正确：
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/vote_db
spring.datasource.username=root
spring.datasource.password=123456
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

### 3. 启动应用
```bash
cd backend
mvn spring-boot:run
```

### 4. 自动建表
Hibernate会根据实体类自动创建数据库表（`spring.jpa.hibernate.ddl-auto=update`）

## 📝 注意事项

1. **分布式锁**: 确保Redis服务可用，否则提交操作会失败
2. **并发控制**: 使用乐观锁防止数据覆盖
3. **幂等性**: 同一用户对同一问卷只能提交一次（除非配置了maxSubmissions）
4. **事件处理**: 异步事件处理，可能有延迟
5. **缓存**: 问卷详情使用Redis缓存，修改后自动失效
6. **时间控制**: 问卷有开始和结束时间，非活动期间无法提交

## 🔐 安全性

- 使用Spring Security进行认证授权
- 用户ID通过Header传递（X-User-Id）
- 支持匿名提交
- 记录用户IP和UserAgent
- 防重复提交机制

## 📊 监控

- Actuator端点: `/actuator/health`, `/actuator/metrics`
- Prometheus指标导出
- OpenTelemetry链路追踪
- 日志记录所有关键操作
