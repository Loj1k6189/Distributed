问卷统计`

### 主要调整
- `QuestList.vue`
    - 调用 `GET /api/questionnaires/active`
    - 适配后端 `{ success, data }` 返回结构
- `QuestCreate.vue`
    - 重写为新协议创建页
    - 支持创建者 ID、题目列表、题型、必答、匿名提交、最大提交次数、开始/结束时间
- `QuestSubmit.vue`
    - 先加载活跃问卷列表，再选择问卷
    - 调用 `GET /api/questionnaires/{id}` 获取详情
    - 调用 `POST /api/questionnaires/submit` 提交答卷
- `QuestStats.vue`
    - 先加载活跃问卷列表，再选择问卷
    - 调用 `GET /api/questionnaires/{id}/statistics`
    - 页面已按后端当前汇总统计结构重构

## 接龙模块新增
本次已为后端新增的接龙功能补齐前端。

### 新增页面
- `ChainList.vue`
- `ChainCreate.vue`
- `ChainJoin.vue`
- `ChainManage.vue`

### 新增路由
- `/chain/list`
- `/chain/create`
- `/chain/join/:id?`
- `/chain/manage/:id?`

### 页面能力
- `ChainList.vue`
    - 展示活跃接龙列表
    - 调用 `GET /api/chains/active`
- `ChainCreate.vue`
    - 支持创建者 ID、标题、描述、最大参与人数、重复参与设置、开始/结束时间
    - 调用 `POST /api/chains`
- `ChainJoin.vue`
    - 支持选择接龙、查看详情、提交接龙内容、可选回复某条历史接龙
    - 调用 `GET /api/chains/{id}` 和 `POST /api/chains/{id}/join`
- `ChainManage.vue`
    - 支持查看接龙记录
    - 支持使用创建者 ID 删除接龙
    - 调用 `GET /api/chains/{id}` 和 `DELETE /api/chains/{id}`

## 下拉框为空问题定位
### 现象
创建问卷/接龙后，在以下页面中下拉框仍然只有默认项“请选择...”：

- `参与问卷`
- `问卷统计`
- `参与接龙`
- `接龙管理`

### 排查结果
排查后确认：

- 前端下拉框逻辑正常
- 数据已经成功保存到数据库
- 详情接口可以正确返回刚创建的数据
- 真正返回空的是“活跃列表”接口

### 根因
问题本质是时间判断存在时区错配：

1. 前端 `datetime-local` 提交的是本地时间
2. 后端容器中的业务时间比较实际按 UTC 在运行
3. 后端因此误判“尚未开始”
4. 活跃列表接口返回空数组

这个问题不仅影响下拉框，也会影响参与时的“是否已开始”校验。

## 时间修复方案
本次修复分为两层：

### 1. 活跃列表查询放宽空开始时间逻辑
活跃判断调整为：

- `startTime` 为空时，视为立即生效
- `endTime` 为空时，视为未结束

### 2. 业务时间统一使用 `Asia/Shanghai`
在问卷和接龙服务中，显式使用：

```java
LocalDateTime.now(ZoneId.of("Asia/Shanghai"))
```

统一处理：

- 活跃列表判断
- 是否开始判断
- 是否结束判断
- 部分统计时间写入

## 主要修改文件
### 前端
- `front-end/src/App.vue`
- `front-end/src/router/index.ts`
- `front-end/src/components/QuestList.vue`
- `front-end/src/components/QuestCreate.vue`
- `front-end/src/components/QuestSubmit.vue`
- `front-end/src/components/QuestStats.vue`
- `front-end/src/components/ChainList.vue`
- `front-end/src/components/ChainCreate.vue`
- `front-end/src/components/ChainJoin.vue`
- `front-end/src/components/ChainManage.vue`

### 后端
- `backend/src/main/java/com/example/distributed/quest/repository/QuestionnaireRepository.java`
- `backend/src/main/java/com/example/distributed/chain/repository/ChainRepository.java`
- `backend/src/main/java/com/example/distributed/quest/service/QuestionnaireService.java`
- `backend/src/main/java/com/example/distributed/chain/service/ChainService.java`

## 验证情况
### 已完成
前端已完成构建验证：

```bash
cd front-end
npm run build
```

构建通过。

### 注意
后端时间修复属于服务端代码改动，必须在后端重新构建并重启后才能生效。若仍运行旧容器，可能继续出现：

- 活跃列表为空
- 下拉框看不到新建标题
- 设置了开始时间却仍被判定为未开始

## 建议回归测试
### 问卷
1. 创建一个开始时间为当前时间前后的问卷
2. 检查 `问卷列表` 是否可见
3. 检查 `参与问卷` 下拉框是否可选
4. 检查 `问卷统计` 下拉框是否可选
5. 提交答卷并检查统计是否更新

### 接龙
1. 创建一个开始时间为当前时间前后的接龙
2. 检查 `接龙列表` 是否可见
3. 检查 `参与接龙` 下拉框是否可选
4. 提交接龙内容并检查历史记录是否刷新
5. 检查 `接龙管理` 是否可选中并展示记录

## 总结
本次更新完成了三项关键工作：

1. 问卷前端全部迁移到当前正式接口
2. 接龙功能前端页面、路由和导航全部补齐
3. 修复了因时区错配导致的活跃列表为空问题