# 大屏抽奖服务 (Role B) API 接口文档

## 概述
本接口文档提供大屏抽奖功能支持，包含参与抽奖池、执行原子抽奖、实时获取中奖结果与历史记录查询接口。系统通过 Redis SPOP 保证单轮抽奖强原子性与抗超发，利用 WebSocket 与 Redis Pub/Sub 给所有大屏客户端实时推送播报最新中奖情况。

---

## 1. REST API 接口

### 1.1 加入抽奖池 (Join Pool)
将指定用户放入当前抽奖活动的奖池中，通过 `Set` 的特性自动实现幂等（不可重复入池）。

**URL**: `/api/lottery/{activityId}/join`
**Method**: `POST`

**Request Parameters**:
- `userId` (String): 用户独立标识

**返回结果**: `200 OK` (无返回值)

---

### 1.2 执行抽奖 (Draw)
基于原子队列进行奖池拉取，抽取同时向连接 WebSocket 的大屏进行全局播报，并落库 MySQL 持久化。

**URL**: `/api/lottery/{activityId}/draw`
**Method**: `POST`

**Request Parameters**:
- `round` (Int): 抽奖轮次 (如第 1 轮抽取三等奖)
- `count` (Int): 本轮抽奖人数

**返回 (List<String>)**:
```json
[
  "u1005",
  "u2031",
  "u0019"
]
```

**内部保障原理**: 
1. **原子随机/公平防止超发**：采用 Redis 的 `SPOP` 原子指令执行随机抽取，弹出即保证不会被别人抽走。
2. **幂等抽奖**：抽中后会通过 `Set` 的 `SADD` 校验此用户在此前任何轮次中是否已经中过奖（控制一个人只能中一次）。
3. **两阶段防超/持久化**：先拿到 Redis 中奖名额，再构建 `LotteryHistory` 持久化写入数据库。

---

### 1.3 获取最新一轮中奖名单 (Latest Winners)
主动抓取本轮次最新产生的中奖名单（大屏如果不使用 WebSocket 连接时使用的短轮询降级接口）。

**URL**: `/api/lottery/{activityId}/winners/latest`
**Method**: `GET`

**Request Parameters**:
- `round` (Int): 要查询的对应轮次

**返回 (List<String>)**:
```json
[
  "u1005",
  "u2031"
]
```
---

### 1.4 中奖历史记录分页查询 (History)
历史记录保存在 MySQL 中供管理后台审核。

**URL**: `/api/lottery/{activityId}/history`
**Method**: `GET`

**Request Parameters**:
- `page` (Int): 页码（从`0`开始，默认 0）
- `size` (Int): 页面容量（默认 10）

**返回 (Page)**:
```json
{
  "content": [
    {
      "id": 1,
      "activityId": "annual-2026",
      "userId": "u1005",
      "round": 1,
      "wonAt": "2026-05-17T10:00:00Z"
    }
  ],
  "pageable": { ... },
  "totalElements": 1
}
```

---

## 2. WebSocket 实时推送接口

抽奖服务底层整合了 Spring WebSocket 和 STOMP 协议。大屏客户端只需要在一开始建立 STOMP 长连，并订阅指定频道的 Topic，即可在 `/draw` 被调用时**瞬时（<50ms）接收** 到中奖名单，确保百屏同步无错乱。

**订阅 Endpoint (STOMP)**: `ws://{host}:8080/ws`
**广播频道 (Topic)**: `/topic/lottery/winners/{activityId}`

### 2.1 推送数据格式定义 (JSON Array)
每次触发抽奖完成持久化落库后，实时推送到客户端的数据体为一个标准的 JSON 字符串数组，内容为被抽中的独立 `userId`。

**数据样例**:
```json
[
  "u1005",
  "u2031",
  "u0019"
]
```

### 2.2 给前端开发者 (角色 D) 的对接样例代码

**依赖安装**:
```bash
npm install sockjs-client @stomp/stompjs
```

**Vue3 组合式 API (Composition API) 对接示例**:
```javascript
<template>
  <div class="lottery-board">
    <h2>实时大屏中奖名单</h2>
    <ul>
      <li v-for="winner in currentWinners" :key="winner">{{ winner }}</li>
    </ul>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import SockJS from 'sockjs-client/dist/sockjs';
import { Stomp } from '@stomp/stompjs';

const currentWinners = ref([]);
let stompClient = null;
const activityId = 'annual-2026';

onMounted(() => {
  // 建立连接
  const socket = new SockJS('http://localhost:8080/ws');
  stompClient = Stomp.over(socket);
  // 取消调试日志输出
  stompClient.debug = () => {}; 

  stompClient.connect({}, (frame) => {
    console.log('大屏推送已连接:', frame);
    
    // 订阅当前活动的抽奖结果
    stompClient.subscribe(`/topic/lottery/winners/${activityId}`, (message) => {
      if (message.body) {
         // 解析推送过来的 JSON 数组格式
         const newWinners = JSON.parse(message.body);
         console.log("接收到实时新增中奖者: ", newWinners);
         
         // 更新前端展示
         currentWinners.value = [...currentWinners.value, ...newWinners];
      }
    });
  });
});

onUnmounted(() => {
  if (stompClient) {
    stompClient.disconnect();
  }
});
## 3. 测试及联调指南 (供前端快速验证)

在开发大屏页面前，您可以直接在终端通过 `curl` 模拟添加用户及触发抽奖，以验证接口流转和 WebSocket 数据推送。

**第一步：注入测试数据 (加入奖池)**
```bash
curl -X POST "http://localhost:8080/api/lottery/annual-2026/join?userId=UserA"
curl -X POST "http://localhost:8080/api/lottery/annual-2026/join?userId=UserB"
curl -X POST "http://localhost:8080/api/lottery/annual-2026/join?userId=UserC"
```

**第二步：触发抽奖并观测 WebSocket**
*(在此之前，请先用代码或 WebSocket 测试客户端连上 `ws://localhost:8080/ws` 并订阅 `/topic/lottery/winners/annual-2026`)*
```bash
curl -X POST "http://localhost:8080/api/lottery/annual-2026/draw?round=1&count=2"
# 此时 WebSocket 将会收到如下类似 payload:
# ["UserC", "UserA"]
```

**第三步：历史翻查校验**
```bash
curl -X GET "http://localhost:8080/api/lottery/annual-2026/history"
```