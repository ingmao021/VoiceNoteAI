import type { TaskGateway } from '../ports/task-gateway.js';

/** Elimina una tarea desde el historial del reloj (deslizar a la izquierda). */
export class DeleteTaskUseCase {
  constructor(private readonly tasks: TaskGateway) {}

  async execute(id: string): Promise<void> {
    await this.tasks.delete(id);
  }
}
