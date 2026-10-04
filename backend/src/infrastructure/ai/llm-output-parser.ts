import { parseDueDate } from '../../domain/note/due-date.js';
import {
  MAX_NOTES_LENGTH,
  MAX_SUBTASKS,
  MAX_TITLE_LENGTH,
  type StructuredNote,
} from '../../domain/note/structured-note.js';

/**
 * Valida y recorta la salida del LLM. Lanza si no trae un título utilizable, para que se caiga
 * en la nota cruda en vez de crear una entrada vacía (RN-02).
 */
export function parseLlmOutput(raw: unknown): StructuredNote {
  const obj = (raw ?? {}) as Record<string, unknown>;
  const title = typeof obj.title === 'string' ? obj.title.trim().replace(/\.+$/, '') : '';
  if (!title) throw new Error('LLM returned no title');
  const notes = typeof obj.notes === 'string' ? obj.notes.trim() : '';
  const subtasks = Array.isArray(obj.subtasks)
    ? obj.subtasks.filter((s): s is string => typeof s === 'string').map((s) => s.trim()).filter(Boolean)
    : [];
  return {
    title: title.slice(0, MAX_TITLE_LENGTH),
    notes: notes.slice(0, MAX_NOTES_LENGTH),
    subtasks: subtasks.slice(0, MAX_SUBTASKS).map((s) => s.slice(0, MAX_TITLE_LENGTH)),
    // Una fecha mal formada no invalida la nota: se guarda sin fecha.
    dueDate: parseDueDate(obj.dueDate),
  };
}
