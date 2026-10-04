/** Lo que el LLM devuelve y lo que se guarda en Google Tasks. */
export interface StructuredNote {
  title: string;
  notes: string;
  subtasks: string[];
  /** Día para el que es la tarea, `AAAA-MM-DD`; `null` si el dictado no indica ninguno. */
  dueDate: string | null;
}

/** Límites de la Tasks API, recortados con margen. */
export const MAX_TITLE_LENGTH = 200;
export const MAX_NOTES_LENGTH = 8000;
export const MAX_SUBTASKS = 20;
const RAW_TITLE_WORDS = 8;

/** Degradación (FA-11, RN-06): el texto crudo, con las primeras palabras como título. */
export function createRawNote(text: string): StructuredNote {
  const clean = collapseWhitespace(text);
  const words = clean.split(' ');
  const head = words.slice(0, RAW_TITLE_WORDS).join(' ');
  const title = words.length > RAW_TITLE_WORDS ? `${head}…` : head;
  return {
    title: (title.charAt(0).toUpperCase() + title.slice(1)).slice(0, MAX_TITLE_LENGTH),
    notes: clean.slice(0, MAX_NOTES_LENGTH),
    subtasks: [],
    dueDate: null,
  };
}

/** La descripción nunca se pierde: si viene vacía, va el dictado tal cual. */
export function withNotesFallback(note: StructuredNote, text: string): StructuredNote {
  return note.notes ? note : { ...note, notes: createRawNote(text).notes };
}

function collapseWhitespace(text: string): string {
  return text.trim().replace(/\s+/g, ' ');
}
