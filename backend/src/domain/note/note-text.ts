import { EmptyNoteTextError, NoteTextTooLongError } from '../errors/note-text-errors.js';

/** Máximo de caracteres aceptado: 60 s de dictado son ~150 palabras (PRD §4.3); esto deja holgura. */
export const MAX_TEXT_LENGTH = 5000;

/** Objeto de valor: el dictado ya validado. Nunca se crea vacío (RN-02). */
export class NoteText {
  private constructor(readonly value: string) {}

  static create(raw: string): NoteText {
    const value = raw.trim();
    if (!value) throw new EmptyNoteTextError();
    if (value.length > MAX_TEXT_LENGTH) throw new NoteTextTooLongError(MAX_TEXT_LENGTH);
    return new NoteText(value);
  }

  get length(): number {
    return this.value.length;
  }
}
