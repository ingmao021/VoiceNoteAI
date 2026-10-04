import type { NoteStructurer, StructuringResult } from '../../application/ports/note-structurer.js';
import { createRawNote } from '../../domain/note/structured-note.js';

/** Objeto nulo: sin clave de Gemini, toda nota se guarda como texto crudo (FA-11). */
export class RawNoteStructurer implements NoteStructurer {
  async structure(text: string): Promise<StructuringResult> {
    return { note: createRawNote(text), structured: false };
  }
}
