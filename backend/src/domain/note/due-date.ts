/** `AAAA-MM-DD` de un día que existe (descarta "2026-02-30"); cualquier otra cosa, `null`. */
export function parseDueDate(value: unknown): string | null {
  if (typeof value !== 'string') return null;
  const date = value.trim();
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) return null;
  const parsed = new Date(`${date}T00:00:00Z`);
  return !Number.isNaN(parsed.getTime()) && parsed.toISOString().startsWith(date) ? date : null;
}

/** `AAAA-MM-DD` del instante [now] en [timeZone] (en-CA formatea justo así). */
export function dateInTimeZone(now: Date, timeZone: string): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone, year: 'numeric', month: '2-digit', day: '2-digit' }).format(now);
}
