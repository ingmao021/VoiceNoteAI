import { describe, expect, it } from 'vitest';
import { CreateNoteUseCase } from '../../src/application/use-cases/create-note.use-case.js';
import { EmptyNoteTextError } from '../../src/domain/errors/note-text-errors.js';
import { RawNoteStructurer } from '../../src/infrastructure/ai/raw-note-structurer.js';
import { FakeTaskGateway, fixedStructurer } from '../support/fakes.js';
import { silentLogger } from '../support/silent-logger.js';

describe('CreateNoteUseCase', () => {
  it('structures the text and creates the task', async () => {
    const note = { title: 'Compras', notes: 'Comprar leche y pan.', subtasks: ['Leche', 'Pan'], dueDate: null };
    const tasks = new FakeTaskGateway();

    const result = await new CreateNoteUseCase(fixedStructurer(note), tasks).execute('eh comprar leche y pan', silentLogger);

    expect(result).toEqual({ id: 'task-1', note, structured: true });
    expect(tasks.created).toEqual([note]);
  });

  it('saves the dictation when the structurer leaves the notes empty', async () => {
    const tasks = new FakeTaskGateway();
    const structurer = fixedStructurer({ title: 'Revisar el informe', notes: '', subtasks: [], dueDate: '2026-10-05' });

    await new CreateNoteUseCase(structurer, tasks).execute('la próxima semana   revisar el informe', silentLogger);

    expect(tasks.created[0]?.notes).toBe('la próxima semana revisar el informe');
  });

  it('creates nothing for empty text (RN-02)', async () => {
    const tasks = new FakeTaskGateway();
    await expect(new CreateNoteUseCase(new RawNoteStructurer(), tasks).execute('   ', silentLogger)).rejects.toThrow(
      EmptyNoteTextError,
    );
    expect(tasks.created).toHaveLength(0);
  });
});
