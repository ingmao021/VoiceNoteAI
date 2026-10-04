import type { Logger } from '../../application/ports/logger.js';
import type { NoteStructurer, StructuringResult } from '../../application/ports/note-structurer.js';

/**
 * Decorador: si la estrategia principal falla, usa la de respaldo.
 * FA-11: mejor la transcripción cruda que nada (RN-06).
 */
export class FallbackNoteStructurer implements NoteStructurer {
  constructor(
    private readonly primary: NoteStructurer,
    private readonly fallback: NoteStructurer,
  ) {}

  async structure(text: string, logger: Logger): Promise<StructuringResult> {
    try {
      return await this.primary.structure(text, logger);
    } catch (e) {
      logger.warn({ err: e instanceof Error ? e.message : String(e) }, 'LLM failed; saving unstructured note');
      return this.fallback.structure(text, logger);
    }
  }
}
