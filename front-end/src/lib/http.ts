import axios from 'axios'
import type { AxiosRequestConfig } from 'axios'

type RequestOptions = AxiosRequestConfig & {
  idempotencyKey?: string
  retries?: number
}

const DEFAULT_CONCURRENCY = 6
const DEFAULT_RETRIES = 2

class HttpClient {
  private queue: Array<() => void> = []
  private active = 0
  private concurrency: number
  private instance = axios.create({ baseURL: (import.meta as any).env?.VITE_API_BASE ?? '' })

  constructor(concurrency = DEFAULT_CONCURRENCY) {
    this.concurrency = concurrency
  }

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

  async request<T = any>(config: RequestOptions): Promise<T> {
    const retries = config.retries ?? DEFAULT_RETRIES
    // attach idempotency header if provided
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

  get<T = any>(url: string, config?: RequestOptions) {
    return this.request<T>({ ...(config || {}), method: 'get', url })
  }

  post<T = any>(url: string, data?: any, config?: RequestOptions) {
    return this.request<T>({ ...(config || {}), method: 'post', url, data })
  }
}

const http = new HttpClient()

export { http }
export type { RequestOptions }
