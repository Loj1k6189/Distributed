import { createRouter, createWebHistory } from 'vue-router'
import CreatePoll from '../components/CreatePoll.vue'
import VoteSubmit from '../components/VoteSubmit.vue'
import VoteResult from '../components/VoteResult.vue'
import Admin from '../components/Admin.vue'

// 新增问卷相关组件
import QuestCreate from '../components/QuestCreate.vue'
import QuestList from '../components/QuestList.vue'
import QuestSubmit from '../components/QuestSubmit.vue'
import QuestStats from '../components/QuestStats.vue'

// 新增抽奖相关组件
import LotteryJoin from '../components/LotteryJoin.vue'
import LotteryDraw from '../components/LotteryDraw.vue'
import LotteryWinners from '../components/LotteryWinners.vue'
import LotteryHistory from '../components/LotteryHistory.vue'

const routes = [
  { path: '/', redirect: '/create' },

  // ===== 投票系统 =====
  { path: '/create', component: CreatePoll },
  { path: '/vote', component: VoteSubmit },
  { path: '/vote/:id', component: VoteSubmit },
  { path: '/result', component: VoteResult },
  { path: '/result/:id', component: VoteResult },
  { path: '/admin', component: Admin },
  
  // ===== 问卷系统 =====
  { path: '/quest/create', component: QuestCreate },
  { path: '/quest/list', component: QuestList },
  { path: '/quest/submit/:id', component: QuestSubmit },
  { path: '/quest/stats/:id', component: QuestStats },
  
  // ===== 抽奖系统 =====
  { path: '/lottery/join/:activityId', component: LotteryJoin },
  { path: '/lottery/draw', component: LotteryDraw },
  { path: '/lottery/winners/:activityId', component: LotteryWinners },
  { path: '/lottery/history/:activityId', component: LotteryHistory },
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router