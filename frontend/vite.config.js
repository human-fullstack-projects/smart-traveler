import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // Browser cookies stay on the React origin; backend port can be overridden locally.
      '/api': { target: process.env.SMART_TRAVEL_API_TARGET || 'http://127.0.0.1:8080', changeOrigin: true },
    },
  },
});
