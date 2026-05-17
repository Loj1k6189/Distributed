// src/router/index.ts
import { createRouter, createWebHistory } from 'vue-router'
import CreatePoll from '../components/CreatePoll.vue'
import VoteSubmit from '../components/VoteSubmit.vue'
import VoteResult from '../components/VoteResult.vue'
import Admin from '../components/Admin.vue'

const routes = [
  { path: '/create', component: CreatePoll },
  { path: '/vote/:id', component: VoteSubmit },
  { path: '/result/:id', component: VoteResult },
  { path: '/admin', component: Admin } 
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router