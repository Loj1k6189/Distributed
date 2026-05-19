/**
 * debounce
 * - 简单的防抖工具函数，适用于输入防抖、搜索框等场景
 * - 每次调用都会重置计时器，只有在最后一次调用之后等待 `wait` 毫秒无新调用时才执行 `fn`
 * - 返回的函数会保留对原始参数的透传
 *
 * 使用示例：
 *   const debounced = debounce((value) => doSearch(value), 300)
 *   input.addEventListener('input', (e) => debounced((e.target as HTMLInputElement).value))
 */
export function debounce<T extends (...args: any[]) => any>(fn: T, wait = 200) {
  // 当前活跃的定时器引用，null 表示未设置
  let timer: ReturnType<typeof setTimeout> | null = null
  return function (...args: Parameters<T>) {
    // 如果之前已有计时器，清除它以重置等待时间
    if (timer) clearTimeout(timer)
    // 重新设置计时器，等待指定时间后调用原始函数
    timer = setTimeout(() => fn(...args), wait)
  }
}
