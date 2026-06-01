import axios from 'axios'
import type { AxiosRequestConfig } from 'axios'

/*
  http.ts
  - 项目内轻量级 HTTP 客户端封装，基于 axios
  - 特性：请求并发限制、请求队列、重试（指数退避）、幂等性头支持
  - 目标：在高并发场景下减少瞬时并发压力、提供可配置重试与幂等性支持

  使用示例：
    import { http } from './lib/http'
    await http.get('/api/votes/polls')
    await http.post('/api/votes/submit', payload, { idempotencyKey: 'uuid-v1' })

  说明：本封装返回的是后端响应的 `data` 字段（与 axios 默认返回值不同，方便直接使用）。
*/

type RequestOptions = AxiosRequestConfig & {
  // 幂等性键：用于对幂等写操作做去重/校验（需后端配合）
  idempotencyKey?: string
  // 覆盖默认重试次数（见 DEFAULT_RETRIES）
  retries?: number
}

// 默认并发数（浏览器并发限制和实际业务并发权衡得出一个合理值）
const DEFAULT_CONCURRENCY = 6
// 默认重试次数（仅对网络/临时可恢复错误有意义）
const DEFAULT_RETRIES = 2

class HttpClient {
  // 内部请求等待队列（当并发达到上限时，新的请求将被推入队列）
  private queue: Array<() => void> = []
  // 当前正在执行的请求数
  private active = 0
  // 最大并发限制
  private concurrency: number
  // axios 实例，使用 VITE_API_BASE 作为 baseURL（如果存在）
  private instance = axios.create({ baseURL: (import.meta as any).env?.VITE_API_BASE ?? '' })

  constructor(concurrency = DEFAULT_CONCURRENCY) {
    this.concurrency = concurrency
  }

  /*
    runQueue：并发控制器
    - 如果当前活跃请求数 >= 并发上限，则返回一个 Promise 并把 resolve 推入队列，等候被唤醒
    - 否则直接增加活跃计数并执行传入的异步函数
    - finally 中减少活跃计数并唤醒队列中的下一个请求
  */
  private async runQueue<T>(fn: () => Promise<T>): Promise<T> {
    if (this.active >= this.concurrency) {
      await new Promise<void>((resolve) => this.queue.push(resolve))
    }
    this.active++
    try {
      return await fn()
    } finally {
      this.active--
      const next = this.queue.shift()
      if (next) next()
    }
  }

  /*
    retryRequest：带指数退避的重试逻辑
    - fn：实际请求函数
    - retries：最大重试次数（不包含首次尝试）
    - 采用简单的指数退避：backoff = 2^attempt * 100ms
    - 注意：此处对所有异常都会重试，调用方若需区分可恢复/不可恢复错误需在外部处理
  */
  private async retryRequest<T>(fn: () => Promise<T>, retries: number): Promise<T> {
    let attempt = 0
    while (true) {
      try {
        return await fn()
      } catch (e) {
        attempt++
        if (attempt > retries) throw e
        const backoff = Math.pow(2, attempt) * 100
        await new Promise((r) => setTimeout(r, backoff))
      }
    }
  }

  /*
    request：统一请求方法
    - 支持 `idempotencyKey` 通过请求头 `Idempotency-Key` 发送给后端，用于幂等处理
    - 支持 `retries` 覆盖默认重试次数
    - 最终返回后端的 `data` 字段（便于调用处直接拿到业务数据）
    - 将实际请求交给 `runQueue`（并发控制）和 `retryRequest`（重试）协调执行
  */
  async request<T = any>(config: RequestOptions): Promise<T> {
    const retries = config.retries ?? DEFAULT_RETRIES
    // 复制并确保 headers 对象存在，然后注入幂等性头（若提供）
    const headers = { ...(config.headers || {}) }
    if (config.idempotencyKey) {
      headers['Idempotency-Key'] = config.idempotencyKey
    }
    const fn = async () => {
      const res = await this.instance.request<T>({ ...config, headers })
      return res.data
    }
    return this.runQueue(() => this.retryRequest(fn, retries))
  }

  // 便捷的 GET 方法签名（返回业务数据）
  get<T = any>(url: string, config?: RequestOptions) {
    return this.request<T>({ ...(config || {}), method: 'get', url })
  }

  // 便捷的 POST 方法签名（返回业务数据）
  post<T = any>(url: string, data?: any, config?: RequestOptions) {
    return this.request<T>({ ...(config || {}), method: 'post', url, data })
  }
}

// 导出默认客户端实例，项目中直接导入使用即可
const http = new HttpClient()

export { http }
export type { RequestOptions }
