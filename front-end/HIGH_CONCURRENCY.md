# 前端高并发支持说明

本项目已实现并集成一组面向高并发的前端改造，包含代码更改、使用说明与实践建议。

## 已新增的代码与改动

- `src/lib/http.ts`：轻量的 HTTP 客户端封装
	- 功能：并发限制（默认 6）、请求队列、重试（指数退避，默认 2 次）、支持 `Idempotency-Key`。
	- 返回值：直接返回后端响应的 `data`（与 axios 不同，不再使用 `res.data`）。
- `src/lib/debounce.ts`：通用去抖工具（用于防止短周期频繁触发）。
- `src/lib/httpPlugin.ts`：将 `http` 暴露为 Vue 插件，已在 `src/main.ts` 中注册，可通过 `app.config.globalProperties.$http` 访问。

已替换为使用 `http` 的组件（关键路径）：
- `src/components/Admin.vue`（DLQ 重试、重建、快照）
- `src/components/VoteSubmit.vue`（投票：并发保护、idempotency、乐观更新）
- `src/components/LotteryJoin.vue`（参与抽奖：idempotency、乐观更新）
- `src/components/LotteryDraw.vue`, `LotteryWinners.vue`, `LotteryHistory.vue`（抽奖相关查询/触发）
- 问卷相关组件：`QuestCreate.vue`, `QuestList.vue`, `QuestSubmit.vue`, `QuestStats.vue` 等

此外：
- 在 `src/App.vue` 中补齐了抽奖相关导航入口，方便访问。

## 如何使用 `http`

导入直接使用：

```ts
import { http } from './lib/http'

await http.get('/api/votes/polls')
await http.post('/api/votes/submit', payload, { idempotencyKey: 'uuid-v1' })
```

或者使用全局插件：组件内可通过 `const http = getCurrentInstance().appContext.config.globalProperties.$http` 访问，或直接导入 `http`。

注意：`http` 返回的是后端 `data`，不需要再访问 `.data`。

`RequestOptions` 支持额外字段：
- `idempotencyKey?: string` — 并发写入时用于后端幂等校验。
- `retries?: number` — 覆盖默认重试次数（默认 2）。

## 乐观更新示例（已实现）

- `VoteSubmit.vue`：在提交投票时，先在 UI 层本地增加票数并标记为已投（减少等待感），若后端失败则回滚到先前状态。
- `LotteryJoin.vue`：在发起参与时显示临时成功信息，后端失败会回滚并显示错误。

乐观更新注意事项：
- 必须与后端幂等/验证结合，避免重复写入。前端通过 `Idempotency-Key` 提供幂等性保障。
- 对于强一致性场景（余额、库存等），谨慎使用乐观更新，并提供回滚与用户提示。

## 列表与虚拟滚动建议

- 对于可能呈现大量条目的页面（如 `LotteryHistory`、问卷/投票列表），建议使用后端分页 + 前端虚拟滚动组件（例如 `vue-virtual-scroller`），避免一次性拉取和渲染大量数据。
- 实现步骤：后端保证分页接口；前端使用 `http.get('/.../history?page=X&size=Y')` 分页加载，使用虚拟滚动组件只渲染可视区域。

## 并发限制与队列

- `http` 的并发限制默认设置为 6（浏览器常见并发限制），可以在 `src/lib/http.ts` 中修改构造参数以调优。
- 关键页面应限制并发请求，例如图片/资源浅加载或复杂查询时控制同时请求数。

## 监控与可观测性

建议集成以下能力：
- 错误与性能埋点：Sentry 或其他 RUM（前端错误、慢请求、长任务等）。
- 请求追踪：在 `http` 中自动添加 `trace-id`（若后端支持），并将其上报到日志/监控以便关联前后端请求。

示例：在 `src/lib/http.ts` 的请求拦截器中添加 `X-Trace-Id`：

```ts
// 简要示意
const traceId = generateTraceId()
headers['X-Trace-Id'] = traceId
```

并在后端日志中关联该 `trace-id`。

## 压测（load-testing）建议与示例脚本

- 使用 `k6` 或 `artillery` 对关键接口做压力测试（投票提交、抽奖参与、查询历史）。
- 在仓库中可添加 `front-end/load-test/vote_submit.js`（示例）并在 CI 或本地运行：

```bash
# 安装 k6 (本机或 CI 环境)
# macOS/Linux: brew install k6 或 下载二进制
k6 run front-end/load-test/vote_submit.js
```

示例测试应包含：并发提交、随机用户ID、检查后端是否返回成功、并发下是否有重复写入或错误率飙升。

## 安全与幂等性

- 所有写操作（投票、抽奖参与、问卷提交）应携带 `Idempotency-Key`。后端需要去重并返回明确的语义（重复提交应返回 200 并指出已存在）。
- 前端不应依赖仅靠响应码判断业务成功，应以后端语义字段（如 `alreadyProcessed` / `submitted`）为准。

## 本次变更清单（代码文件）

- 新增：
	- `src/lib/http.ts`
	- `src/lib/debounce.ts`
	- `src/lib/httpPlugin.ts`
	- `front-end/HIGH_CONCURRENCY.md`（本文件）
- 修改：
	- `src/main.ts` （注册插件）
	- 多个组件替换 `axios` 为 `http`：`Admin.vue`, `VoteSubmit.vue`, `LotteryJoin.vue`, `LotteryDraw.vue`, `LotteryWinners.vue`, `LotteryHistory.vue`, `CreatePoll.vue`, `QuestCreate.vue`, `QuestList.vue`, `QuestSubmit.vue`, `QuestStats.vue`, `VoteResult.vue` 等。

## 下一步（可选，我可以为你执行）

1. 将 `http` 做成 Vue 插件并替换全仓库剩余的 `axios` 用法（已替换关键组件，剩余文件较少）。
2. 在 CI 中加入简单的压力测试作业（运行 k6 脚本）。
3. 为关键写操作实现统一的乐观更新/补偿模式库（辅助回滚）。
4. 集成 Sentry 或其他 RUM 服务，并在 `http` 中自动上报慢请求与错误。

如果你希望我继续，我可以：
- 完成全量替换所有 `axios` 的调用（并提交变更）；
- 添加示例 `k6` 脚本到 `front-end/load-test/`；
- 在 `VoteSubmit` 中增加更多并发保护与本地缓存示例。

---

文件位置：`front-end/HIGH_CONCURRENCY.md`

