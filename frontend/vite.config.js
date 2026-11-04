import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// 개발 중에도 같은 출처로 요청하여 세션 쿠키와 CSRF 검증을 사용한다.
export default defineConfig({ plugins: [react()], server: { proxy: { '/api': 'http://localhost:8080' } } });
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // Browser cookies stay on the React origin; backend port can be overridden locally.
      '/api': { target: process.env.SMART_TRAVEL_API_TARGET || 'http://127.0.0.1:8080', changeOrigin: true },
    },
  },
});
