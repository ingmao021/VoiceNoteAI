import { errorResponse } from './error.schema.js';

export interface ListTasksQuery {
  limit?: number;
}

export interface TaskParams {
  id: string;
}

export interface SetTaskCompletedBody {
  completed: boolean;
}

export const listTasksSchema = {
  summary: 'List recent Google Tasks; expired tasks are hidden',
  security: [{ bearer: [] }],
  querystring: {
    type: 'object',
    properties: {
      limit: { type: 'integer', minimum: 1, maximum: 50, default: 20 },
    },
    additionalProperties: false,
  },
  response: {
    200: {
      description: 'Recent tasks in Google Tasks order, plus the day used to hide expired ones',
      type: 'object',
      properties: {
        today: { type: 'string' },
        tasks: {
          type: 'array',
          items: {
            type: 'object',
            properties: {
              id: { type: 'string' },
              title: { type: 'string' },
              /** `null` si la tarea no tiene fecha de vencimiento. */
              dueDate: { type: ['string', 'null'] },
              completed: { type: 'boolean' },
            },
            required: ['id', 'title', 'dueDate', 'completed'],
          },
        },
      },
      required: ['today', 'tasks'],
    },
    401: errorResponse('`invalid_token`: missing or wrong app token'),
    424: errorResponse('`google_access_revoked`: the Google account must be linked again (FA-13)'),
    502: errorResponse('`tasks_provider_error`: Google Tasks rejected the request (FA-16)'),
  },
} as const;

export const deleteTaskSchema = {
  summary: 'Delete a task (swipe left in the watch history)',
  security: [{ bearer: [] }],
  params: {
    type: 'object',
    properties: { id: { type: 'string', minLength: 1 } },
    required: ['id'],
    additionalProperties: false,
  },
  response: {
    200: {
      description: 'Task deleted',
      type: 'object',
      properties: { id: { type: 'string' } },
      required: ['id'],
    },
    401: errorResponse('`invalid_token`: missing or wrong app token'),
    404: errorResponse('`task_not_found`: the task no longer exists'),
    424: errorResponse('`google_access_revoked`: the Google account must be linked again (FA-13)'),
    502: errorResponse('`tasks_provider_error`: Google Tasks rejected the request (FA-16)'),
  },
} as const;

export const setTaskCompletedSchema = {
  summary: 'Mark a task as completed or pending',
  security: [{ bearer: [] }],
  params: {
    type: 'object',
    properties: { id: { type: 'string', minLength: 1 } },
    required: ['id'],
    additionalProperties: false,
  },
  body: {
    type: 'object',
    properties: { completed: { type: 'boolean' } },
    required: ['completed'],
    additionalProperties: false,
  },
  response: {
    200: {
      description: 'Task updated',
      type: 'object',
      properties: {
        id: { type: 'string' },
        completed: { type: 'boolean' },
      },
      required: ['id', 'completed'],
    },
    400: errorResponse('`invalid_request`: malformed body'),
    401: errorResponse('`invalid_token`: missing or wrong app token'),
    404: errorResponse('`task_not_found`: the task no longer exists'),
    424: errorResponse('`google_access_revoked`: the Google account must be linked again (FA-13)'),
    502: errorResponse('`tasks_provider_error`: Google Tasks rejected the request (FA-16)'),
  },
} as const;
