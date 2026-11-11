import { CreateNoteUseCase } from '../application/use-cases/create-note.use-case.js';
import { DeleteTaskUseCase } from '../application/use-cases/delete-task.use-case.js';
import { ListRecentTasksUseCase } from '../application/use-cases/list-recent-tasks.use-case.js';
import { SetTaskCompletedUseCase } from '../application/use-cases/set-task-completed.use-case.js';
import type { NoteStructurer } from '../application/ports/note-structurer.js';
import { FallbackNoteStructurer } from '../infrastructure/ai/fallback-note-structurer.js';
import { GeminiNoteStructurer } from '../infrastructure/ai/gemini-note-structurer.js';
import { RawNoteStructurer } from '../infrastructure/ai/raw-note-structurer.js';
import type { AppConfig } from '../infrastructure/config/env-config.js';
import { GoogleTasksGateway } from '../infrastructure/google/google-tasks-gateway.js';
import type { AppDependencies } from '../presentation/http/app.js';

/** Raíz de composición: el único sitio que conoce las implementaciones concretas. */
export function createContainer(config: AppConfig): AppDependencies {
  const tasks = new GoogleTasksGateway(config.google);
  return {
    devApiToken: config.devApiToken,
    createNote: new CreateNoteUseCase(createStructurer(config), tasks),
    listTasks: new ListRecentTasksUseCase(tasks, config.timeZone),
    setTaskCompleted: new SetTaskCompletedUseCase(tasks),
    deleteTask: new DeleteTaskUseCase(tasks),
  };
}

function createStructurer(config: AppConfig): NoteStructurer {
  const raw = new RawNoteStructurer();
  if (!config.gemini) return raw;
  const gemini = new GeminiNoteStructurer(config.gemini.apiKey, config.gemini.model, { timeZone: config.timeZone });
  return new FallbackNoteStructurer(gemini, raw);
}
