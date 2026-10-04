/** Base de los errores de negocio: la capa HTTP los traduce a códigos de estado en un solo sitio. */
export abstract class DomainError extends Error {
  protected constructor(message: string, options?: ErrorOptions) {
    super(message, options);
    this.name = new.target.name;
  }
}
