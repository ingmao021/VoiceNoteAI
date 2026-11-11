import type { FastifyInstance } from 'fastify';
import type { DeleteTaskUseCase } from '../../../application/use-cases/delete-task.use-case.js';
import type { ListRecentTasksUseCase } from '../../../application/use-cases/list-recent-tasks.use-case.js';
import type { SetTaskCompletedUseCase } from '../../../application/use-cases/set-task-completed.use-case.js';
import { bearerAuth } from '../plugins/bearer-auth.js';
import {
  deleteTaskSchema,
  listTasksSchema,
  setTaskCompletedSchema,
  type ListTasksQuery,
  type SetTaskCompletedBody,
  type TaskParams,
} from '../schemas/tasks.schemas.js';

export interface TasksRoutesOptions {
  devApiToken: string;
  listTasks: ListRecentTasksUseCase;
  setTaskCompleted: SetTaskCompletedUseCase;
  deleteTask: DeleteTaskUseCase;
}

/** Historial del reloj: leer tareas recientes y marcar/desmarcar. */
export async function tasksRoutes(
  app: FastifyInstance,
  { devApiToken, listTasks, setTaskCompleted, deleteTask }: TasksRoutesOptions,
) {
  app.get<{ Querystring: ListTasksQuery }>(
    '/v1/tasks',
    { schema: listTasksSchema, onRequest: bearerAuth(devApiToken) },
    async (req) => listTasks.execute(req.query.limit ?? 20, req.log),
  );

  app.patch<{ Params: TaskParams; Body: SetTaskCompletedBody }>(
    '/v1/tasks/:id',
    { schema: setTaskCompletedSchema, onRequest: bearerAuth(devApiToken) },
    async (req) => {
      await setTaskCompleted.execute(req.params.id, req.body.completed);
      return { id: req.params.id, completed: req.body.completed };
    },
  );

  app.delete<{ Params: TaskParams }>(
    '/v1/tasks/:id',
    { schema: deleteTaskSchema, onRequest: bearerAuth(devApiToken) },
    async (req) => {
      await deleteTask.execute(req.params.id);
      return { id: req.params.id };
    },
  );
}
