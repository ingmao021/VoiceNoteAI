import type { StructuredNote } from '../../domain/note/structured-note.js';
import type { TaskSummary } from '../../domain/task/task-summary.js';
import type { Logger } from './logger.js';

export interface TaskGateway {
  /**
   * Crea una sola entrada (RN-08); las subtareas cuelgan de ella. Devuelve el id de la tarea.
   * Lanza [TaskProviderAccessRevokedError] o [TaskProviderError].
   */
  create(note: StructuredNote, logger: Logger): Promise<string>;

  /**
   * Lista tareas de nivel superior, incluidas las completadas. El orden es el de Google Tasks.
   * Lanza [TaskProviderAccessRevokedError] o [TaskProviderError].
   */
  list(maxResults: number): Promise<TaskSummary[]>;

  /**
   * Marca o desmarca una tarea. Lanza [TaskNotFoundError], [TaskProviderAccessRevokedError] o
   * [TaskProviderError].
   */
  setCompleted(id: string, completed: boolean): Promise<void>;

  /**
   * Elimina una tarea (y sus subtareas). Lanza [TaskNotFoundError], [TaskProviderAccessRevokedError]
   * o [TaskProviderError].
   */
  delete(id: string): Promise<void>;
}
