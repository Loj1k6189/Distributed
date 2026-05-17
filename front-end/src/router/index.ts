// src/router/index.ts
import { createRouter, createWebHistory } from 'vue-router'
import CreatePoll from '../components/CreatePoll.vue'
import VoteSubmit from '../components/VoteSubmit.vue'
import VoteResult from '../components/VoteResult.vue'

const routes = [
  { path: '/create', component: CreatePoll },
  { path: '/vote/:id', component: VoteSubmit },
  { path: '/result/:id', component: VoteResult },
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router