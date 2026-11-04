import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// 개발 중에도 같은 출처로 요청하여 세션 쿠키와 CSRF 검증을 사용한다.
export default defineConfig({ plugins: [react()], server: { proxy: { '/api': 'http://localhost:8080' } } });
