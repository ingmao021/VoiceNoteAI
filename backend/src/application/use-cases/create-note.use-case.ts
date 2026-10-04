import { NoteText } from '../../domain/note/note-text.js';
import { withNotesFallback, type StructuredNote } from '../../domain/note/structured-note.js';
import type { Logger } from '../ports/logger.js';
import type { NoteStructurer } from '../ports/note-structurer.js';
import type { TaskGateway } from '../ports/task-gateway.js';

export interface CreatedNote {
  id: string;
  note: StructuredNote;
  structured: boolean;
}

/** Dictado → nota estructurada → tarea en el proveedor. */
export class CreateNoteUseCase {
  constructor(
    private readonly structurer: NoteStructurer,
    private readonly tasks: TaskGateway,
  ) {}

  async execute(rawText: string, logger: Logger): Promise<CreatedNote> {
    const text = NoteText.create(rawText);

    const result = await this.structurer.structure(text.value, logger);
    const note = withNotesFallback(result.note, text.value);
    const id = await this.tasks.create(note, logger);

    // El texto nunca se registra (RN-07, RNF-18): solo su longitud.
    logger.info(
      {
        characters: text.length,
        subtasks: note.subtasks.length,
        hasDueDate: note.dueDate !== null,
        structured: result.structured,
      },
      'Note created',
    );
    return { id, note, structured: result.structured };
  }
}
