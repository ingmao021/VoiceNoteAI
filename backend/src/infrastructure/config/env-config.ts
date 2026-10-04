import { DEFAULT_TIME_ZONE } from '../ai/gemini-note-structurer.js';
import type { GoogleCredentials } from '../google/google-tasks-gateway.js';

/** Configuración desde variables de entorno (PRD §2.2). Falla al arrancar si falta algo obligatorio. */
export interface AppConfig {
  port: number;
  devApiToken: string;
  google: GoogleCredentials;
  /** `null` si no hay clave: las notas se guardan sin estructurar (FA-11). */
  gemini: { apiKey: string; model: string } | null;
  /** Zona IANA con la que se resuelven las fechas relativas del dictado. */
  timeZone: string;
}

export const DEFAULT_GEMINI_MODEL = 'gemini-3.5-flash-lite';

export function loadConfig(env: NodeJS.ProcessEnv = process.env): AppConfig {
  const missing: string[] = [];
  const required = (name: string): string => {
    const value = env[name]?.trim();
    if (!value) missing.push(name);
    return value ?? '';
  };

  const config: AppConfig = {
    port: Number(env.PORT ?? 3000),
    devApiToken: required('DEV_API_TOKEN'),
    google: {
      clientId: required('GOOGLE_CLIENT_ID'),
      clientSecret: required('GOOGLE_CLIENT_SECRET'),
      refreshToken: required('GOOGLE_REFRESH_TOKEN'),
    },
    gemini: env.GEMINI_API_KEY?.trim()
      ? { apiKey: env.GEMINI_API_KEY.trim(), model: env.GEMINI_MODEL?.trim() || DEFAULT_GEMINI_MODEL }
      : null,
    timeZone: env.TIME_ZONE?.trim() || DEFAULT_TIME_ZONE,
  };

  if (missing.length > 0) {
    throw new Error(`Missing environment variables: ${missing.join(', ')}. Copy .env.example to .env and fill it in.`);
  }
  return config;
}
