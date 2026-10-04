import { afterEach, describe, expect, it } from 'vitest';
import type { FastifyInstance } from 'fastify';
import type { NoteStructurer } from '../../../src/application/ports/note-structurer.js';
import { CreateNoteUseCase } from '../../../src/application/use-cases/create-note.use-case.js';
import { TaskProviderAccessRevokedError, TaskProviderError } from '../../../src/domain/errors/task-provider-errors.js';
import { RawNoteStructurer } from '../../../src/infrastructure/ai/raw-note-structurer.js';
import { buildApp } from '../../../src/presentation/http/app.js';
import { FakeTaskGateway, fixedStructurer } from '../../support/fakes.js';

const TOKEN = 'test-token';
const authorization = { authorization: `Bearer ${TOKEN}` };

let app: FastifyInstance;
let tasks: FakeTaskGateway;

async function start(structurer: NoteStructurer = new RawNoteStructurer()) {
  tasks = new FakeTaskGateway();
  app = await buildApp({ devApiToken: TOKEN, createNote: new CreateNoteUseCase(structurer, tasks) }, { logger: false });
  return app;
}

const post = (body: unknown, headers: Record<string, string> = authorization) =>
  app.inject({ method: 'POST', url: '/v1/notes', headers, payload: body as object });

afterEach(() => app?.close());

describe('POST /v1/notes', () => {
  it('returns 201 with the created task', async () => {
    const note = { title: 'Comprar café', notes: 'En la tarde', subtasks: [], dueDate: '2026-10-04' };
    await start(fixedStructurer(note));

    const r = await post({ text: 'comprar café mañana en la tarde' });

    expect(r.statusCode).toBe(201);
    expect(r.json()).toEqual({ id: 'task-1', title: 'Comprar café', structured: true, dueDate: '2026-10-04' });
    expect(tasks.created).toEqual([note]);
  });

  it('reports unstructured notes (FA-11)', async () => {
    await start();
    const r = await post({ text: 'idea suelta' });
    expect(r.statusCode).toBe(201);
    expect(r.json()).toMatchObject({ title: 'Idea suelta', structured: false, dueDate: null });
  });

  it('returns 422 for empty text without creating anything (RN-02)', async () => {
    await start();
    const r = await post({ text: '   ' });
    expect(r.statusCode).toBe(422);
    expect(r.json().error).toBe('text_empty');
    expect(tasks.created).toHaveLength(0);
  });

  it('returns 422 for text over the limit', async () => {
    await start();
    const r = await post({ text: 'x'.repeat(5001) });
    expect(r.statusCode).toBe(422);
    expect(r.json().error).toBe('text_too_long');
  });

  it('returns 401 with WWW-Authenticate without a valid token', async () => {
    await start();
    expect((await post({ text: 'hola' }, {})).statusCode).toBe(401);
    const r = await post({ text: 'hola' }, { authorization: 'Bearer other' });
    expect(r.statusCode).toBe(401);
    expect(r.headers['www-authenticate']).toBe('Bearer');
    expect(r.json().error).toBe('invalid_token');
    expect(tasks.created).toHaveLength(0);
  });

  it('returns 424 when Google access was revoked (FA-13)', async () => {
    await start();
    tasks.failure = new TaskProviderAccessRevokedError(new Error('invalid_grant'));
    const r = await post({ text: 'hola' });
    expect(r.statusCode).toBe(424);
    expect(r.json().error).toBe('google_access_revoked');
  });

  it('returns 502 when Google Tasks fails (FA-16)', async () => {
    await start();
    tasks.failure = new TaskProviderError(new Error('500 from Google'));
    const r = await post({ text: 'hola' });
    expect(r.statusCode).toBe(502);
    expect(r.json().error).toBe('tasks_provider_error');
  });

  it('returns 500 for unexpected errors', async () => {
    await start();
    tasks.failure = new Error('boom');
    const r = await post({ text: 'hola' });
    expect(r.statusCode).toBe(500);
    expect(r.json()).toEqual({ error: 'internal_error', message: 'Internal server error' });
  });

  it('returns 400 for a malformed body', async () => {
    await start();
    expect((await post({})).statusCode).toBe(400);
    expect((await post({ text: { nested: true } })).json().error).toBe('invalid_request');
    const r = await app.inject({
      method: 'POST',
      url: '/v1/notes',
      headers: { ...authorization, 'content-type': 'application/json' },
      payload: '{not json',
    });
    expect(r.statusCode).toBe(400);
  });
});

describe('GET /v1/health', () => {
  it('responds without authentication', async () => {
    await start();
    const r = await app.inject({ method: 'GET', url: '/v1/health' });
    expect(r.statusCode).toBe(200);
    expect(r.json()).toEqual({ status: 'ok' });
  });
});

describe('unknown routes', () => {
  it('return 404 with an error body', async () => {
    await start();
    const r = await app.inject({ method: 'GET', url: '/v1/notas' });
    expect(r.statusCode).toBe(404);
    expect(r.json().error).toBe('not_found');
  });
});

describe('OpenAPI', () => {
  it('publishes the /v1/notes contract with its status codes', async () => {
    await start();
    const r = await app.inject({ method: 'GET', url: '/openapi.json' });
    const responses = r.json().paths['/v1/notes'].post.responses;
    expect(Object.keys(responses)).toEqual(expect.arrayContaining(['201', '400', '401', '422', '424', '502']));
  });
});
