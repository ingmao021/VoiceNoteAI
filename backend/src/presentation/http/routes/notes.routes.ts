import type { FastifyInstance } from 'fastify';
import type { CreateNoteUseCase } from '../../../application/use-cases/create-note.use-case.js';
import { bearerAuth } from '../plugins/bearer-auth.js';
import { createNoteSchema, type CreateNoteBody } from '../schemas/notes.schemas.js';

export interface NotesRoutesOptions {
  devApiToken: string;
  createNote: CreateNoteUseCase;
}

/** Controlador fino: traduce HTTP ↔ caso de uso. Los errores los mapea el manejador global. */
export async function notesRoutes(app: FastifyInstance, { devApiToken, createNote }: NotesRoutesOptions) {
  app.post<{ Body: CreateNoteBody }>(
    '/v1/notes',
    { schema: createNoteSchema, onRequest: bearerAuth(devApiToken) },
    async (req, reply) => {
      const { id, note, structured } = await createNote.execute(req.body.text, req.log);
      return reply.code(201).send({ id, title: note.title, structured, dueDate: note.dueDate });
    },
  );
}
