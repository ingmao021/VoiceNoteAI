import { DomainError } from '../../domain/errors/domain-error.js';
import { EmptyNoteTextError, NoteTextTooLongError } from '../../domain/errors/note-text-errors.js';
import { TaskProviderAccessRevokedError, TaskProviderError } from '../../domain/errors/task-provider-errors.js';

export interface HttpError {
  status: number;
  code: string;
  message: string;
}

type DomainErrorClass = abstract new (...args: never[]) => DomainError;

/** Único sitio donde un error de negocio se convierte en código HTTP. */
const DOMAIN_ERRORS: ReadonlyArray<readonly [DomainErrorClass, number, string]> = [
  [EmptyNoteTextError, 422, 'text_empty'],
  [NoteTextTooLongError, 422, 'text_too_long'],
  // El token de la app es válido: falló una dependencia (Google), no la autenticación (FA-13).
  [TaskProviderAccessRevokedError, 424, 'google_access_revoked'],
  [TaskProviderError, 502, 'tasks_provider_error'],
];

const CLIENT_ERROR_CODES: Readonly<Record<number, string>> = {
  400: 'invalid_request',
  401: 'invalid_token',
  404: 'not_found',
  413: 'payload_too_large',
  415: 'unsupported_media_type',
};

export const INTERNAL_ERROR: HttpError = { status: 500, code: 'internal_error', message: 'Internal server error' };

export function mapError(error: unknown): HttpError {
  if (error instanceof DomainError) {
    const match = DOMAIN_ERRORS.find(([type]) => error instanceof type);
    if (match) return { status: match[1], code: match[2], message: error.message };
  }
  // Errores propios de Fastify (validación, JSON mal formado, tipo de contenido…).
  const status = (error as { statusCode?: unknown })?.statusCode;
  if (typeof status === 'number' && status >= 400 && status < 500) {
    return {
      status,
      code: CLIENT_ERROR_CODES[status] ?? 'invalid_request',
      message: error instanceof Error ? error.message : 'Invalid request',
    };
  }
  return INTERNAL_ERROR;
}
