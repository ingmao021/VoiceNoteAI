import { DomainError } from './domain-error.js';

/** El refresh token ya no sirve: el usuario revocó el acceso o caducó (FA-13). */
export class TaskProviderAccessRevokedError extends DomainError {
  constructor(cause: unknown) {
    super('Refresh token is invalid or access was revoked', { cause });
  }
}

/** El proveedor de tareas rechazó la petición (FA-16). */
export class TaskProviderError extends DomainError {
  constructor(cause: unknown) {
    super('Task provider request failed', { cause });
  }
}
