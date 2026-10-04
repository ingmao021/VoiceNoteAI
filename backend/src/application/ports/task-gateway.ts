import type { StructuredNote } from '../../domain/note/structured-note.js';
import type { Logger } from './logger.js';

export interface TaskGateway {
  /**
   * Crea una sola entrada (RN-08); las subtareas cuelgan de ella. Devuelve el id de la tarea.
   * Lanza [TaskProviderAccessRevokedError] o [TaskProviderError].
   */
  create(note: StructuredNote, logger: Logger): Promise<string>;
}
