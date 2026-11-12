import type { NoteStructurer } from '../../src/application/ports/note-structurer.js';
import type { TaskGateway } from '../../src/application/ports/task-gateway.js';
import type { StructuredNote } from '../../src/domain/note/structured-note.js';
import type { TaskSummary } from '../../src/domain/task/task-summary.js';

export class FakeTaskGateway implements TaskGateway {
  created: StructuredNote[] = [];
  failure: Error | null = null;

  listed: TaskSummary[] = [];
  listFailure: Error | null = null;

  completedCalls: Array<{ id: string; completed: boolean }> = [];
  completeFailure: Error | null = null;

  async create(note: StructuredNote) {
    if (this.failure) throw this.failure;
    this.created.push(note);
    return `task-${this.created.length}`;
  }

  async list() {
    if (this.listFailure) throw this.listFailure;
    return this.listed;
  }

  async setCompleted(id: string, completed: boolean) {
    if (this.completeFailure) throw this.completeFailure;
    this.completedCalls.push({ id, completed });
  }

  deletedIds: string[] = [];
  deleteFailure: Error | null = null;

  async delete(id: string) {
    if (this.deleteFailure) throw this.deleteFailure;
    this.deletedIds.push(id);
  }
}

export const fixedStructurer = (note: StructuredNote): NoteStructurer => ({
  structure: async () => ({ note, structured: true }),
});
