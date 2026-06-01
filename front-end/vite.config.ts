import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080', // 后端地址 (映射到宿主机端口)
        changeOrigin: true,
        ws: true
      }
    }
  }
})