import type { TaskGateway } from '../ports/task-gateway.js';

/** Marca o desmarca una tarea desde el historial del reloj. */
export class SetTaskCompletedUseCase {
  constructor(private readonly tasks: TaskGateway) {}

  async execute(id: string, completed: boolean): Promise<void> {
    await this.tasks.setCompleted(id, completed);
  }
}
