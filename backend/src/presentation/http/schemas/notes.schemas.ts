import { errorResponse } from './error.schema.js';

export interface CreateNoteBody {
  text: string;
}

export const createNoteSchema = {
  summary: 'Structures the text with the LLM and saves it as a Google Tasks task',
  security: [{ bearer: [] }],
  // La longitud y el vacío los valida el dominio (422); aquí solo la forma (400).
  body: {
    type: 'object',
    properties: { text: { type: 'string' } },
    required: ['text'],
    additionalProperties: false,
  },
  response: {
    201: {
      description: 'Task created',
      type: 'object',
      properties: {
        id: { type: 'string' },
        title: { type: 'string' },
        /** `false` si el LLM falló y se guardó el texto crudo (FA-11). */
        structured: { type: 'boolean' },
        /** Día asignado a la tarea (`AAAA-MM-DD`), o `null` si el dictado no indicaba ninguno. */
        dueDate: { type: ['string', 'null'] },
      },
      required: ['id', 'title', 'structured', 'dueDate'],
    },
    400: errorResponse('`invalid_request`: malformed body'),
    401: errorResponse('`invalid_token`: missing or wrong app token'),
    422: errorResponse('`text_empty` (RN-02) or `text_too_long`'),
    424: errorResponse('`google_access_revoked`: the Google account must be linked again (FA-13)'),
    502: errorResponse('`tasks_provider_error`: Google Tasks rejected the request (FA-16)'),
  },
} as const;
