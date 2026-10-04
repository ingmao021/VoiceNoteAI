import type { StructuredNote } from '../../domain/note/structured-note.js';
import type { Logger } from './logger.js';

export interface StructuringResult {
  note: StructuredNote;
  /** `false` si la nota es el texto crudo porque no hubo LLM o falló (FA-11). */
  structured: boolean;
}

/** Estrategia que convierte un dictado en nota (Gemini, texto crudo…). */
export interface NoteStructurer {
  structure(text: string, logger: Logger): Promise<StructuringResult>;
}
