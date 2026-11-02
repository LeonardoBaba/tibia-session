/** Time and duration formatting for the stamina tools. Durations are minutes, instants epoch ms. */

function pad2(n: number): string {
  return String(n).padStart(2, '0');
}

/** Minutes as hh:mm, e.g. 2520 → "42:00". Negative values show as 00:00. */
export function hhmm(min: number): string {
  const m = Math.max(0, Math.floor(min + 1e-9));
  return pad2(Math.floor(m / 60)) + ':' + pad2(m % 60);
}

/** Parses "hh:mm" (also "hhHmm", "hh mm" or just "hh") into minutes; null when it doesn't match. */
export function parseHHMM(text: string): number | null {
  const m = text.match(/^\s*(\d{1,2})\s*[:hH]?\s*(\d{1,2})?\s*$/);
  if (!m) return null;
  return (parseInt(m[1], 10) || 0) * 60 + (parseInt(m[2] ?? '0', 10) || 0);
}

/** Minutes as a compact duration, e.g. 2700 → "1d 21h 0m". */
export function durationLabel(mins: number): string {
  const m = Math.round(mins);
  const d = Math.floor(m / 1440);
  const h = Math.floor((m % 1440) / 60);
  const out: string[] = [];
  if (d) out.push(d + 'd');
  if (h) out.push(h + 'h');
  out.push((m % 60) + 'm');
  return out.join(' ');
}

/** Epoch ms rounded down to the whole minute. */
export function roundToMinute(ms: number): number {
  return Math.floor(ms / 60000) * 60000;
}

/** Epoch ms as a `datetime-local` input value in local time, e.g. "2026-11-02T14:05". */
export function toLocalInput(ms: number): string {
  const d = new Date(ms);
  return (
    d.getFullYear() +
    '-' +
    pad2(d.getMonth() + 1) +
    '-' +
    pad2(d.getDate()) +
    'T' +
    pad2(d.getHours()) +
    ':' +
    pad2(d.getMinutes())
  );
}

/** Weekday, date and time, e.g. "Mon, Nov 02, 14:05". */
export function clockLabel(ms: number, clock24h = true): string {
  return new Date(ms).toLocaleString('en-US', {
    weekday: 'short',
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: clock24h ? 'h23' : 'h12',
  });
}

/** Time of day only, e.g. "14:05" or "02:05 PM". */
export function timeLabel(ms: number, clock24h = true): string {
  return new Date(ms).toLocaleTimeString('en-US', {
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: clock24h ? 'h23' : 'h12',
  });
}

/** The day relative to `nowMs` plus the date: "Today, Nov 02", "Tomorrow, Nov 03", "Wednesday, Nov 04". */
export function dayLabel(ms: number, nowMs: number): string {
  const d = new Date(ms);
  const n = new Date(nowMs);
  const dayStart = new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
  const todayStart = new Date(n.getFullYear(), n.getMonth(), n.getDate()).getTime();
  // Rounded so a 23h or 25h daylight-saving day still counts as one day.
  const days = Math.round((dayStart - todayStart) / 86400000);
  const word =
    days === 0
      ? 'Today'
      : days === 1
        ? 'Tomorrow'
        : days === -1
          ? 'Yesterday'
          : d.toLocaleDateString('en-US', { weekday: 'long' });
  return word + ', ' + d.toLocaleDateString('en-US', { month: 'short', day: '2-digit' });
}

/** The local UTC offset at `ms`, e.g. "UTC−03:00". */
export function utcOffsetLabel(ms: number): string {
  const offset = -new Date(ms).getTimezoneOffset();
  const abs = Math.abs(offset);
  return 'UTC' + (offset < 0 ? '−' : '+') + pad2(Math.floor(abs / 60)) + ':' + pad2(abs % 60);
}
