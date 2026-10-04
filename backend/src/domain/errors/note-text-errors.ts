import { DomainError } from './domain-error.js';

/** RN-02: no se crea una entrada vacía. */
export class EmptyNoteTextError extends DomainError {
  constructor() {
    super('Note text is empty');
  }
}

export class NoteTextTooLongError extends DomainError {
  constructor(readonly maxLength: number) {
    super(`Note text exceeds ${maxLength} characters`);
  }
}
