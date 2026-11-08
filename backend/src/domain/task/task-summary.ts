/** Resumen de una tarea de Google Tasks para el historial del reloj. */
export interface TaskSummary {
  id: string;
  title: string;
  /** Día de vencimiento (`AAAA-MM-DD`), o `null` si la tarea no tiene fecha. */
  dueDate: string | null;
  completed: boolean;
}

/** Lo que el backend devuelve al reloj: las tareas visibles y el «hoy» con el que se filtraron. */
export interface RecentTasks {
  /** `AAAA-MM-DD` en la zona configurada; el reloj lo usa para etiquetar HOY/MAÑANA. */
  today: string;
  tasks: TaskSummary[];
}
