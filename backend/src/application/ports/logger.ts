/** Lo mínimo que la aplicación necesita para registrar; el logger de Fastify (pino) lo cumple. */
export interface Logger {
  info(obj: object, msg: string): void;
  warn(obj: object, msg: string): void;
  error(obj: object, msg: string): void;
}
