import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    // La primera construcción de la app (Fastify + Ajv) puede tardar varios segundos en frío en Windows.
    testTimeout: 20_000,
  },
});
