import { describe, expect, it } from 'vitest';
import { dateInTimeZone, parseDueDate } from '../../src/domain/note/due-date.js';
import { MAX_TEXT_LENGTH, NoteText } from '../../src/domain/note/note-text.js';
import { createRawNote, withNotesFallback } from '../../src/domain/note/structured-note.js';
import { EmptyNoteTextError, NoteTextTooLongError } from '../../src/domain/errors/note-text-errors.js';

describe('createRawNote', () => {
  it('uses the first 8 words as title and the whole text as notes', () => {
    const text = 'revisar   el informe del proyecto antes del viernes por la tarde';
    expect(createRawNote(text)).toEqual({
      title: 'Revisar el informe del proyecto antes del viernes…',
      notes: 'revisar el informe del proyecto antes del viernes por la tarde',
      subtasks: [],
      dueDate: null,
    });
  });

  it('adds no ellipsis when the text is short', () => {
    expect(createRawNote('pan').title).toBe('Pan');
  });
});

describe('withNotesFallback', () => {
  it('fills empty notes with the dictation', () => {
    const note = { title: 'Informe', notes: '', subtasks: [], dueDate: null };
    expect(withNotesFallback(note, ' revisar   el informe ').notes).toBe('revisar el informe');
  });

  it('keeps non-empty notes', () => {
    const note = { title: 'Informe', notes: 'Revisar.', subtasks: [], dueDate: null };
    expect(withNotesFallback(note, 'otra cosa')).toBe(note);
  });
});

describe('NoteText', () => {
  it('trims the text', () => {
    expect(NoteText.create('  hola ').value).toBe('hola');
  });

  it('rejects empty text (RN-02)', () => {
    expect(() => NoteText.create('   ')).toThrow(EmptyNoteTextError);
  });

  it('rejects text over the limit', () => {
    expect(() => NoteText.create('x'.repeat(MAX_TEXT_LENGTH + 1))).toThrow(NoteTextTooLongError);
    expect(NoteText.create('x'.repeat(MAX_TEXT_LENGTH)).length).toBe(MAX_TEXT_LENGTH);
  });
});

describe('due dates', () => {
  it('parseDueDate requires a day that exists', () => {
    expect(parseDueDate('2028-02-29')).toBe('2028-02-29');
    expect(parseDueDate(' 2026-10-09 ')).toBe('2026-10-09');
    expect(parseDueDate('2026-02-30')).toBeNull();
    expect(parseDueDate('2026-13-01')).toBeNull();
    expect(parseDueDate('04/10/2026')).toBeNull();
    expect(parseDueDate(20261004)).toBeNull();
  });

  it("today's date is the user's time zone date, not UTC", () => {
    // 3 de octubre, 22:00 en Bogotá (UTC-5) = 4 de octubre, 03:00 UTC.
    expect(dateInTimeZone(new Date('2026-10-04T03:00:00Z'), 'America/Bogota')).toBe('2026-10-03');
  });
});
