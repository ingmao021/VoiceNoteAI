import Fastify, { type FastifyServerOptions } from 'fastify';
import swagger from '@fastify/swagger';
import { TaskProviderAccessRevokedError } from '../../domain/errors/task-provider-errors.js';
import { mapError } from './error-mapper.js';
import { healthRoutes } from './routes/health.routes.js';
import { notesRoutes, type NotesRoutesOptions } from './routes/notes.routes.js';

export type AppDependencies = NotesRoutesOptions;

/**
 * Construye la app con sus dependencias inyectadas, para que las pruebas usen falsos con
 * `app.inject()` sin tocar Gemini ni Google.
 */
export async function buildApp(deps: AppDependencies, options: FastifyServerOptions = {}) {
  const app = Fastify({
    // Fastify no registra cuerpos de petición por defecto: el texto dictado no llega al log (RN-07).
    logger: { level: 'info' },
    ...options,
  });

  // El contrato con el reloj (PRD §2.3): los modelos de Kotlin se escriben contra este OpenAPI.
  await app.register(swagger, {
    openapi: {
      info: { title: 'VoiceNoteAI', version: '0.3.0' },
      components: { securitySchemes: { bearer: { type: 'http', scheme: 'bearer' } } },
    },
  });
  app.get('/openapi.json', { schema: { hide: true } }, async () => app.swagger());

  app.setErrorHandler((error, req, reply) => {
    const http = mapError(error);
    if (http.status >= 500) {
      req.log.error({ err: error }, 'Request failed');
    } else if (error instanceof TaskProviderAccessRevokedError) {
      req.log.warn('Google access revoked');
    }
    return reply.code(http.status).send({ error: http.code, message: http.message });
  });

  app.setNotFoundHandler((req, reply) =>
    reply.code(404).send({ error: 'not_found', message: `Route ${req.method} ${req.url} not found` }),
  );

  await app.register(healthRoutes);
  await app.register(notesRoutes, deps);
  return app;
}
