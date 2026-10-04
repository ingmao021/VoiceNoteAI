import type { Logger } from '../../src/application/ports/logger.js';

export const silentLogger: Logger = { info: () => {}, warn: () => {}, error: () => {} };
