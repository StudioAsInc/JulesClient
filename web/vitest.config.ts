import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    include: ['tests/unit/**/*.test.ts'],
    globals: true,
    setupFiles: [], // Add setup file if needed for jest-dom matchers, but simple tests might not need it yet
  },
});
