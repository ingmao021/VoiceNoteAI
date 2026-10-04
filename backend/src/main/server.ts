import { loadConfig } from '../infrastructure/config/env-config.js';
import { buildApp } from '../presentation/http/app.js';
import { createContainer } from './container.js';

const config = loadConfig();
const app = await buildApp(createContainer(config));

if (config.gemini) {
  app.log.info(`LLM: ${config.gemini.model}, dates in ${config.timeZone}`);
} else {
  app.log.warn('No GEMINI_API_KEY: notes will be saved unstructured (FA-11)');
}

// 0.0.0.0 para que el emulador llegue por 10.0.2.2 y para Render.
await app.listen({ port: config.port, host: '0.0.0.0' });
