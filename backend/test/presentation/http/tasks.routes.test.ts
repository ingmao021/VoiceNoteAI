import { afterEach, describe, expect, it } from 'vitest';
import type { FastifyInstance } from 'fastify';
import { CreateNoteUseCase } from '../../../src/application/use-cases/create-note.use-case.js';
import { DeleteTaskUseCase } from '../../../src/application/use-cases/delete-task.use-case.js';
import { ListRecentTasksUseCase } from '../../../src/application/use-cases/list-recent-tasks.use-case.js';
import { SetTaskCompletedUseCase } from '../../../src/application/use-cases/set-task-completed.use-case.js';
import type { TaskSummary } from '../../../src/domain/task/task-summary.js';
import { RawNoteStructurer } from '../../../src/infrastructure/ai/raw-note-structurer.js';
import {
  TaskNotFoundError,
  TaskProviderAccessRevokedError,
  TaskProviderError,
} from '../../../src/domain/errors/task-provider-errors.js';
import { buildApp } from '../../../src/presentation/http/app.js';
import { FakeTaskGateway } from '../../support/fakes.js';

const TOKEN = 'test-token';
const authorization = { authorization: `Bearer ${TOKEN}` };
const NOW = new Date('2026-11-07T15:00:00.000Z');

const tasks: TaskSummary[] = [
  { id: 'future', title: 'Comprar café', dueDate: '2026-11-08', completed: false },
  { id: 'today', title: 'Llamar a Juan', dueDate: '2026-11-07', completed: false },
  { id: 'expired', title: 'Pagar el recibo', dueDate: '2026-11-06', completed: false },
  { id: 'expired-done', title: 'Enviar correo', dueDate: '2026-11-06', completed: true },
  { id: 'no-date', title: 'Revisar el informe', dueDate: null, completed: true },
];

let app: FastifyInstance;
let gateway: FakeTaskGateway;

async function start() {
  gateway = new FakeTaskGateway();
  gateway.listed = tasks;
  app = await buildApp(
    {
      devApiToken: TOKEN,
      // Las rutas de notas no se ejercitan aquí, pero el registro de la app las exige.
      createNote: new CreateNoteUseCase(new RawNoteStructurer(), gateway),
      listTasks: new ListRecentTasksUseCase(gateway, 'America/Bogota', () => NOW),
      setTaskCompleted: new SetTaskCompletedUseCase(gateway),
      deleteTask: new DeleteTaskUseCase(gateway),
    },
    { logger: false },
  );
  return app;
}

afterEach(() => app?.close());

const get = (query = '', headers: Record<string, string> = authorization) =>
  app.inject({ method: 'GET', url: `/v1/tasks${query}`, headers });

const patch = (id: string, body: unknown, headers: Record<string, string> = authorization) =>
  app.inject({ method: 'PATCH', url: `/v1/tasks/${id}`, headers, payload: body as object });

describe('GET /v1/tasks', () => {
  it('returns the day used for filtering and hides expired tasks, completed or not', async () => {
    await start();
    const r = await get();

    expect(r.statusCode).toBe(200);
    expect(r.json()).toEqual({
      today: '2026-11-07',
      tasks: [
        { id: 'future', title: 'Comprar café', dueDate: '2026-11-08', completed: false },
        { id: 'today', title: 'Llamar a Juan', dueDate: '2026-11-07', completed: false },
        { id: 'no-date', title: 'Revisar el informe', dueDate: null, completed: true },
      ],
    });
  });

  it('respects the limit', async () => {
    await start();
    const r = await get('?limit=1');
    expect(r.statusCode).toBe(200);
    expect(r.json().tasks).toHaveLength(1);
    expect(r.json().tasks[0].id).toBe('future');
  });

  it('returns 401 without a valid token', async () => {
    await start();
    const r = await get('', {});
    expect(r.statusCode).toBe(401);
    expect(r.headers['www-authenticate']).toBe('Bearer');
  });

  it('returns 424 when Google access was revoked (FA-13)', async () => {
    await start();
    gateway.listFailure = new TaskProviderAccessRevokedError(new Error('invalid_grant'));
    const r = await get();
    expect(r.statusCode).toBe(424);
    expect(r.json().error).toBe('google_access_revoked');
  });

  it('returns 502 when Google Tasks fails (FA-16)', async () => {
    await start();
    gateway.listFailure = new TaskProviderError(new Error('500 from Google'));
    const r = await get();
    expect(r.statusCode).toBe(502);
    expect(r.json().error).toBe('tasks_provider_error');
  });

  it('rejects an out-of-range limit', async () => {
    await start();
    expect((await get('?limit=0')).statusCode).toBe(400);
    expect((await get('?limit=51')).statusCode).toBe(400);
  });
});

describe('PATCH /v1/tasks/:id', () => {
  it('marks a task as completed', async () => {
    await start();
    const r = await patch('task-1', { completed: true });
    expect(r.statusCode).toBe(200);
    expect(r.json()).toEqual({ id: 'task-1', completed: true });
    expect(gateway.completedCalls).toEqual([{ id: 'task-1', completed: true }]);
  });

  it('marks a task as pending', async () => {
    await start();
    const r = await patch('task-1', { completed: false });
    expect(r.statusCode).toBe(200);
    expect(gateway.completedCalls).toEqual([{ id: 'task-1', completed: false }]);
  });

  it('returns 404 when the task no longer exists', async () => {
    await start();
    gateway.completeFailure = new TaskNotFoundError(new Error('404'));
    const r = await patch('gone', { completed: true });
    expect(r.statusCode).toBe(404);
    expect(r.json().error).toBe('task_not_found');
  });

  it('returns 400 for a malformed body', async () => {
    await start();
    expect((await patch('task-1', {})).statusCode).toBe(400);
    expect((await patch('task-1', { completed: 'yes' })).statusCode).toBe(400);
  });

  it('returns 401 without a valid token', async () => {
    await start();
    expect((await patch('task-1', { completed: true }, {})).statusCode).toBe(401);
    expect(gateway.completedCalls).toHaveLength(0);
  });

  it('returns 424 and 502 from the provider', async () => {
    await start();
    gateway.completeFailure = new TaskProviderAccessRevokedError(new Error('invalid_grant'));
    expect((await patch('task-1', { completed: true })).json().error).toBe('google_access_revoked');

    gateway.completeFailure = new TaskProviderError(new Error('500'));
    expect((await patch('task-1', { completed: true })).json().error).toBe('tasks_provider_error');
  });
});

describe('DELETE /v1/tasks/:id', () => {
  const del = (id: string, headers: Record<string, string> = authorization) =>
    app.inject({ method: 'DELETE', url: `/v1/tasks/${id}`, headers });

  it('deletes a task', async () => {
    await start();
    const r = await del('task-1');
    expect(r.statusCode).toBe(200);
    expect(r.json()).toEqual({ id: 'task-1' });
    expect(gateway.deletedIds).toEqual(['task-1']);
  });

  it('returns 404 when the task no longer exists', async () => {
    await start();
    gateway.deleteFailure = new TaskNotFoundError(new Error('404'));
    const r = await del('gone');
    expect(r.statusCode).toBe(404);
    expect(r.json().error).toBe('task_not_found');
  });

  it('returns 401 without a valid token', async () => {
    await start();
    expect((await del('task-1', {})).statusCode).toBe(401);
    expect(gateway.deletedIds).toHaveLength(0);
  });

  it('returns 424 and 502 from the provider', async () => {
    await start();
    gateway.deleteFailure = new TaskProviderAccessRevokedError(new Error('invalid_grant'));
    expect((await del('task-1')).json().error).toBe('google_access_revoked');

    gateway.deleteFailure = new TaskProviderError(new Error('500'));
    expect((await del('task-1')).json().error).toBe('tasks_provider_error');
  });
});

describe('OpenAPI', () => {
  it('publishes the tasks contract with its status codes', async () => {
    await start();
    const r = await app.inject({ method: 'GET', url: '/openapi.json' });
    const paths = r.json().paths;
    expect(Object.keys(paths['/v1/tasks'].get.responses)).toEqual(
      expect.arrayContaining(['200', '401', '424', '502']),
    );
    expect(Object.keys(paths['/v1/tasks/{id}'].patch.responses)).toEqual(
      expect.arrayContaining(['200', '400', '401', '404', '424', '502']),
    );
  });
});
