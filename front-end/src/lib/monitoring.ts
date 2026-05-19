// 简单的前端监控助手（可扩展为 Sentry / RUM 集成）

export function reportError(err: any, context?: Record<string, any>) {
  try {
    // TODO: 替换为 Sentry.captureException 或自定义上报接口
    // Sentry 示例（已注释）：
    // import * as Sentry from '@sentry/browser'
    // Sentry.captureException(err)

    // 临时：打印到控制台并发送到后端埋点（如果有）
    console.error('[monitor] error', err, context)
  } catch (e) {
    // ignore
  }
}

export function reportPerf(name: string, metrics: Record<string, any>) {
  try {
    console.log('[monitor] perf', name, metrics)
    // TODO: 聚合并上报到 APM
  } catch (e) {}
}
