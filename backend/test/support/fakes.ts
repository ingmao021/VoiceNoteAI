import type { NoteStructurer } from '../../src/application/ports/note-structurer.js';
import type { TaskGateway } from '../../src/application/ports/task-gateway.js';
import type { StructuredNote } from '../../src/domain/note/structured-note.js';

export class FakeTaskGateway implements TaskGateway {
  created: StructuredNote[] = [];
  failure: Error | null = null;

  async create(note: StructuredNote) {
    if (this.failure) throw this.failure;
    this.created.push(note);
    return `task-${this.created.length}`;
  }
}

export const fixedStructurer = (note: StructuredNote): NoteStructurer => ({
  structure: async () => ({ note, structured: true }),
});
