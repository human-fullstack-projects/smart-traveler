import { defineConfig, loadEnv } from 'vite';
import { readFileSync, existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import react from '@vitejs/plugin-react';

// 개발 중에도 같은 출처로 요청하여 세션 쿠키와 CSRF 검증을 사용한다.
const frontendDir = fileURLToPath(new URL('.', import.meta.url));
const kakaoFile = fileURLToPath(new URL('../secrets/kakao.json', import.meta.url));

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, frontendDir, 'VITE_');
  const config = existsSync(kakaoFile) ? JSON.parse(readFileSync(kakaoFile, 'utf8').replace(/^\uFEFF/, '')) : {};
  // REST API 키와 client_secret은 브라우저로 전달하지 않는다.
  const mapKey = env.VITE_KAKAO_MAP_APP_KEY || config.javascript_key || config.javascriptKey || '';
  return {
  define: { 'import.meta.env.VITE_KAKAO_MAP_APP_KEY': JSON.stringify(mapKey) },
  plugins: [react(), {
    name: 'local-kakao-map-key',
    configureServer(server) {
      server.watcher.add(kakaoFile);
      const reload = path => { if (path === kakaoFile) void server.restart(); };
      server.watcher.on('change', reload);
      server.watcher.on('add', reload);
    },
  }],
  server: {
    host:'127.0.0.1',
    proxy: {
      // Browser cookies stay on the React origin; backend port can be overridden locally.
      '/api': { target: process.env.SMART_TRAVEL_API_TARGET || 'http://127.0.0.1:8080', changeOrigin: true },
    },
  },
  };
});
