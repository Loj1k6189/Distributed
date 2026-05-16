# Distributed Backend（大型现场投票系统后端）

## 1. 项目实现需求

本项目面向“大型现场投票”场景，后端已实现以下核心需求：

1. **投票活动管理**：创建投票活动，支持单选/多选。
2. **高并发投票写入**：投票先写 Redis 计数，快速返回受理结果。
3. **异步可靠落库**：通过 **Outbox + Redis Streams** 异步持久化到 MySQL。
4. **幂等与防重**：基于事件 ID 做幂等消费，避免重复计票。
5. **限流防刷**：按用户与 IP 进行滑动窗口限流。
6. **并发一致性控制**：通过 Redlock 风格分布式锁保护关键更新区。
7. **故障恢复能力**：支持计数快照、按事件回放重建 Redis 计数。
8. **死信重试**：异常消息进入 DLQ Stream，可通过管理接口重试。
9. **实时能力预留**：提供 WebSocket/STOMP 基础配置用于结果推送扩展。

---

## 2. 架构与数据流（当前版本）

投票提交主链路：

1. 客户端调用 `/api/votes/submit`
2. 服务端校验活动状态、选项合法性、限流
3. 获取分布式锁并更新 Redis 计数
4. 写入 Outbox 表（MySQL）
5. 发布事件到 Redis Stream（`app.vote.mq-stream-key`）
6. 消费者组批量消费并持久化事件与选项计数
7. ACK 成功消息；异常消息转入 DLQ Stream

恢复链路：

1. 定时快照将 MySQL 计数与票数快照化
2. 当 Redis 故障时可通过管理接口重建
3. 按快照时间点后事件进行 replay，恢复最新计数

---

## 3. 技术栈与版本

> 以下为仓库内可直接确认的版本；未单独写死版本的依赖由 Spring Boot BOM 统一管理。

### 3.1 运行环境

| 组件 | 版本 |
|---|---|
| Java | **17** |
| Spring Boot Parent | **4.0.6** |
| 项目版本 | **0.0.1-SNAPSHOT** |
| MySQL | **8.x**（本地运行指引） |
| Redis | **6.2+ / 7.x 推荐**（需支持 Streams） |

### 3.2 主要后端依赖（由 Spring Boot 4.0.6 BOM 管理）

| 依赖 | 版本来源 |
|---|---|
| spring-boot-starter-webmvc | Spring Boot **4.0.6** BOM |
| spring-boot-starter-data-jpa | Spring Boot **4.0.6** BOM |
| spring-boot-starter-data-redis | Spring Boot **4.0.6** BOM |
| spring-boot-starter-security | Spring Boot **4.0.6** BOM |
| spring-boot-starter-websocket | Spring Boot **4.0.6** BOM |
| spring-boot-starter-validation | Spring Boot **4.0.6** BOM |
| spring-boot-starter-json | Spring Boot **4.0.6** BOM |
| spring-boot-starter-actuator | Spring Boot **4.0.6** BOM |
| micrometer-registry-prometheus | Spring Boot **4.0.6** BOM |
| micrometer-tracing-bridge-otel | Spring Boot **4.0.6** BOM |
| opentelemetry-exporter-otlp | Spring Boot **4.0.6** BOM |
| mysql-connector-j | Spring Boot **4.0.6** BOM |
| lombok | Spring Boot **4.0.6** BOM |
| h2（测试/本地） | Spring Boot **4.0.6** BOM |

### 3.3 明确声明版本的构建插件

| 插件 | 版本 |
|---|---|
| asciidoctor-maven-plugin | **2.2.1** |

---

## 4. 主要接口

### 4.1 业务接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/votes/polls` | 创建投票活动 |
| POST | `/api/votes/submit` | 提交投票 |
| GET | `/api/votes/polls/{pollId}/result` | 查询投票结果 |

#### 创建投票（示例）

```json
{
  "name": "最佳方案评选",
  "allowMultiple": false,
  "options": ["方案A", "方案B", "方案C"]
}
```

#### 提交投票（示例）

```json
{
  "pollId": 1,
  "voterId": "user-1001",
  "optionIds": [2]
}
```

### 4.2 管理接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/votes/admin/recovery/rebuild` | 从快照+事件回放重建 Redis 计数 |
| POST | `/api/votes/admin/snapshot` | 立即创建一次快照 |
| POST | `/api/votes/admin/dlq/retry?limit=100` | 重试 DLQ Stream 消息 |

### 4.3 WebSocket

| 类型 | 配置 |
|---|---|
| STOMP 连接端点 | `/ws` |
| Topic 前缀 | `/topic` |
| Application 前缀 | `/app` |

### 4.4 错误响应格式

```json
{
  "code": "VALIDATION_ERROR",
  "message": "optionIds must not be empty",
  "timestamp": "2026-05-13T12:34:56Z"
}
```

---

## 5. 快速启动流程

## 5.1 前置准备

1. 启动 MySQL 8（默认：`localhost:3306`，`root/123456`）
2. 启动 Redis（默认：`localhost:6379`）
3. 确认 `src/main/resources/application.properties` 配置可用

## 5.2 启动应用（Windows）

```bash
mvnw.cmd spring-boot:run
```

或打包运行：

```bash
mvnw.cmd clean package
java -jar target/Distributed-0.0.1-SNAPSHOT.jar
```

## 5.3 核心配置项（消息链路）

| 配置项 | 默认值 | 说明 |
|---|---|---|
| app.vote.mq-enabled | true | 是否启用消息链路 |
| app.vote.mq-stream-key | vote.event.stream | 主事件 Stream |
| app.vote.mq-dlq-stream-key | vote.event.dlq.stream | DLQ Stream |
| app.vote.mq-consumer-group | vote.event.group | 消费者组 |
| app.vote.mq-consumer-name | vote.event.consumer | 消费者名 |
| app.vote.mq-batch-size | 100 | 批量消费大小 |
| app.vote.consumer-poll-delay-ms | 500 | 消费轮询间隔 |
| app.vote.outbox-relay-delay-ms | 3000 | Outbox relay 轮询间隔 |

---

## 6. 监控与可观测性

| 能力 | 配置/端点 |
|---|---|
| Actuator | `/actuator` |
| Prometheus 指标 | `/actuator/prometheus` |
| Tracing 采样率 | `management.tracing.sampling.probability=1.0` |
| OTLP 导出端点 | `http://localhost:4318/v1/traces` |

---

## 7. 当前安全策略说明

当前 `SecurityConfig` 为开发友好模式：

- 关闭 CSRF
- 所有请求放行（`anyRequest().permitAll()`）
- 开启 HTTP Basic 默认配置

生产部署前建议补齐鉴权、细粒度授权、CORS 与限流网关策略。
