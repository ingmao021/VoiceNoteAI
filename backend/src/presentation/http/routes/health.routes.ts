import type { FastifyInstance } from 'fastify';

export async function healthRoutes(app: FastifyInstance) {
  app.get(
    '/v1/health',
    {
      schema: {
        summary: 'Warm-up ping (PRD §3.5)',
        response: {
          200: { type: 'object', properties: { status: { type: 'string' } }, required: ['status'] },
        },
      },
    },
    async () => ({ status: 'ok' }),
  );
}
