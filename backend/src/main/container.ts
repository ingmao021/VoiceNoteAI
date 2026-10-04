import { CreateNoteUseCase } from '../application/use-cases/create-note.use-case.js';
import type { NoteStructurer } from '../application/ports/note-structurer.js';
import { FallbackNoteStructurer } from '../infrastructure/ai/fallback-note-structurer.js';
import { GeminiNoteStructurer } from '../infrastructure/ai/gemini-note-structurer.js';
import { RawNoteStructurer } from '../infrastructure/ai/raw-note-structurer.js';
import type { AppConfig } from '../infrastructure/config/env-config.js';
import { GoogleTasksGateway } from '../infrastructure/google/google-tasks-gateway.js';
import type { AppDependencies } from '../presentation/http/app.js';

/** Raíz de composición: el único sitio que conoce las implementaciones concretas. */
export function createContainer(config: AppConfig): AppDependencies {
  return {
    devApiToken: config.devApiToken,
    createNote: new CreateNoteUseCase(createStructurer(config), new GoogleTasksGateway(config.google)),
  };
}

function createStructurer(config: AppConfig): NoteStructurer {
  const raw = new RawNoteStructurer();
  if (!config.gemini) return raw;
  const gemini = new GeminiNoteStructurer(config.gemini.apiKey, config.gemini.model, { timeZone: config.timeZone });
  return new FallbackNoteStructurer(gemini, raw);
}
