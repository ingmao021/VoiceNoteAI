import { describe, expect, it } from 'vitest';
import type { NoteStructurer } from '../../../src/application/ports/note-structurer.js';
import { MAX_SUBTASKS } from '../../../src/domain/note/structured-note.js';
import { FallbackNoteStructurer } from '../../../src/infrastructure/ai/fallback-note-structurer.js';
import { buildUserContent } from '../../../src/infrastructure/ai/gemini-prompt.js';
import { parseLlmOutput } from '../../../src/infrastructure/ai/llm-output-parser.js';
import { RawNoteStructurer } from '../../../src/infrastructure/ai/raw-note-structurer.js';
import { silentLogger } from '../../support/silent-logger.js';

describe('parseLlmOutput', () => {
  it('trims spaces, drops the trailing period of the title and empty subtasks', () => {
    expect(parseLlmOutput({ title: ' Compras. ', notes: ' para el finde ', subtasks: ['Leche', ' ', 3, 'Pan '] })).toEqual({
      title: 'Compras',
      notes: 'para el finde',
      subtasks: ['Leche', 'Pan'],
      dueDate: null,
    });
  });

  it('tolerates missing fields except the title', () => {
    expect(parseLlmOutput({ title: 'Idea' })).toEqual({ title: 'Idea', notes: '', subtasks: [], dueDate: null });
  });

  it('keeps a valid date and drops a malformed one without invalidating the note', () => {
    expect(parseLlmOutput({ title: 'Médico', dueDate: ' 2026-10-09 ' }).dueDate).toBe('2026-10-09');
    expect(parseLlmOutput({ title: 'Médico', dueDate: 'el jueves' }).dueDate).toBeNull();
    expect(parseLlmOutput({ title: 'Médico', dueDate: '' }).dueDate).toBeNull();
  });

  it('throws without a title so the fallback kicks in', () => {
    expect(() => parseLlmOutput({ title: '  ', notes: 'algo' })).toThrow();
    expect(() => parseLlmOutput(null)).toThrow();
  });

  it('caps the number of subtasks', () => {
    const subtasks = Array.from({ length: 50 }, (_, i) => `t${i}`);
    expect(parseLlmOutput({ title: 'Muchas', subtasks }).subtasks).toHaveLength(MAX_SUBTASKS);
  });
});

describe('buildUserContent', () => {
  it("prefixes the dictation with today's date in the user's time zone", () => {
    const night = new Date('2026-10-04T03:00:00Z');
    expect(buildUserContent('comprar pan mañana', night, 'America/Bogota')).toBe(
      'Today is Saturday 2026-10-03.\n\nDictation:\ncomprar pan mañana',
    );
  });
});

describe('FallbackNoteStructurer', () => {
  const failing: NoteStructurer = {
    structure: async () => {
      throw new Error('503 from the LLM');
    },
  };

  it('returns the primary result when it succeeds', async () => {
    const note = { title: 'Compras', notes: 'Leche', subtasks: [], dueDate: null };
    const primary: NoteStructurer = { structure: async () => ({ note, structured: true }) };
    const result = await new FallbackNoteStructurer(primary, new RawNoteStructurer()).structure('x', silentLogger);
    expect(result).toEqual({ note, structured: true });
  });

  it('falls back to the raw note when the primary fails (FA-11)', async () => {
    const result = await new FallbackNoteStructurer(failing, new RawNoteStructurer()).structure(
      'llamar al médico el jueves',
      silentLogger,
    );
    expect(result.structured).toBe(false);
    expect(result.note.title).toBe('Llamar al médico el jueves');
  });
});
