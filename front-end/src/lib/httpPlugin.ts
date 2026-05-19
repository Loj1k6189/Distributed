import type { App } from 'vue'
import { http } from './http'

/**
 * Vue 插件，用于在 Vue 应用中注册全局 `$http` 对象
 * 这样就可以在组件中通过 `this.$http` 或者通过全局属性获取到封装后的 http 客户端
 * 示例：在组件内部使用 `this.$http.post('/api/..', payload, { idempotencyKey })`
 */
export default {
  install(app: App) {
    // 将封装好的 http 实例挂载到全局属性，兼容选项式 API 的 `this.$http` 使用方式
    app.config.globalProperties.$http = http
  }
}
