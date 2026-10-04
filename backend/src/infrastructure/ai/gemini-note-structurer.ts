import { GoogleGenAI } from '@google/genai';
import type { NoteStructurer, StructuringResult } from '../../application/ports/note-structurer.js';
import { buildUserContent, RESPONSE_SCHEMA, SYSTEM_INSTRUCTIONS } from './gemini-prompt.js';
import { parseLlmOutput } from './llm-output-parser.js';

/**
 * Tiempo máximo para el LLM. Sumado al de Google Tasks debe quedar muy por debajo de los 90 s
 * del reloj (RNF-05); si se agota, la nota se guarda sin estructurar (FA-11).
 */
export const LLM_TIMEOUT_MS = 10_000;

/** Zona del usuario para resolver «mañana», «el viernes»… Configurable con `TIME_ZONE`. */
export const DEFAULT_TIME_ZONE = 'America/Bogota';

export interface GeminiOptions {
  timeZone?: string;
  timeoutMs?: number;
  /** Inyectable para las pruebas. */
  now?: () => Date;
}

/** Estrategia con Gemini. Lanza ante cualquier fallo: la degradación la decide [FallbackNoteStructurer]. */
export class GeminiNoteStructurer implements NoteStructurer {
  private readonly ai: GoogleGenAI;
  private readonly timeZone: string;
  private readonly timeoutMs: number;
  private readonly now: () => Date;

  constructor(
    apiKey: string,
    private readonly model: string,
    { timeZone = DEFAULT_TIME_ZONE, timeoutMs = LLM_TIMEOUT_MS, now = () => new Date() }: GeminiOptions = {},
  ) {
    this.ai = new GoogleGenAI({ apiKey });
    this.timeZone = timeZone;
    this.timeoutMs = timeoutMs;
    this.now = now;
  }

  async structure(text: string): Promise<StructuringResult> {
    const response = await this.ai.models.generateContent({
      model: this.model,
      contents: buildUserContent(text, this.now(), this.timeZone),
      config: {
        systemInstruction: SYSTEM_INSTRUCTIONS,
        responseMimeType: 'application/json',
        responseJsonSchema: RESPONSE_SCHEMA,
        temperature: 0.2,
        abortSignal: AbortSignal.timeout(this.timeoutMs),
      },
    });
    return { note: parseLlmOutput(JSON.parse(response.text ?? '')), structured: true };
  }
}
