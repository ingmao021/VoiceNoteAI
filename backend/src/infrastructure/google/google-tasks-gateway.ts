import { auth, tasks, type tasks_v1 } from '@googleapis/tasks';
import type { Logger } from '../../application/ports/logger.js';
import type { TaskGateway } from '../../application/ports/task-gateway.js';
import type { StructuredNote } from '../../domain/note/structured-note.js';
import { TaskProviderAccessRevokedError, TaskProviderError } from '../../domain/errors/task-provider-errors.js';

export interface GoogleCredentials {
  clientId: string;
  clientSecret: string;
  refreshToken: string;
}

const DEFAULT_TASK_LIST = '@default';

/**
 * Adaptador de Google Tasks con un único refresh token. Andamio de la etapa 2 (PRD §12): en la
 * etapa 4 el token sale de la tabla de usuarios. La librería renueva el access token sola (FA-12).
 */
export class GoogleTasksGateway implements TaskGateway {
  private readonly api: tasks_v1.Tasks;

  constructor(credentials: GoogleCredentials) {
    const oauth = new auth.OAuth2(credentials.clientId, credentials.clientSecret);
    oauth.setCredentials({ refresh_token: credentials.refreshToken });
    this.api = tasks({ version: 'v1', auth: oauth });
  }

  async create(note: StructuredNote, logger: Logger): Promise<string> {
    const parentId = await this.insertParent(note);

    // La entrada ya existe: si una subtarea falla, reintentar duplicaría la nota (RN-08),
    // así que se registra y se sigue. `previous` mantiene el orden dictado.
    let previous: string | undefined;
    for (const [index, title] of note.subtasks.entries()) {
      try {
        const { data } = await this.api.tasks.insert({
          tasklist: DEFAULT_TASK_LIST,
          parent: parentId,
          previous,
          requestBody: { title },
        });
        previous = data.id ?? previous;
      } catch (e) {
        logger.warn({ err: errorMessage(e), subtask: index }, 'Could not create a subtask');
      }
    }
    return parentId;
  }

  private async insertParent(note: StructuredNote): Promise<string> {
    try {
      const { data } = await this.api.tasks.insert({
        tasklist: DEFAULT_TASK_LIST,
        requestBody: {
          title: note.title,
          notes: note.notes || undefined,
          // La Tasks API solo guarda el día y descarta la hora: medianoche UTC basta.
          due: note.dueDate ? `${note.dueDate}T00:00:00.000Z` : undefined,
        },
      });
      if (!data.id) throw new Error('Google Tasks returned no id');
      return data.id;
    } catch (e) {
      if (isAccessRevoked(e)) throw new TaskProviderAccessRevokedError(e);
      throw new TaskProviderError(e);
    }
  }
}

export function isAccessRevoked(e: unknown): boolean {
  const err = e as { response?: { data?: { error?: unknown } }; message?: unknown };
  return err?.response?.data?.error === 'invalid_grant' || String(err?.message ?? '').includes('invalid_grant');
}

function errorMessage(e: unknown): string {
  return e instanceof Error ? e.message : String(e);
}
