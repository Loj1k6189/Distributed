<template>
  <div class="app-shell">
    <header class="topbar">
      <div class="brand">
        <div class="brand-mark" />
        <div>
          <h1>现场活动系统</h1>
          <p>投票 · 问卷 · 抽奖</p>
        </div>
      </div>
      <nav class="primary-nav">
        <router-link
          v-for="module in modules"
          :key="module.id"
          :to="moduleTarget(module)"
          :class="{ active: isModuleActive(module) }"
        >
          {{ module.title }}
        </router-link>
      </nav>
    </header>

    <section class="page-tabs">
      <router-link
        v-for="item in activeModuleItems"
        :key="item.key"
        :to="item.path"
        :class="{ active: isItemActive(item) }"
      >
        {{ item.label }}
      </router-link>
    </section>

    <main class="page-main">
      <div class="page-card">
        <router-view v-slot="{ Component, route: currentRoute }">
          <Transition name="page-fade" mode="out-in">
            <component :is="Component" :key="currentRoute.fullPath" />
          </Transition>
        </router-view>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

type NavItem = {
  key: string
  label: string
  path: string
  matchPrefix: string[]
}

type NavModule = {
  id: string
  title: string
  defaultPath: string
  matchPrefix: string[]
  items: NavItem[]
}

const route = useRoute()

const MODULE_LAST_ROUTE_KEY = 'live-system:last-module-route'

const readLastRouteMap = (): Record<string, string> => {
  try {
    const raw = localStorage.getItem(MODULE_LAST_ROUTE_KEY)
    if (!raw) return {}
    const parsed = JSON.parse(raw) as Record<string, string>
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

const lastVisitedByModule = ref<Record<string, string>>(readLastRouteMap())

const modules: NavModule[] = [
  {
    id: 'vote',
    title: '投票',
    defaultPath: '/create',
    matchPrefix: ['/create', '/vote', '/result', '/admin'],
    items: [
      { key: 'vote-create', label: '创建投票', path: '/create', matchPrefix: ['/create'] },
      { key: 'vote-submit', label: '参与投票', path: '/vote', matchPrefix: ['/vote'] },
      { key: 'vote-result', label: '查看结果', path: '/result', matchPrefix: ['/result'] },
      { key: 'vote-admin', label: '投票管理', path: '/admin', matchPrefix: ['/admin'] }
    ]
  },
  {
    id: 'quest',
    title: '问卷',
    defaultPath: '/quest/list',
    matchPrefix: ['/quest'],
    items: [
      { key: 'quest-list', label: '问卷列表', path: '/quest/list', matchPrefix: ['/quest/list'] },
      { key: 'quest-create', label: '创建问卷', path: '/quest/create', matchPrefix: ['/quest/create'] },
      { key: 'quest-submit', label: '参与问卷', path: '/quest/submit/1', matchPrefix: ['/quest/submit'] },
      { key: 'quest-stats', label: '问卷统计', path: '/quest/stats/1', matchPrefix: ['/quest/stats'] }
    ]
  },
  {
    id: 'lottery',
    title: '抽奖',
    defaultPath: '/lottery/draw',
    matchPrefix: ['/lottery'],
    items: [
      { key: 'lottery-draw', label: '执行抽奖', path: '/lottery/draw', matchPrefix: ['/lottery/draw'] },
      { key: 'lottery-join', label: '参与抽奖', path: '/lottery/join/1', matchPrefix: ['/lottery/join'] },
      { key: 'lottery-winners', label: '查看中奖者', path: '/lottery/winners/1', matchPrefix: ['/lottery/winners'] },
      { key: 'lottery-history', label: '中奖历史', path: '/lottery/history/1', matchPrefix: ['/lottery/history'] }
    ]
  }
]

const activeModule = computed(() => {
  return modules.find((item) =>
    item.matchPrefix.some((prefix) => route.path.startsWith(prefix))
  ) ?? modules[0]
})

const activeModuleItems = computed(() => {
  return activeModule.value.items
})

const isModuleActive = (module: NavModule) => {
  return module.matchPrefix.some((prefix) => route.path.startsWith(prefix))
}

const isItemActive = (item: NavItem) => {
  return item.matchPrefix.some((prefix) => route.path.startsWith(prefix))
}

const moduleTarget = (module: NavModule) => {
  const lastPath = lastVisitedByModule.value[module.id]
  if (!lastPath) return module.defaultPath
  const isValid = module.matchPrefix.some((prefix) => lastPath.startsWith(prefix))
  return isValid ? lastPath : module.defaultPath
}

watch(
  () => route.fullPath,
  () => {
    const current = activeModule.value
    lastVisitedByModule.value = {
      ...lastVisitedByModule.value,
      [current.id]: route.fullPath
    }
    localStorage.setItem(MODULE_LAST_ROUTE_KEY, JSON.stringify(lastVisitedByModule.value))
  },
  { immediate: true }
)
</script>