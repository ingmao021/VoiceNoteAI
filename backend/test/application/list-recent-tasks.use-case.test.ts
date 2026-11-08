import { describe, expect, it } from 'vitest';
import { ListRecentTasksUseCase } from '../../src/application/use-cases/list-recent-tasks.use-case.js';
import type { TaskSummary } from '../../src/domain/task/task-summary.js';
import { FakeTaskGateway } from '../support/fakes.js';
import { silentLogger } from '../support/silent-logger.js';

const task = (overrides: Partial<TaskSummary>): TaskSummary => ({
  id: 'id',
  title: 'Tarea',
  dueDate: null,
  completed: false,
  ...overrides,
});

describe('ListRecentTasksUseCase', () => {
  it('hides tasks due before today in the configured zone, completed or not', async () => {
    const gateway = new FakeTaskGateway();
    gateway.listed = [
      task({ id: 'past', dueDate: '2026-11-06' }),
      task({ id: 'past-done', dueDate: '2026-11-06', completed: true }),
      task({ id: 'today', dueDate: '2026-11-07' }),
      task({ id: 'future', dueDate: '2026-11-08' }),
      task({ id: 'none' }),
    ];
    // En Bogotá (UTC-5) todavía es 7 de noviembre aunque en UTC ya sea el 8.
    const useCase = new ListRecentTasksUseCase(gateway, 'America/Bogota', () => new Date('2026-11-08T03:00:00.000Z'));

    const result = await useCase.execute(20, silentLogger);

    expect(result.today).toBe('2026-11-07');
    expect(result.tasks.map((t) => t.id)).toEqual(['today', 'future', 'none']);
  });

  it('caps the result to the requested limit', async () => {
    const gateway = new FakeTaskGateway();
    gateway.listed = Array.from({ length: 10 }, (_, i) => task({ id: `t${i}` }));
    const useCase = new ListRecentTasksUseCase(gateway, 'America/Bogota', () => new Date('2026-11-07T15:00:00.000Z'));

    const result = await useCase.execute(3, silentLogger);

    expect(result.tasks.map((t) => t.id)).toEqual(['t0', 't1', 't2']);
  });
});
